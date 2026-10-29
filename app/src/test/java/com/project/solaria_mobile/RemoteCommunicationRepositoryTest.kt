package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.communication.data.ConversationStateSync
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.database.ConversationEntity
import com.project.solaria_mobile.core.database.LocalDataWiper
import com.project.solaria_mobile.core.database.MessageEntity
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.database.UserEntity
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.MessageOutbox
import com.project.solaria_mobile.feature.communication.data.MessageStore
import com.project.solaria_mobile.feature.communication.data.MessageSync
import com.project.solaria_mobile.feature.communication.data.RemoteCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.UserDirectory
import com.project.solaria_mobile.feature.communication.domain.MessageStatus
import com.project.solaria_mobile.feature.profile.data.UserSummaryDto
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do [RemoteCommunicationRepository] lendo do banco: o que a lista e o chat mostram para o que está
 * guardado, com nomes, mensagens pendentes, exclusões, perda de acesso e chatbot.
 *
 * Usa dublês em memória do Room e do messenger. Nenhum teste aqui usa a rede de verdade.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteCommunicationRepositoryTest {
    private val conversationDao = InMemoryConversationDao()
    private val messageDao = InMemoryMessageDao(conversationDao)
    private val userDao = InMemoryUserDao()
    private val pendingDao = InMemoryPendingMessageDao()
    private val api = ScriptedMessengerApi()
    private val profile = FakeProfileApi()
    private val network = MutableStateFlow(true)
    private val store = MessageStore(messageDao, conversationDao)

    private fun conversation(
        id: String = "c1",
        type: String = "GROUP",
        title: String? = "Equipe",
        participants: String = "user-1,user-2,user-3",
        archived: Boolean = false,
        lost: Boolean = false,
        preview: String? = "oi",
        previewDeleted: Boolean = false,
    ) = ConversationEntity(id, "e1", 1, type, participants, title, archived, 1_793_181_600_000, "$id-m1", 1, preview, previewDeleted, lost)

    private fun message(
        sequence: Int,
        sender: String = "user-2",
        content: String? = "mensagem $sequence",
        edited: Boolean = false,
        deleted: Boolean = false,
        clientId: String? = null,
        conversationId: String = "c1",
    ) = MessageEntity("$conversationId-m$sequence", conversationId, sequence, sender, clientId, content, edited, deleted, 1_793_181_600_000, sequence.toLong())

    private fun TestScope.repository(): RemoteCommunicationRepository {
        val executor = ApiCallExecutor(ApiErrorParser(ApiJson.instance))
        val credentials = object : CredentialProvider { override fun accessToken() = tokenFor("user-1") }
        val networkMonitor = object : NetworkMonitor { override val isOnline = network }
        val outbox = MessageOutbox(pendingDao, api, executor, networkMonitor, credentials, store, backgroundScope)
        val users = UserDirectory(profile, executor, userDao, conversationDao, messageDao)
        val sync = MessageSync(
            api = api,
            executor = executor,
            tokens = FakeTokenStore(),
            realtime = FakeRealtimeClient(),
            networkMonitor = networkMonitor,
            json = ApiJson.instance,
            store = store,
            conversations = conversationDao,
            messages = messageDao,
            syncState = InMemorySyncStateDao(),
            users = users,
            wiper = LocalDataWiper { },
            scope = backgroundScope,
        )
        val stateSync = ConversationStateSync(api, executor, networkMonitor, conversationDao, messageDao, backgroundScope)
        return RemoteCommunicationRepository(api, executor, credentials, outbox, networkMonitor, store, conversationDao, users, sync, FakeCommunicationRepository(), stateSync)
    }

    @Test fun listShowsStoredConversationsWithoutNetworkAndHidesArchivedOnes() = runTest {
        network.value = false
        conversationDao.put(conversation("c1"))
        conversationDao.put(conversation("c2", archived = true))

        val list = repository().conversations().first()

        assertEquals(listOf("c1"), list.map { it.id })
        assertEquals("oi", list.single().preview)
        assertFalse(list.single().accessLost)
    }

    @Test fun directConversationTakesTheNameOfTheOtherPersonOrFallsBackToConversation() = runTest {
        conversationDao.put(conversation("d1", type = "DIRECT", title = null, participants = "user-1,user-2"))
        val repository = repository()
        assertEquals("Conversa", repository.conversation("d1").first()?.name)

        userDao.put(UserEntity("user-2", "Ana Bia", null))

        assertEquals("Ana Bia", repository.conversation("d1").first()?.name)
        assertEquals("AB", repository.conversation("d1").first()?.initials)
    }

    @Test fun groupKeepsItsTitleEvenWhenParticipantNamesAreKnown() = runTest {
        conversationDao.put(conversation("c1", title = "Equipe"))
        userDao.put(UserEntity("user-2", "Ana Bia", null))

        assertEquals("Equipe", repository().conversation("c1").first()?.name)
    }

    @Test fun lostAccessReachesTheScreenModelAndDeletedPreviewShowsTheDeletedText() = runTest {
        conversationDao.put(conversation("c1", lost = true, preview = null, previewDeleted = true))

        val item = repository().conversations().first().single()

        assertTrue(item.accessLost)
        assertEquals("Mensagem apagada", item.preview)
    }

    @Test fun chatShowsStoredMessagesInSequenceOrderAndTheQueuedOnesAfterThem() = runTest {
        conversationDao.put(conversation())
        messageDao.upsert(listOf(message(2, clientId = "x"), message(1)))
        pendingDao.insert(PendingMessageEntity(clientMessageId = "x", senderId = "user-1", conversationId = "c1", messageType = "USER_TO_GROUP", content = "já confirmada", createdAt = 1))
        pendingDao.insert(PendingMessageEntity(clientMessageId = "y", senderId = "user-1", conversationId = "c1", messageType = "USER_TO_GROUP", content = "na fila", createdAt = 2))

        val timeline = repository().messages("c1").first()

        assertEquals(listOf("c1-m1", "c1-m2", "y"), timeline.map { it.id })
        assertEquals(MessageStatus.PENDING, timeline.last().status)
        assertTrue(timeline.last().mine)
    }

    @Test fun groupBubblesCarryTheNameOfTheSenderOnlyForOtherPeople() = runTest {
        conversationDao.put(conversation("c1", type = "GROUP"))
        userDao.put(UserEntity("user-2", "Ana Bia", null))
        userDao.put(UserEntity("user-1", "Eu", null))
        messageDao.upsert(listOf(message(1, sender = "user-2"), message(2, sender = "USER-1"), message(3, sender = "user-9")))

        val timeline = repository().messages("c1").first()

        assertEquals(listOf("Ana Bia", null, null), timeline.map { it.sender })
        assertEquals(listOf(false, true, false), timeline.map { it.mine })
    }

    @Test fun directConversationBubblesHaveNoSenderLabel() = runTest {
        conversationDao.put(conversation("d1", type = "DIRECT", title = null, participants = "user-1,user-2"))
        userDao.put(UserEntity("user-2", "Ana Bia", null))
        messageDao.upsert(listOf(message(1, conversationId = "d1")))

        assertNull(repository().messages("d1").first().single().sender)
    }

    @Test fun editedMessageIsFlaggedAndDeletedMessageShowsTheDeletedText() = runTest {
        conversationDao.put(conversation())
        messageDao.upsert(listOf(message(1, edited = true), message(2, content = null, deleted = true, edited = true)))

        val timeline = repository().messages("c1").first()

        assertTrue(timeline[0].edited)
        assertEquals("Mensagem apagada", timeline[1].text)
        assertFalse("mensagem excluída não é mostrada como editada", timeline[1].edited)
    }

    @Test fun sendUsesTheStoredConversationTypeEvenOffline() = runTest {
        network.value = false
        conversationDao.put(conversation("g1", type = "GROUP"))
        val repository = repository()

        val result = repository.sendMessage("g1", "oi grupo")
        runCurrent()

        assertEquals(ApiResult.Success(Unit), result)
        assertEquals("USER_TO_GROUP", pendingDao.rows.single().messageType)
        assertTrue(api.sendRequests.isEmpty())
    }

    @Test fun sendIsRefusedForAConversationWithLostAccessOrUnknown() = runTest {
        conversationDao.put(conversation("c1", lost = true))
        val repository = repository()

        assertEquals(ApiResult.Failure(ErrorKind.FORBIDDEN), repository.sendMessage("c1", "oi"))
        assertEquals(ApiResult.Failure(ErrorKind.NOT_FOUND), repository.sendMessage("desconhecida", "oi"))
        assertTrue(pendingDao.rows.isEmpty())
    }

    @Test fun openWithAStoredConversationDoesNotTouchTheNetwork() = runTest {
        conversationDao.put(conversation())

        val result = repository().open("c1")

        assertEquals(ApiResult.Success(Unit), result)
        assertEquals(0, api.historyCalls)
    }

    @Test fun openFetchesAMissingConversationAndStoresIt() = runTest {
        api.conversationItems += ConversationDto(id = "novo", conversationType = "GROUP", participantIds = listOf("user-1", "user-2"), title = "Novo")

        val repository = repository()
        val result = repository.open("novo")

        assertEquals(ApiResult.Success(Unit), result)
        assertEquals("Novo", repository.conversation("novo").first()?.name)
    }

    @Test fun openReturnsTheErrorWhenTheConversationIsMissingAndTheNetworkFails() = runTest {
        api.conversationFailure = IOException("sem rede")

        val result = repository().open("novo")

        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), result)
        assertTrue(conversationDao.rows.isEmpty())
    }

    @Test fun openLoadsTheChatbotHistoryIntoMemoryAndNeverIntoTheDatabase() = runTest {
        conversationDao.put(conversation("bot", type = "CHAT_BOT", title = null, participants = "user-1"))
        api.serverMessage("bot", 1, content = "pergunta", senderId = "user-1")
        api.serverMessage("bot", 2, content = "resposta")
        val repository = repository()

        val result = repository.open("bot")

        assertEquals(ApiResult.Success(Unit), result)
        assertEquals(listOf("pergunta", "resposta"), repository.messages("bot").first().map { it.text })
        assertTrue(messageDao.rows.isEmpty())
    }

    @Test fun openOfTheChatbotFailsWithoutNetworkEvenWithAStoredConversation() = runTest {
        conversationDao.put(conversation("bot", type = "CHAT_BOT", title = null, participants = "user-1"))
        api.historyFailure = IOException("sem rede")

        val result = repository().open("bot")

        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), result)
    }

    @Test fun refreshNamesFetchesThePeopleOfTheOpenConversation() = runTest {
        conversationDao.put(conversation("c1", type = "GROUP", participants = "user-1,user-2,user-3"))
        messageDao.upsert(listOf(message(1, sender = "user-9")))
        profile.summaries["user-2"] = UserSummaryDto("Ana", null)
        profile.summaries["user-3"] = UserSummaryDto("Beto", null)
        profile.summaries["user-9"] = UserSummaryDto("Caio", null)

        repository().refreshNames("c1")

        assertEquals(listOf("Ana", "Beto", "Caio"), userDao.rows.map { it.username }.sorted())
        assertFalse("o próprio usuário não é consultado", "user-1" in profile.summaryCalls)
    }
}
