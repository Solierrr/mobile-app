package com.project.solaria_mobile

import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.realtime.AuthFailure
import com.project.solaria_mobile.core.network.realtime.RealtimeEndpoint
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import com.project.solaria_mobile.core.network.realtime.RealtimeTokenRefresher
import com.project.solaria_mobile.core.network.realtime.StompAuthException
import com.project.solaria_mobile.core.network.realtime.StompConnector
import com.project.solaria_mobile.core.network.realtime.StompLink
import com.project.solaria_mobile.core.network.realtime.StompRealtimeClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeTokenRecoveryTest {
    @Test fun aLaterExpirationCanRenewTheReplacementTokenAgain() = runTest {
        var token = "token-0"
        var refreshes = 0
        val losses = mutableListOf<kotlinx.coroutines.CompletableDeferred<AuthFailure?>>()
        val connector = object : StompConnector {
            override suspend fun connect(url: String, token: String): StompLink {
                val loss = kotlinx.coroutines.CompletableDeferred<AuthFailure?>().also(losses::add)
                return object : StompLink {
                    override fun subscribe(destination: String) = emptyFlow<String>()
                    override suspend fun send(destination: String, body: String) = Unit
                    override suspend fun close() = Unit
                    override suspend fun awaitLoss() = loss.await()
                }
            }
        }
        val client = StompRealtimeClient(connector, RealtimeEndpoint { "wss://example/ws" },
            object : CredentialProvider { override fun accessToken() = token }, backgroundScope,
            RealtimeTokenRefresher { refreshes++; token = "token-$refreshes"; token })
        client.connect()
        runCurrent()
        losses[0].complete(AuthFailure.TOKEN_REJECTED)
        runCurrent()
        losses[1].complete(AuthFailure.TOKEN_REJECTED)
        runCurrent()
        assertEquals(2, refreshes)
        assertEquals(3, losses.size)
        assertEquals(RealtimeState.Connected, client.state.value)
    }

    @Test fun anExpiredTokenIsRenewedAndReconnectsWithoutReturningToTheForeground() = runTest {
        var token = "expired"
        val attempts = mutableListOf<String>()
        val connector = object : StompConnector {
            override suspend fun connect(url: String, token: String): StompLink {
                attempts += token
                if (token == "expired") throw StompAuthException(AuthFailure.TOKEN_REJECTED)
                return idleLink()
            }
        }
        val client = StompRealtimeClient(connector, RealtimeEndpoint { "wss://example/ws" },
            object : CredentialProvider { override fun accessToken() = token }, backgroundScope,
            RealtimeTokenRefresher { token = "renewed"; token })
        client.connect()
        runCurrent()
        assertEquals(listOf("expired", "renewed"), attempts)
        assertEquals(RealtimeState.Connected, client.state.value)
    }

    @Test fun aRejectedReplacementStopsInsteadOfRefreshingForever() = runTest {
        var token = "expired"
        var refreshes = 0
        val connector = object : StompConnector {
            override suspend fun connect(url: String, token: String): StompLink = throw StompAuthException(AuthFailure.TOKEN_REJECTED)
        }
        val client = StompRealtimeClient(connector, RealtimeEndpoint { "wss://example/ws" },
            object : CredentialProvider { override fun accessToken() = token }, backgroundScope,
            RealtimeTokenRefresher { refreshes++; token = "renewed"; token })
        client.connect()
        runCurrent()
        assertEquals(1, refreshes)
        assertEquals(RealtimeState.AuthFailed(AuthFailure.TOKEN_REJECTED), client.state.value)
    }

    private fun idleLink() = object : StompLink {
        override fun subscribe(destination: String) = emptyFlow<String>()
        override suspend fun send(destination: String, body: String) = Unit
        override suspend fun close() = Unit
        override suspend fun awaitLoss(): AuthFailure? = awaitCancellation()
    }
}
