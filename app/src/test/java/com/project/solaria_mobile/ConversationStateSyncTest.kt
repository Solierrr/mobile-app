package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.database.ConversationEntity
import com.project.solaria_mobile.core.database.MessageEntity
import com.project.solaria_mobile.core.database.displayedUnread
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.feature.communication.data.ConversationStateRequest
import com.project.solaria_mobile.feature.communication.data.ConversationStateSync
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do estado pessoal das conversas: a leitura feita offline e enviada depois, o que acontece quando o servidor a
 * recusa, e silenciar e arquivar, que exigem internet.
 *
 * Usa dublês em memória do Room e do messenger.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationStateSyncTest {
    private val conversationDao = InMemoryConversationDao()
    private val messageDao = InMemoryMessageDao(conversationDao)
    private val api = ScriptedMessengerApi()
    private val network = MutableStateFlow(true)

    private fun TestScope.stateSync(): ConversationStateSync {
        val executor = ApiCallExecutor(ApiErrorParser(ApiJson.instance))
        val monitor = object : NetworkMonitor { override val isOnline = network }
        return ConversationStateSync(api, executor, monitor, conversationDao, messageDao, backgroundScope).also { it.start() }
    }

    // Conversa com [count] mensagens guardadas, nenhuma lida, e as mesmas no servidor
    private suspend fun seed(id: String = "c1", count: Int = 3, type: String = "GROUP") {
        conversationDao.put(
            ConversationEntity(id, "e1", 1, type, "user-1,user-2", "Equipe", false, 1L, "$id-m$count", count, "oi", false, false, unreadCount = count.toLong()),
        )
        val sent = (1..count).map { api.serverMessage(id, it) }
        messageDao.upsert(sent.map { MessageEntity(it.id, id, it.sequence, it.senderId, null, it.content, false, false, 1L, it.changeVersion) })
    }

    @Test fun readingOfflineIsKeptLocallyAndSentWhenTheInternetReturns() = runTest {
        seed()
        network.value = false
        val sync = stateSync()
        runCurrent()

        sync.markRead("c1")
        runCurrent()

        val offline = conversationDao.find("c1")!!
        assertEquals(3, offline.localReadSequence)
        assertEquals(0, offline.lastReadSequence)
        assertEquals(0L, offline.displayedUnread)
        assertTrue(api.stateRequests.isEmpty())

        network.value = true
        runCurrent()

        assertEquals(listOf("c1" to ConversationStateRequest(lastReadSequence = 3)), api.stateRequests)
        val sent = conversationDao.find("c1")!!
        assertEquals(3, sent.lastReadSequence)
        assertEquals(0L, sent.unreadCount)
        assertTrue(conversationDao.pendingReads().isEmpty())
    }

    @Test fun readingOnlineIsSentRightAway() = runTest {
        seed()
        val sync = stateSync()
        runCurrent()

        sync.markRead("c1")
        runCurrent()

        assertEquals(1, api.stateRequests.size)
        assertEquals(3, conversationDao.find("c1")!!.lastReadSequence)
    }

    @Test fun readingNeverGoesBackwards() = runTest {
        seed()
        val sync = stateSync()
        runCurrent()
        sync.markRead("c1")
        runCurrent()

        conversationDao.markReadLocally("c1", 1)

        assertEquals(3, conversationDao.find("c1")!!.localReadSequence)
    }

    @Test fun valueAboveTheServerHeadIsDroppedInsteadOfRetriedForever() = runTest {
        seed()
        conversationDao.markReadLocally("c1", 9)
        stateSync()
        runCurrent()

        assertEquals(1, api.stateRequests.size)
        val after = conversationDao.find("c1")!!
        assertEquals(0, after.localReadSequence)
        assertTrue(conversationDao.pendingReads().isEmpty())
    }

    @Test fun lostAccessDropsThePendingRead() = runTest {
        seed()
        api.forbidden += "c1"
        val sync = stateSync()
        runCurrent()

        sync.markRead("c1")
        runCurrent()

        assertEquals(1, api.stateRequests.size)
        assertTrue(conversationDao.pendingReads().isEmpty())
    }

    @Test fun networkFailureKeepsTheReadPendingAndRetriesAfterThirtySeconds() = runTest {
        seed()
        api.stateFailure = IOException("sem rede")
        val sync = stateSync()
        runCurrent()

        sync.markRead("c1")
        runCurrent()

        assertEquals(1, api.stateRequests.size)
        assertEquals(1, conversationDao.pendingReads().size)

        api.stateFailure = null
        advanceTimeBy(30_001)
        runCurrent()

        assertEquals(2, api.stateRequests.size)
        assertTrue(conversationDao.pendingReads().isEmpty())
    }

    @Test fun chatbotConversationsHaveNothingToMark() = runTest {
        conversationDao.put(ConversationEntity("bot", "e1", 1, "CHAT_BOT", "user-1", null, false, 1L, null, 0, null, false, false))
        val sync = stateSync()
        runCurrent()

        sync.markRead("bot")
        runCurrent()

        assertTrue(api.stateRequests.isEmpty())
    }

    @Test fun muteAndArchiveNeedInternet() = runTest {
        seed()
        network.value = false
        val sync = stateSync()
        runCurrent()

        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), sync.setMuted("c1", true))
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), sync.setArchived("c1", true))
        assertTrue(api.stateRequests.isEmpty())
    }

    @Test fun mutingAndArchivingAreStoredFromTheServerAnswer() = runTest {
        seed()
        val sync = stateSync()
        runCurrent()

        assertEquals(ApiResult.Success(Unit), sync.setMuted("c1", true))
        assertEquals(ApiResult.Success(Unit), sync.setArchived("c1", true))

        assertEquals(listOf("c1" to ConversationStateRequest(muted = true), "c1" to ConversationStateRequest(archived = true)), api.stateRequests)
        val after = conversationDao.find("c1")!!
        assertTrue(after.muted)
        assertTrue(after.archived)

        assertEquals(ApiResult.Success(Unit), sync.setMuted("c1", false))
        assertEquals(false, conversationDao.find("c1")!!.muted)
    }

    @Test fun serverRefusalOfMuteReachesTheCaller() = runTest {
        seed()
        api.forbidden += "c1"
        val sync = stateSync()
        runCurrent()

        assertEquals(ApiResult.Failure(ErrorKind.FORBIDDEN), sync.setMuted("c1", true))
        assertEquals(false, conversationDao.find("c1")!!.muted)
    }
}
