package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.realtime.AuthFailure
import com.project.solaria_mobile.core.network.realtime.RealtimeEndpoint
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import com.project.solaria_mobile.core.network.realtime.StompAuthException
import com.project.solaria_mobile.core.network.realtime.StompConnector
import com.project.solaria_mobile.core.network.realtime.StompLink
import com.project.solaria_mobile.core.network.realtime.StompRealtimeClient
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StompSessionTest {
    private class Link : StompLink {
        val lost = CompletableDeferred<AuthFailure?>()
        var closed = false
        val sent = mutableListOf<String>()
        override fun subscribe(destination: String) = emptyFlow<String>()
        override suspend fun send(destination: String, body: String) { sent += body }
        override suspend fun close() { closed = true }
        override suspend fun awaitLoss() = lost.await()
    }
    private val endpoint = mockk<RealtimeEndpoint> { every { url() } returns "wss://qa.example/messaging/ws" }

    @Test fun delayedOldConnectionCannotReplaceTheNewAccountSocket() = runTest {
        var token: String? = "alice"
        val gate = CompletableDeferred<Unit>()
        val old = Link()
        val current = Link()
        val connector = object : StompConnector {
            override suspend fun connect(url: String, token: String): StompLink = if (token == "alice") withContext(NonCancellable) { gate.await(); old } else current
        }
        val client = StompRealtimeClient(connector, endpoint, object : CredentialProvider { override fun accessToken() = token }, backgroundScope)
        client.connect()
        runCurrent()
        client.disconnect()
        token = "bob"
        client.connect()
        runCurrent()
        assertEquals(RealtimeState.Connected, client.state.value)
        gate.complete(Unit)
        runCurrent()
        assertTrue(old.closed)
        assertEquals(ApiResult.Success(Unit), client.send("/app/chat", "only-bob"))
        assertEquals(listOf("only-bob"), current.sent)
        assertTrue(old.sent.isEmpty())
        token = "carol"
        assertEquals(ApiResult.Failure(ErrorKind.UNAUTHORIZED), client.send("/app/chat", "blocked"))
        client.disconnect()
        runCurrent()
        assertTrue(current.closed)
    }

    @Test fun rejectedTokenStopsRetriesAndNewLoginCanReconnect() = runTest {
        var token = "rejected"
        var calls = 0
        val connector = object : StompConnector {
            override suspend fun connect(url: String, token: String): StompLink {
                calls++
                if (token == "rejected") throw StompAuthException(AuthFailure.TOKEN_REJECTED)
                return Link()
            }
        }
        val client = StompRealtimeClient(connector, endpoint, object : CredentialProvider { override fun accessToken() = token }, backgroundScope)
        client.connect()
        runCurrent()
        advanceTimeBy(60_000)
        assertEquals(1, calls)
        assertEquals(RealtimeState.AuthFailed(AuthFailure.TOKEN_REJECTED), client.state.value)
        token = "renewed"
        client.connect()
        runCurrent()
        assertEquals(RealtimeState.Connected, client.state.value)
        assertEquals(2, calls)
        client.disconnect()
    }
}
