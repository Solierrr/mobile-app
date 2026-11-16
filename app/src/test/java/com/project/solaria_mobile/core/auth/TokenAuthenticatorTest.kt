package com.project.solaria_mobile.core.auth

import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenAuthenticatorTest {
    private val initial = AuthTokens("access", "refresh")

    @Test fun unavailableRefreshPreservesTheSession() {
        for (status in listOf(400, 401, 403, 429, 500, 502, 503)) {
            val store = Store(initial)
            assertNull(authenticator(store, status, "{}").authenticate(null, unauthorized()))
            assertEquals(initial, store.currentTokens())
        }
    }

    @Test fun malformedSuccessPreservesTheSession() {
        for (content in listOf("broken", "{}", "{\"accessToken\":\"\",\"refreshToken\":\"new\"}")) {
            val store = Store(initial)
            assertNull(authenticator(store, 200, content).authenticate(null, unauthorized()))
            assertEquals(initial, store.currentTokens())
        }
    }

    @Test fun networkFailurePreservesTheSession() {
        val store = Store(initial)
        val client = OkHttpClient.Builder().addInterceptor { throw IOException("offline") }.build()
        assertNull(TokenAuthenticator(store, client, "https://qa.example/auth/refresh").authenticate(null, unauthorized()))
        assertEquals(initial, store.currentTokens())
    }

    @Test fun definitiveRejectionClearsTheSession() {
        for ((status, content) in listOf(401 to "{\"code\":\"INVALID_REFRESH_TOKEN\"}", 401 to "{\"code\":\"REFRESH_TOKEN_REUSED\"}", 400 to "{\"code\":\"REFRESH_TOKEN_EXPIRED\"}", 403 to "{\"code\":\"REFRESH_TOKEN_REVOKED\"}")) {
            val store = Store(initial)
            assertNull(authenticator(store, status, content).authenticate(null, unauthorized()))
            assertNull(store.currentTokens())
        }
    }

    @Test fun successfulRefreshPersistsTokensAndRetriesTheOriginalRequest() {
        val store = Store(initial)
        val retry = authenticator(store, 200, "{\"accessToken\":\"new-access\",\"refreshToken\":\"new-refresh\"}")
            .authenticate(null, unauthorized())
        assertEquals(AuthTokens("new-access", "new-refresh"), store.currentTokens())
        assertEquals("Bearer new-access", retry?.header("Authorization"))
    }

    @Test fun logoutDuringRefreshCannotRestoreTheSession() {
        val store = Store(initial)
        val retry = authenticator(store, 200, "{\"accessToken\":\"new-access\",\"refreshToken\":\"new-refresh\"}") {
            runBlocking { store.clearTokens() }
        }.authenticate(null, unauthorized())
        assertNull(retry)
        assertNull(store.currentTokens())
    }

    @Test fun staleRejectionCannotClearANewerLogin() {
        val store = Store(initial)
        val newer = AuthTokens("other-access", "other-refresh")
        authenticator(store, 401, "{\"code\":\"INVALID_REFRESH_TOKEN\"}") { runBlocking { store.saveTokens(newer) } }.authenticate(null, unauthorized())
        assertEquals(newer, store.currentTokens())
    }

    @Test fun concurrentRequestReusesAnAlreadyRenewedToken() {
        val store = Store(AuthTokens("new-access", "new-refresh"))
        val retry = authenticator(store, 500, "{}").authenticate(null, unauthorized())
        assertEquals("Bearer new-access", retry?.header("Authorization"))
    }

    private fun authenticator(store: TokenStore, status: Int, content: String, beforeResponse: () -> Unit = {}) =
        TokenAuthenticator(store, OkHttpClient.Builder().addInterceptor {
            beforeResponse()
            Response.Builder().request(it.request()).protocol(Protocol.HTTP_1_1).code(status).message("test")
                .body(content.toResponseBody()).build()
        }.build(), "https://qa.example/auth/refresh")

    private fun unauthorized() = Response.Builder()
        .request(Request.Builder().url("https://qa.example/projects").header("Authorization", "Bearer access").build())
        .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").body("".toResponseBody()).build()

    private class Store(initial: AuthTokens?) : TokenStore {
        override val tokens = MutableStateFlow(initial)
        override fun currentTokens() = tokens.value
        override suspend fun saveTokens(tokens: AuthTokens) { this.tokens.value = tokens }
        override suspend fun clearTokens() { tokens.value = null }
        override suspend fun savePendingLogin(pending: PendingLogin) = Unit
        override suspend fun takePendingLogin(): PendingLogin? = null
    }
}
