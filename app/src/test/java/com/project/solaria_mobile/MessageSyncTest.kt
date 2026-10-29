package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.database.LocalDataWiper
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.database.SyncStateEntity
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.LastMessageDto
import com.project.solaria_mobile.feature.communication.data.MessageStore
import com.project.solaria_mobile.feature.communication.data.MessageSync
import com.project.solaria_mobile.feature.communication.data.UserDirectory
import com.project.solaria_mobile.feature.profile.data.UserSummaryDto
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do motor de sincronização ([MessageSync]): primeira sincronização, catch-up, eventos ao vivo, buraco de
 * `sequence`, troca de época, perda de acesso, chatbot, logout e nomes.
 *
 * Roda o motor de verdade sobre dublês em memória do Room e do messenger. O que o Room faz de verdade (consultas SQL e
 * transações) fica sem prova aqui.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageSyncTest {
    private val conversationDao = InMemoryConversationDao()
    private val messageDao = InMemoryMessageDao(conversationDao)
    private val userDao = InMemoryUserDao()
    private val syncDao = InMemorySyncStateDao()
    private val pendingDao = InMemoryPendingMessageDao()
    private val api = ScriptedMessengerApi()
    private val profile = FakeProfileApi()
    private val realtime = FakeRealtimeClient()
    private val network = MutableStateFlow(true)
    private val tokens = FakeTokenStore()
    private val store = MessageStore(messageDao, conversationDao)
    private var wipes = 0
    private var clock = 0L

    private val events = "/topic/users.user-1.events"

    private fun TestScope.startSync(signedIn: Boolean = true): MessageSync {
        val executor = ApiCallExecutor(ApiErrorParser(ApiJson.instance))
        val sync = MessageSync(
            api = api,
            executor = executor,
            tokens = tokens,
            realtime = realtime,
            networkMonitor = object : NetworkMonitor { override val isOnline = network },
            json = ApiJson.instance,
            store = store,
            conversations = conversationDao,
            messages = messageDao,
            syncState = syncDao,
            users = UserDirectory(profile, executor, userDao, conversationDao, messageDao).also { it.now = { clock } },
            wiper = LocalDataWiper {
                wipes++
                conversationDao.clear()
                messageDao.clear()
                userDao.clear()
                syncDao.clear()
                pendingDao.clear()
            },
            scope = backgroundScope,
        )
        if (signedIn) tokens.signIn("user-1")
        sync.start()
        runCurrent()
        return sync
    }

    private fun conversation(
        id: String,
        type: String = "DIRECT",
        lastSequence: Int = 0,
        archived: Boolean? = null,
        version: Long = api.nextVersion(),
    ) = ConversationDto(
        id = id,
        syncEpoch = api.epoch,
        syncVersion = version,
        conversationType = type,
        participantIds = listOf("user-1", "user-2"),
        archived = archived,
        lastInteractionAt = "2026-10-28T10:00:00Z",
        lastMessage = if (lastSequence > 0) LastMessageDto("$id-m$lastSequence", lastSequence, "mensagem $lastSequence") else null,
    )

    private fun messageFrame(type: String, conversationId: String, sequence: Int, content: String, version: Long, extra: String = "") =
        """{"type":"$type","conversationId":"$conversationId","eventId":"e$version","payload":{"id":"$conversationId-m$sequence","conversationId":"$conversationId","senderId":"user-2","content":"$content","sequence":$sequence,"timestamp":"2026-10-28T10:00:00Z","changeVersion":$version$extra}}"""

    private fun conversationFrame(type: String, item: ConversationDto) =
        """{"type":"$type","conversationId":"${item.id}","eventId":"c${item.syncVersion}","payload":${ApiJson.instance.encodeToString(ConversationDto.serializer(), item)}}"""

    private fun removedFrame(id: String, version: Long) =
        """{"type":"CONVERSATION_REMOVED","eventId":"r$version","payload":{"id":"$id","syncEpoch":"${api.epoch}","syncVersion":$version}}"""

    private fun serverWith(conversationId: String, count: Int, type: String = "DIRECT") {
        (1..count).forEach { api.serverMessage(conversationId, it) }
        api.conversationItems += conversation(conversationId, type = type, lastSequence = count)
    }

    private fun pendingMessage() = PendingMessageEntity(
        clientMessageId = "pendente", senderId = "user-1", conversationId = "c1", messageType = "USER_TO_USER", content = "na fila", createdAt = 1,
    )

    private fun contentOf(id: String) = messageDao.rows.single { it.id == id }.content

    @Test fun firstSyncStoresTheCursorAndDownloadsEveryHumanConversationDownToSequenceOne() = runTest {
        serverWith("c1", 1200)
        serverWith("bot", 2, type = "CHAT_BOT")

        val sync = startSync()

        assertEquals((1..1200).toList(), messageDao.rows.filter { it.conversationId == "c1" }.map { it.sequence }.sorted())
        assertTrue("o chatbot não tem mensagens no banco", messageDao.rows.none { it.conversationId == "bot" })
        assertEquals(SyncStateEntity(syncEpoch = "epoch-1", cursor = api.version), syncDao.state)
        // As 500 mais recentes, depois as 500 antes da 701 e por fim as 200 que faltam
        val requests = api.messageRequests.filter { it.conversationId == "c1" }
        assertEquals(listOf(null to null, null to 701, null to 201), requests.map { it.sinceSequence to it.beforeSequence })
        assertTrue(api.messageRequests.none { it.conversationId == "bot" })
        assertEquals(ApiResult.Success(Unit), sync.result.value)
    }

    @Test fun syncOfAnUpToDateDeviceDownloadsNothingAgain() = runTest {
        serverWith("c1", 30)
        val sync = startSync()
        val calls = api.historyCalls

        sync.refresh()
        runCurrent()

        assertEquals(calls, api.historyCalls)
        assertEquals(30, messageDao.rows.size)
    }

    @Test fun catchUpAppliesEditsDeletesAndNewMessagesAndMovesTheCursor() = runTest {
        serverWith("c1", 5)
        val sync = startSync()
        val firstCursor = syncDao.state!!.cursor

        api.serverEdit("c1-m3", "texto novo")
        api.serverDelete("c1-m5")
        api.serverMessage("c1", 6)
        api.conversationItems.replaceAll { it.copy(syncVersion = api.nextVersion(), lastMessage = LastMessageDto("c1-m6", 6, "mensagem 6")) }
        sync.refresh()
        runCurrent()

        assertEquals("texto novo", contentOf("c1-m3"))
        assertTrue(messageDao.rows.single { it.id == "c1-m3" }.edited)
        val deleted = messageDao.rows.single { it.id == "c1-m5" }
        assertTrue(deleted.deleted)
        assertNull("a exclusão apaga o texto do banco", deleted.content)
        assertEquals(6, messageDao.rows.size)
        assertTrue(syncDao.state!!.cursor > firstCursor)
        assertEquals(api.history.maxOf { it.changeVersion }, syncDao.state!!.cursor)
    }

    @Test fun catchUpFollowsTheNextPageWhileThereIsMore() = runTest {
        serverWith("c1", 600)
        val sync = startSync()
        val updatesBefore = api.updateCalls

        (1..600).forEach { api.serverEdit("c1-m$it", "editada $it") }
        sync.refresh()
        runCurrent()

        assertEquals("editada 1", contentOf("c1-m1"))
        assertEquals("editada 600", contentOf("c1-m600"))
        assertEquals(2, api.updateCalls - updatesBefore)
        assertEquals(api.version, syncDao.state!!.cursor)
    }

    @Test fun liveEventsUpdateTheMessageButNeverMoveTheCursor() = runTest {
        serverWith("c1", 3)
        startSync()
        val cursor = syncDao.state!!.cursor

        realtime.frames.emit(events to messageFrame("MESSAGE_EDITED", "c1", 2, "editada ao vivo", api.nextVersion(), ",\"edited\":true"))
        runCurrent()

        assertEquals("editada ao vivo", contentOf("c1-m2"))
        assertEquals(cursor, syncDao.state!!.cursor)
    }

    @Test fun liveDeleteErasesTheTextAndAStaleHistoryPageCannotBringItBack() = runTest {
        serverWith("c1", 3)
        startSync()
        val staleVersion = messageDao.rows.single { it.id == "c1-m3" }.changeVersion

        realtime.frames.emit(events to messageFrame("MESSAGE_DELETED", "c1", 3, "", api.nextVersion(), ",\"deleted\":true"))
        runCurrent()
        // Uma página de histórico lida antes da exclusão chega depois do evento
        realtime.frames.emit(events to messageFrame("MESSAGE_CREATED", "c1", 3, "texto antigo", staleVersion))
        runCurrent()

        val after = messageDao.rows.single { it.id == "c1-m3" }
        assertTrue(after.deleted)
        assertNull(after.content)
    }

    @Test fun deletingTheLastMessageClearsThePreviewOfTheList() = runTest {
        serverWith("c1", 3)
        startSync()
        assertEquals("mensagem 3", conversationDao.rows.single().lastMessageContent)

        realtime.frames.emit(events to messageFrame("MESSAGE_DELETED", "c1", 3, "", api.nextVersion(), ",\"deleted\":true"))
        runCurrent()

        val row = conversationDao.rows.single()
        assertNull(row.lastMessageContent)
        assertTrue(row.lastMessageDeleted)
    }

    @Test fun liveEventThatLeavesAHoleFetchesTheMissingRange() = runTest {
        serverWith("c1", 3)
        startSync()
        // O servidor já tem a 4, a 5 e a 6, mas só o evento da 6 chegou
        api.serverMessage("c1", 4)
        api.serverMessage("c1", 5)
        val sixth = api.serverMessage("c1", 6)
        api.messageRequests.clear()

        realtime.frames.emit(events to messageFrame("MESSAGE_CREATED", "c1", 6, "mensagem 6", sixth.changeVersion))
        runCurrent()

        assertEquals((1..6).toList(), messageDao.rows.map { it.sequence }.sorted())
        assertEquals(listOf(3), api.messageRequests.map { it.sinceSequence })
    }

    @Test fun holeTheServerNeverFillsIsAskedOncePerSyncAndDoesNotLoop() = runTest {
        serverWith("c1", 5)
        api.history.removeAll { it.id == "c1-m3" }
        val sync = startSync()

        assertEquals(listOf(1, 2, 4, 5), messageDao.rows.map { it.sequence }.sorted())
        // As mais recentes e depois um pedido a partir da 2, que não traz nada novo e encerra a conversa
        assertEquals(2, api.messageRequests.count { it.conversationId == "c1" })

        sync.refresh()
        runCurrent()
        assertEquals(1, api.messageRequests.drop(2).count { it.sinceSequence == 2 })
        assertEquals(3, api.messageRequests.count { it.conversationId == "c1" })
    }

    @Test fun epochChangeWipesMessagesAndCursorButKeepsThePendingQueue() = runTest {
        serverWith("c1", 4)
        val sync = startSync()
        pendingDao.insert(pendingMessage())

        // O relógio do servidor recomeçou: época nova, versões do zero e outras mensagens na conversa
        api.epoch = "epoch-2"
        api.history.clear()
        api.version = 0
        (1..2).forEach { api.serverMessage("c1", it, content = "nova $it") }
        api.conversationItems.clear()
        api.conversationItems += conversation("c1", lastSequence = 2, version = 1)
        sync.refresh()
        runCurrent()

        assertEquals(listOf("nova 1", "nova 2"), messageDao.rows.sortedBy { it.sequence }.map { it.content })
        assertEquals("epoch-2", syncDao.state!!.syncEpoch)
        assertEquals(1, pendingDao.rows.size)
    }

    @Test fun conversationRemovedEventKeepsTheHistoryAndMarksTheLostAccess() = runTest {
        serverWith("c1", 3)
        startSync()

        realtime.frames.emit(events to removedFrame("c1", api.nextVersion()))
        runCurrent()

        assertTrue(conversationDao.rows.single().accessLost)
        assertEquals(3, messageDao.rows.size)
    }

    @Test fun staleConversationUpdateDoesNotUndoARemoval() = runTest {
        serverWith("c1", 3)
        startSync()
        val stale = api.conversationItems.single()
        realtime.frames.emit(events to removedFrame("c1", api.nextVersion()))
        runCurrent()

        realtime.frames.emit(events to conversationFrame("CONVERSATION_UPDATED", stale))
        runCurrent()

        assertTrue(conversationDao.rows.single().accessLost)
    }

    @Test fun newerConversationUpdateReadmitsTheUser() = runTest {
        serverWith("c1", 3)
        startSync()
        realtime.frames.emit(events to removedFrame("c1", api.nextVersion()))
        runCurrent()

        realtime.frames.emit(events to conversationFrame("CONVERSATION_UPDATED", api.conversationItems.single().copy(syncVersion = api.nextVersion())))
        runCurrent()

        assertFalse(conversationDao.rows.single().accessLost)
    }

    @Test fun conversationMissingFromTheListIsMarkedLostAndRecoversWhenItReturns() = runTest {
        serverWith("c1", 3)
        serverWith("c2", 2)
        val sync = startSync()

        // O usuário sai de c1: a conversa some da lista e o servidor passa a negar o acesso
        val dropped = api.conversationItems.single { it.id == "c1" }
        api.conversationItems.remove(dropped)
        api.forbidden += "c1"
        sync.refresh()
        runCurrent()
        assertTrue(conversationDao.rows.single { it.id == "c1" }.accessLost)
        assertFalse(conversationDao.rows.single { it.id == "c2" }.accessLost)
        assertEquals("o histórico baixado fica no aparelho", 3, messageDao.rows.count { it.conversationId == "c1" })

        // Enquanto está fora, c1 recebe a mensagem 4 e o cursor passa dela por causa de uma mensagem de c2
        api.serverMessage("c1", 4)
        api.serverMessage("c2", 3)
        api.conversationItems.replaceAll { it.copy(syncVersion = api.nextVersion(), lastMessage = LastMessageDto("c2-m3", 3, "mensagem 3")) }
        api.messageRequests.clear()
        sync.refresh()
        runCurrent()
        assertTrue("conversa sem acesso não sincroniza", api.messageRequests.none { it.conversationId == "c1" })

        // Readmitida: volta à lista e o trecho enviado na ausência é preenchido pelo histórico
        api.forbidden -= "c1"
        api.conversationItems += dropped.copy(syncVersion = api.nextVersion(), lastMessage = LastMessageDto("c1-m4", 4, "mensagem 4"))
        sync.refresh()
        runCurrent()
        assertFalse(conversationDao.rows.single { it.id == "c1" }.accessLost)
        assertEquals((1..4).toList(), messageDao.rows.filter { it.conversationId == "c1" }.map { it.sequence }.sorted())
        assertEquals(listOf(3), api.messageRequests.filter { it.conversationId == "c1" }.map { it.sinceSequence })
    }

    @Test fun archivedConversationIsNotTakenForALostOne() = runTest {
        serverWith("c1", 2)
        api.conversationItems.replaceAll { it.copy(archived = true) }

        startSync()

        val row = conversationDao.rows.single()
        assertFalse(row.accessLost)
        assertTrue(row.archived)
        assertEquals(2, messageDao.rows.size)
    }

    @Test fun conversationCreatedByAnEventDownloadsItsHistory() = runTest {
        startSync()
        (1..3).forEach { api.serverMessage("novo", it) }

        realtime.frames.emit(events to conversationFrame("CONVERSATION_UPDATED", conversation("novo", lastSequence = 3)))
        runCurrent()

        assertEquals(listOf("novo"), conversationDao.rows.map { it.id })
        assertEquals((1..3).toList(), messageDao.rows.map { it.sequence }.sorted())
    }

    @Test fun forbiddenHistoryOfOneConversationDoesNotBlockTheOthers() = runTest {
        serverWith("c1", 2)
        serverWith("c2", 2)
        api.forbidden += "c1"

        val sync = startSync()

        assertEquals(ApiResult.Success(Unit), sync.result.value)
        assertEquals(2, messageDao.rows.count { it.conversationId == "c2" })
    }

    @Test fun chatbotMessagesStayOutOfTheDatabase() = runTest {
        api.conversationItems += conversation("bot", type = "CHAT_BOT")
        startSync()

        realtime.frames.emit(events to messageFrame("MESSAGE_CREATED", "bot", 1, "resposta do bot", api.nextVersion()))
        runCurrent()

        assertTrue(messageDao.rows.isEmpty())
        assertEquals(listOf("resposta do bot"), store.observe("bot").first().map { it.content })
    }

    @Test fun malformedAndUnknownFramesAreIgnored() = runTest {
        serverWith("c1", 2)
        startSync()

        realtime.frames.emit(events to "não é json")
        realtime.frames.emit(events to """{"type":"MESSAGE_CREATED","payload":{"id":1}}""")
        realtime.frames.emit(events to """{"type":"PRESENCE","payload":{}}""")
        runCurrent()

        assertEquals(2, messageDao.rows.size)
    }

    @Test fun endingTheSessionWipesEveryLocalTableAndTheChatbotMemory() = runTest {
        serverWith("c1", 2)
        api.conversationItems += conversation("bot", type = "CHAT_BOT")
        val sync = startSync()
        pendingDao.insert(pendingMessage())
        realtime.frames.emit(events to messageFrame("MESSAGE_CREATED", "bot", 1, "segredo", api.nextVersion()))
        runCurrent()
        assertEquals(listOf("segredo"), store.observe("bot").first().map { it.content })

        tokens.tokens.value = null
        runCurrent()

        assertEquals(1, wipes)
        assertTrue(messageDao.rows.isEmpty())
        assertTrue(conversationDao.rows.isEmpty())
        assertNull(syncDao.state)
        assertTrue("a fila de envio também é apagada", pendingDao.rows.isEmpty())
        assertNull(sync.result.value)

        // Outra conta entra no mesmo aparelho e a conversa do chatbot volta ao banco, mas não o que estava em memória
        tokens.signIn("user-1")
        runCurrent()
        assertTrue(store.observe("bot").first().isEmpty())
    }

    @Test fun switchingDirectlyToAnotherAccountWipesTheOldDataBeforeTheNewSession() = runTest {
        serverWith("c1", 2)
        startSync()
        assertEquals(2, messageDao.rows.size)
        api.conversationItems.clear()
        api.history.clear()

        tokens.signIn("user-3")
        runCurrent()

        assertEquals(1, wipes)
        assertTrue(messageDao.rows.isEmpty())
        assertTrue(conversationDao.rows.isEmpty())
        assertEquals("user-3 começa do zero", SyncStateEntity(syncEpoch = "epoch-1", cursor = api.version), syncDao.state)
    }

    @Test fun chatbotMessagesThatArrivedBeforeTheirConversationAreRemovedWhenItShowsUp() = runTest {
        startSync()
        // A mensagem chega antes de a conversa existir no banco, então ainda não dá para saber que é do chatbot
        realtime.frames.emit(events to messageFrame("MESSAGE_CREATED", "bot", 1, "resposta", api.nextVersion()))
        runCurrent()
        assertEquals(1, messageDao.rows.size)

        realtime.frames.emit(events to conversationFrame("CONVERSATION_UPDATED", conversation("bot", type = "CHAT_BOT")))
        runCurrent()

        assertTrue(messageDao.rows.isEmpty())
    }

    @Test fun chatbotMemoryAlsoFollowsTheVersionRule() = runTest {
        api.conversationItems += conversation("bot", type = "CHAT_BOT")
        startSync()

        realtime.frames.emit(events to messageFrame("MESSAGE_CREATED", "bot", 1, "primeira", 10))
        realtime.frames.emit(events to messageFrame("MESSAGE_EDITED", "bot", 1, "editada", 12, ",\"edited\":true"))
        realtime.frames.emit(events to messageFrame("MESSAGE_CREATED", "bot", 1, "página velha", 10))
        runCurrent()

        assertEquals(listOf("editada"), store.observe("bot").first().map { it.content })
    }

    @Test fun renewingTheTokenOfTheSameUserDoesNotRestartTheSession() = runTest {
        serverWith("c1", 2)
        startSync()
        val updates = api.updateCalls

        tokens.signIn("user-1", suffix = "renovado")
        runCurrent()

        assertEquals(0, wipes)
        assertEquals(updates, api.updateCalls)
        assertEquals(2, messageDao.rows.size)
    }

    @Test fun startingSignedOutOnlyWipesAndNeverCallsTheServer() = runTest {
        startSync(signedIn = false)

        assertEquals(1, wipes)
        assertEquals(0, api.updateCalls)
    }

    @Test fun withoutInternetNothingIsCalledAndTheResultSaysSo() = runTest {
        network.value = false

        val sync = startSync()

        assertEquals(0, api.updateCalls)
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), sync.result.value)
    }

    @Test fun refreshClearsTheResultUntilTheNewSyncEnds() = runTest {
        serverWith("c1", 1)
        val sync = startSync()
        assertEquals(ApiResult.Success(Unit), sync.result.value)

        sync.refresh()
        assertNull(sync.result.value)
        runCurrent()
        assertEquals(ApiResult.Success(Unit), sync.result.value)
    }

    @Test fun failedSyncIsRepeatedAfterThirtySeconds() = runTest {
        serverWith("c1", 2)
        api.listFailure = IOException("sem resposta")
        val sync = startSync()
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), sync.result.value)

        api.listFailure = null
        advanceTimeBy(30_001)
        runCurrent()

        assertEquals(ApiResult.Success(Unit), sync.result.value)
        assertEquals(2, messageDao.rows.size)
    }

    @Test fun connectingTheWebSocketTriggersASync() = runTest {
        serverWith("c1", 2)
        startSync()
        val updates = api.updateCalls

        // Uma mensagem chegou enquanto o WebSocket estava fora
        api.serverMessage("c1", 3)
        api.conversationItems.replaceAll { it.copy(syncVersion = api.nextVersion(), lastMessage = LastMessageDto("c1-m3", 3, "mensagem 3")) }
        realtime.connection.value = RealtimeState.Connected
        runCurrent()

        assertEquals(3, messageDao.rows.size)
        assertTrue(api.updateCalls > updates)
    }

    @Test fun networkComingBackTriggersASync() = runTest {
        network.value = false
        serverWith("c1", 2)
        startSync()
        assertEquals(0, api.updateCalls)

        network.value = true
        runCurrent()

        assertEquals(2, messageDao.rows.size)
    }

    @Test fun directConversationPeerNameIsFetchedAndWrittenOnlyWhenItChanges() = runTest {
        serverWith("c1", 1)
        profile.summaries["user-2"] = UserSummaryDto("Ana Bia", "https://img/ana.png")
        val sync = startSync()
        assertEquals(listOf("Ana Bia"), userDao.rows.map { it.username })
        assertEquals(1, userDao.writes)

        clock += 31_000
        sync.refresh()
        runCurrent()
        assertEquals("igual: nada é gravado", 1, userDao.writes)
        assertEquals(2, profile.summaryCalls.size)

        profile.summaries["user-2"] = UserSummaryDto("Ana Beatriz", null)
        clock += 31_000
        sync.refresh()
        runCurrent()
        assertEquals(listOf("Ana Beatriz"), userDao.rows.map { it.username })
        assertEquals(2, userDao.writes)
    }

    @Test fun samePersonIsNotAskedAgainWithinThirtySeconds() = runTest {
        serverWith("c1", 1)
        profile.summaries["user-2"] = UserSummaryDto("Ana Bia", null)
        val sync = startSync()

        clock += 10_000
        sync.refresh()
        runCurrent()
        clock += 10_000
        sync.refresh()
        runCurrent()

        assertEquals(1, profile.summaryCalls.size)
    }

    @Test fun lookupThatFailedIsTriedAgainOnTheNextSyncAndALogoutForgetsTheThrottle() = runTest {
        serverWith("c1", 1)
        val sync = startSync()
        assertTrue(userDao.rows.isEmpty())

        // A primeira consulta falhou (404), então não conta como feita: a próxima sincronização tenta de novo
        profile.summaries["user-2"] = UserSummaryDto("Ana Bia", null)
        clock += 1_000
        sync.refresh()
        runCurrent()
        assertEquals(listOf("Ana Bia"), userDao.rows.map { it.username })
        assertEquals(2, profile.summaryCalls.size)

        // Logout e novo login em menos de 30 s: as consultas recomeçam
        tokens.tokens.value = null
        runCurrent()
        tokens.signIn("user-1")
        clock += 1_000
        runCurrent()
        assertEquals(listOf("Ana Bia"), userDao.rows.map { it.username })
        assertEquals(3, profile.summaryCalls.size)
    }

    @Test fun personTheApiDoesNotKnowIsSkippedAndTheSyncStillSucceeds() = runTest {
        serverWith("c1", 1)

        val sync = startSync()

        assertTrue(userDao.rows.isEmpty())
        assertEquals(ApiResult.Success(Unit), sync.result.value)
    }
}
