package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.MessageOutbox
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Testes da fila de envio ([MessageOutbox]): envio offline, ordem, reenvio, confirmação pelo histórico, recusas,
 * troca de conta e 401.
 *
 * Usa [ScriptedMessengerApi] e [InMemoryPendingMessageDao] como dublês.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageOutboxTest {
    private val dao = InMemoryPendingMessageDao()
    private val api = ScriptedMessengerApi()
    private val network = MutableStateFlow(false)
    private var token: String? = tokenFor("user-1")

    private fun TestScope.startOutbox(): MessageOutbox =
        MessageOutbox(
            dao = dao,
            api = api,
            executor = ApiCallExecutor(ApiErrorParser(ApiJson.instance)),
            networkMonitor = object : NetworkMonitor { override val isOnline = network },
            credentials = object : CredentialProvider { override fun accessToken() = token },
            scope = backgroundScope,
        ).also {
            it.start()
            runCurrent()
        }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))

    @Test fun offlineMessageWaitsInTheQueueAndIsSentWhenTheConnectionReturns() = runTest {
        val outbox = startOutbox()

        assertEquals(ApiResult.Success(Unit), outbox.enqueue("c1", "USER_TO_USER", "oi"))
        runCurrent()
        assertTrue(api.sendRequests.isEmpty())
        assertEquals(1, dao.rows.size)

        network.value = true
        runCurrent()
        assertEquals(listOf("oi"), api.sendRequests.map { it.content })
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun onlineMessageIsSentRightAwayAndAnnouncedAsDelivered() = runTest {
        network.value = true
        val outbox = startOutbox()
        val delivered = mutableListOf<MessageDto>()
        backgroundScope.launch { outbox.delivered.toList(delivered) }
        runCurrent()

        outbox.enqueue("c1", "USER_TO_USER", "oi")
        runCurrent()

        assertEquals(1, delivered.size)
        assertEquals(api.sendRequests.single().clientMessageId, delivered.single().clientMessageId)
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun queuedMessagesAreSentInTheOrderTheyWereWritten() = runTest {
        val outbox = startOutbox()
        listOf("primeira", "segunda", "terceira").forEach { outbox.enqueue("c1", "USER_TO_USER", it) }

        network.value = true
        runCurrent()

        assertEquals(listOf("primeira", "segunda", "terceira"), api.sendRequests.map { it.content })
    }

    @Test fun lostResponseIsConfirmedFromTheHistoryInsteadOfSendingTwice() = runTest {
        network.value = true
        val outbox = startOutbox()
        api.persistBeforeFailing = true
        api.sendFailures += IOException("conexão caiu depois do insert")

        outbox.enqueue("c1", "USER_TO_USER", "oi")
        runCurrent()
        assertEquals(1, dao.rows.single().attempts)

        advanceTimeBy(15_001)
        runCurrent()

        assertEquals(1, api.sendRequests.size)
        assertEquals(1, api.history.size)
        assertEquals(1, api.historyCalls)
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun failedAttemptThatNeverReachedTheServerIsSentAgain() = runTest {
        network.value = true
        val outbox = startOutbox()
        api.sendFailures += IOException("sem resposta")

        outbox.enqueue("c1", "USER_TO_USER", "oi")
        runCurrent()
        advanceTimeBy(15_001)
        runCurrent()

        assertEquals(2, api.sendRequests.size)
        assertEquals(1, api.history.size)
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun rejectedMessageIsMarkedAsFailedWithoutBlockingTheOthers() = runTest {
        val outbox = startOutbox()
        api.sendFailures += httpError(403)
        outbox.enqueue("c1", "USER_TO_USER", "recusada")
        outbox.enqueue("c1", "USER_TO_USER", "aceita")

        network.value = true
        runCurrent()

        val rejected = dao.rows.single()
        assertEquals("recusada", rejected.content)
        assertTrue(rejected.failed)
        assertEquals(listOf("aceita"), api.history.map { it.content })

        outbox.retry(rejected.clientMessageId)
        runCurrent()
        assertTrue(dao.rows.isEmpty())
        assertEquals(listOf("aceita", "recusada"), api.history.map { it.content })
    }

    @Test fun messagesWrittenByAnotherUserAreNeverSent() = runTest {
        val outbox = startOutbox()
        outbox.enqueue("c1", "USER_TO_USER", "de outra conta")

        token = tokenFor("user-2")
        network.value = true
        runCurrent()

        assertTrue(api.sendRequests.isEmpty())
        assertEquals(1, dao.rows.size)
    }

    @Test fun unauthorizedStopsUntilTheNextTrigger() = runTest {
        network.value = true
        val outbox = startOutbox()
        api.sendFailures += httpError(401)

        outbox.enqueue("c1", "USER_TO_USER", "oi")
        runCurrent()
        advanceTimeBy(600_000)
        runCurrent()

        assertEquals(1, api.sendRequests.size)
        assertEquals(0, api.historyCalls)
        assertEquals(1, dao.rows.size)
    }

    @Test fun enqueueWithoutSessionIsRefused() = runTest {
        token = null
        val outbox = startOutbox()

        val result = outbox.enqueue("c1", "USER_TO_USER", "oi")

        assertTrue(result is ApiResult.Failure)
        assertTrue(dao.rows.isEmpty())
    }
}
