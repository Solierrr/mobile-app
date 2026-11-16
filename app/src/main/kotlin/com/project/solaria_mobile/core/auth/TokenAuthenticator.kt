package com.project.solaria_mobile.core.auth

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.network.ApiJson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.time.Duration

@Singleton
class TokenAuthenticator internal constructor(
    private val tokenStore: TokenStore,
    private val client: OkHttpClient,
    private val refreshUrl: String,
) : Authenticator {
    @Inject
    constructor(tokenStore: TokenStore) : this(
        tokenStore,
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(60))
            .build(),
        BuildConfig.AUTH_BASE_URL + REFRESH_PATH.removePrefix("/"),
    )

    private val json: Json = ApiJson.instance

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.endsWith(REFRESH_PATH) || responseCount(response) >= MAX_ATTEMPTS) return null
        val usedToken = response.request.header(AUTHORIZATION)?.removePrefix("Bearer ")
        val current = tokenStore.currentTokens() ?: return null
        if (usedToken != null && usedToken != current.accessToken) {
            return response.request.newBuilder().header(AUTHORIZATION, "Bearer ${current.accessToken}").build()
        }
        return when (val result = refresh(current.refreshToken)) {
            is RefreshResult.Renewed -> {
                val saved = runBlocking { tokenStore.replaceTokensIfCurrent(current, result.tokens) }
                if (!saved) return null
                response.request.newBuilder().header(AUTHORIZATION, "Bearer ${result.tokens.accessToken}").build()
            }
            RefreshResult.Rejected -> {
                runBlocking { tokenStore.replaceTokensIfCurrent(current, null) }
                null
            }
            RefreshResult.Unavailable -> null
        }
    }

    private fun refresh(refreshToken: String): RefreshResult {
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(refreshUrl).post(body).build()
        return runCatching {
            client.newCall(request).execute().use { result ->
                val content = result.body.string()
                if (!result.isSuccessful) {
                    val code = runCatching {
                        json.parseToJsonElement(content).jsonObject["code"]?.jsonPrimitive?.content
                    }.getOrNull()
                    return if (result.code in 400..499 && code in REJECTED_REFRESH_CODES) RefreshResult.Rejected else RefreshResult.Unavailable
                }
                val parsed = json.decodeFromString(TokenResponse.serializer(), content)
                if (parsed.accessToken.isBlank() || parsed.refreshToken.isBlank()) return RefreshResult.Unavailable
                RefreshResult.Renewed(AuthTokens(parsed.accessToken, parsed.refreshToken, parsed.accessTokenExpiresAt))
            }
        }.getOrDefault(RefreshResult.Unavailable)
    }

    private sealed interface RefreshResult {
        data class Renewed(val tokens: AuthTokens) : RefreshResult
        data object Rejected : RefreshResult
        data object Unavailable : RefreshResult
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
        val REJECTED_REFRESH_CODES = setOf("INVALID_REFRESH_TOKEN", "REFRESH_TOKEN_INVALID", "REFRESH_TOKEN_REVOKED", "REFRESH_TOKEN_EXPIRED", "REFRESH_TOKEN_REUSED")
    }
}
