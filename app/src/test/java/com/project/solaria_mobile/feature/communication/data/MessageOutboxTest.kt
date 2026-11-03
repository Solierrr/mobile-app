package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryPendingMessageDao : PendingMessageDao {
    val items = MutableStateFlow<List<PendingMessage>>(emptyList())

    override fun observe(account: String, conversationId: String): Flow<List<PendingMessage>> =
        items.map { all -> all.filter { it.account == account && it.conversationId == conversationId }.sortedBy { it.createdAt } }

    override suspend fun pending(account: String) =
        items.value.filter { it.account == account && it.state == PendingState.PENDING }.sortedBy { it.createdAt }

    override suspend fun get(id: String) = items.value.firstOrNull { it.id == id }
    override suspend fun insert(message: PendingMessage) { items.value = items.value + message }
    override suspend fun update(message: PendingMessage) { items.value = items.value.map { if (it.id == message.id) message else it } }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun resetInterrupted() {
        items.value = items.value.map { if (it.state == PendingState.SENDING) it.copy(state = PendingState.PENDING, uncertain = true) else it }
    }
    override suspend fun clear() { items.value = emptyList() }
}

/** Repositório controlável: registra os envios e responde com a falha programada, se houver. */
class ScriptedRepository(private val base: CommunicationRepository = com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository()) :
    CommunicationRepository by base {
    val sent = mutableListOf<String>()
    val history = mutableListOf<ChatMessage>()
    var failures = ArrayDeque<ErrorKind>()
    var deliverBeforeFailing = false
    var historyFails = false

    override suspend fun sendMessage(conversationId: String, text: String, clientMessageId: String?): ApiResult<ChatMessage> {
        val failure = failures.removeFirstOrNull()
        if (failure != null) {
            if (deliverBeforeFailing) history += ChatMessage(text, true, "Agora", clientMessageId)
            return ApiResult.Failure(failure)
        }
        sent += text
        val message = ChatMessage(text, true, "Agora", clientMessageId)
        history += message
        return ApiResult.Success(message)
    }

    override suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>> =
        if (historyFails) ApiResult.Failure(ErrorKind.CONNECTIVITY) else ApiResult.Success(history.toList())
}

fun testOutbox(repository: CommunicationRepository, dao: PendingMessageDao = InMemoryPendingMessageDao(), account: () -> String? = { "conta-a" }): MessageOutbox {
    var clock = 0L
    return MessageOutbox(dao, repository, OutboxAccount(account), flowOf(true), Dispatchers.Unconfined) { clock++ }
}

class MessageOutboxTest {
    @Test fun cancellationReturnsTheInFlightMessageToTheQueueAsUncertain() = runTest {
        val dao = InMemoryPendingMessageDao()
        val sending = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repository = object : CommunicationRepository by FakeCommunicationRepository() {
            override suspend fun sendMessage(conversationId: String, text: String, clientMessageId: String?): ApiResult<ChatMessage> {
                sending.complete(Unit)
                kotlinx.coroutines.awaitCancellation()
            }
        }
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "oi")
        val job = kotlinx.coroutines.CoroutineScope(coroutineContext).launch { outbox.drain() }
        sending.await()
        job.cancel()
        job.join()
        assertEquals(PendingState.PENDING, dao.items.value.single().state)
        assertTrue(dao.items.value.single().uncertain)
    }

    @Test fun repeatedUnauthorizedResponsesStopAfterThreeAttempts() = runTest {
        val repository = ScriptedRepository().apply { repeat(4) { failures.add(ErrorKind.UNAUTHORIZED) } }
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "oi", "GROUP")
        assertTrue(outbox.drain().retryLater)
        assertTrue(outbox.drain().retryLater)
        assertFalse(outbox.drain().retryLater)
        assertEquals(PendingState.FAILED, dao.items.value.single().state)
        assertEquals("GROUP", dao.items.value.single().conversationType)
        assertEquals(3, dao.items.value.single().authAttempts)
    }

    @Test fun anUncertainDeliveryCanBeFoundBeyondTheMostRecentPage() = runTest {
        val dao = InMemoryPendingMessageDao()
        dao.insert(PendingMessage("client-id", "conta-a", "c1", "oi", 0, uncertain = true))
        var sent = false
        val repository = object : CommunicationRepository by FakeCommunicationRepository() {
            override suspend fun messages(conversationId: String) = ApiResult.Success(listOf(ChatMessage("nova", false, "", id = "new", sequence = 100)))
            override suspend fun olderMessages(conversationId: String, beforeSequence: Int) = ApiResult.Success(listOf(ChatMessage("oi", true, "", clientMessageId = "client-id", id = "delivered", sequence = 20)))
            override suspend fun sendMessage(conversationId: String, text: String, clientMessageId: String?): ApiResult<ChatMessage> {
                sent = true
                return ApiResult.Success(ChatMessage(text, true, ""))
            }
        }
        testOutbox(repository, dao).drain()
        assertFalse(sent)
        assertTrue(dao.items.value.isEmpty())
    }

    @Test fun sendsQueuedMessagesInCreationOrder() = runTest {
        val repository = ScriptedRepository()
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "primeira")
        outbox.enqueue("c1", "segunda")

        val result = outbox.drain()

        assertEquals(listOf("primeira", "segunda"), repository.sent)
        assertTrue(dao.items.value.isEmpty())
        assertFalse(result.retryLater)
    }

    @Test fun connectivityFailureKeepsTheMessageForLaterWithoutSkippingIt() = runTest {
        val repository = ScriptedRepository().apply { failures.add(ErrorKind.CONNECTIVITY) }
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "primeira")
        outbox.enqueue("c1", "segunda")

        val result = outbox.drain()

        assertTrue(result.retryLater)
        assertTrue(repository.sent.isEmpty())
        assertEquals(listOf(PendingState.PENDING, PendingState.PENDING), dao.items.value.map { it.state })
        assertTrue(dao.items.value.first().uncertain)
    }

    @Test fun messageDeliveredBeforeATimeoutIsNotSentTwice() = runTest {
        val repository = ScriptedRepository().apply {
            failures.add(ErrorKind.TIMEOUT)
            deliverBeforeFailing = true
        }
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "chegou mesmo assim")

        outbox.drain()
        outbox.drain()

        assertTrue(repository.sent.isEmpty())
        assertEquals(1, repository.history.count { it.text == "chegou mesmo assim" })
        assertTrue(dao.items.value.isEmpty())
    }

    @Test fun messageLostBeforeATimeoutIsSentAgain() = runTest {
        val repository = ScriptedRepository().apply { failures.add(ErrorKind.TIMEOUT) }
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "não chegou")

        outbox.drain()
        outbox.drain()

        assertEquals(listOf("não chegou"), repository.sent)
        assertTrue(dao.items.value.isEmpty())
    }

    @Test fun earlierMessageWithTheSameTextDoesNotCountAsDelivery() = runTest {
        val repository = ScriptedRepository().apply {
            history += ChatMessage("ok", true, "Ontem")
            failures.add(ErrorKind.TIMEOUT)
        }
        val outbox = testOutbox(repository)
        outbox.enqueue("c1", "ok")

        outbox.drain()
        outbox.drain()

        assertEquals(listOf("ok"), repository.sent)
    }

    @Test fun unreadableHistoryKeepsAnUncertainMessageWaiting() = runTest {
        val repository = ScriptedRepository().apply { failures.add(ErrorKind.TIMEOUT) }
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "talvez")
        outbox.drain()
        repository.historyFails = true

        val result = outbox.drain()

        assertTrue(result.retryLater)
        assertTrue(repository.sent.isEmpty())
        assertEquals(PendingState.PENDING, dao.items.value.single().state)
    }

    @Test fun rejectionFailsTheMessageAndContinuesWithTheNext() = runTest {
        val repository = ScriptedRepository().apply { failures.add(ErrorKind.FORBIDDEN) }
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "recusada")
        outbox.enqueue("c1", "seguinte")

        outbox.drain()

        assertEquals(listOf("seguinte"), repository.sent)
        val failed = dao.items.value.single()
        assertEquals(PendingState.FAILED, failed.state)
        assertEquals(ErrorKind.FORBIDDEN, failed.failure)
    }

    @Test fun retryAndDiscardActOnAFailedMessage() = runTest {
        val repository = ScriptedRepository().apply { failures.add(ErrorKind.VALIDATION) }
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(repository, dao)
        outbox.enqueue("c1", "de novo")
        outbox.enqueue("c1", "descartar")
        outbox.drain()
        val failedId = dao.items.value.single { it.text == "de novo" }.id

        outbox.retry(failedId)
        outbox.drain()

        assertEquals(listOf("descartar", "de novo"), repository.sent)
        outbox.enqueue("c1", "fica")
        val keptId = dao.items.value.single().id
        outbox.discard(keptId)
        assertTrue(dao.items.value.isEmpty())
    }

    @Test fun queueBelongsToTheAccountThatWroteIt() = runTest {
        val repository = ScriptedRepository()
        val dao = InMemoryPendingMessageDao()
        var account: String? = "conta-a"
        val outbox = testOutbox(repository, dao) { account }
        outbox.enqueue("c1", "da conta a")

        account = "conta-b"
        outbox.drain()
        assertTrue(repository.sent.isEmpty())

        account = null
        assertFalse(outbox.enqueue("c1", "sem conta"))

        account = "conta-a"
        outbox.drain()
        assertEquals(listOf("da conta a"), repository.sent)
    }

    @Test fun clearRemovesEverythingQueued() = runTest {
        val dao = InMemoryPendingMessageDao()
        val outbox = testOutbox(ScriptedRepository(), dao)
        outbox.enqueue("c1", "um")
        outbox.enqueue("c2", "dois")

        outbox.clear()

        assertTrue(dao.items.value.isEmpty())
    }
}
