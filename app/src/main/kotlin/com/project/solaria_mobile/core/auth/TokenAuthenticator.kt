package com.project.solaria_mobile.core.auth

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.network.ApiJson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.time.Duration

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
) : Authenticator {
    private val json: Json = ApiJson.instance
    private val client = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(60))
        .build()

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.endsWith(REFRESH_PATH) || responseCount(response) >= MAX_ATTEMPTS) return null
        val usedToken = response.request.header(AUTHORIZATION)?.removePrefix("Bearer ")
        val current = tokenStore.currentTokens() ?: return null
        if (usedToken != null && usedToken != current.accessToken) {
            return response.request.newBuilder().header(AUTHORIZATION, "Bearer ${current.accessToken}").build()
        }
        val renewed = refresh(current.refreshToken)
        if (renewed == null) {
            runBlocking { tokenStore.clearTokens() }
            return null
        }
        runBlocking { tokenStore.saveTokens(renewed) }
        return response.request.newBuilder().header(AUTHORIZATION, "Bearer ${renewed.accessToken}").build()
    }

    private fun refresh(refreshToken: String): AuthTokens? {
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(BuildConfig.AUTH_BASE_URL + REFRESH_PATH.removePrefix("/")).post(body).build()
        return try {
            client.newCall(request).execute().use { result ->
                if (result.code >= SERVER_ERROR) throw IOException("Auth indisponível: ${result.code}")
                if (!result.isSuccessful) return null
                val parsed = json.decodeFromString(TokenResponse.serializer(), result.body.string())
                AuthTokens(parsed.accessToken, parsed.refreshToken, parsed.accessTokenExpiresAt)
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val REFRESH_PATH = "/auth/refresh"
        const val MAX_ATTEMPTS = 2
        const val SERVER_ERROR = 500
    }
}
