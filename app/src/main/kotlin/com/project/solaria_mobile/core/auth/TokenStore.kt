package com.project.solaria_mobile.core.auth

import kotlinx.coroutines.flow.StateFlow

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String? = null,
)

data class PendingLogin(val state: String, val codeVerifier: String)

interface TokenStore {
    val tokens: StateFlow<AuthTokens?>

    fun currentTokens(): AuthTokens?

    suspend fun saveTokens(tokens: AuthTokens)

    suspend fun replaceTokensIfCurrent(expected: AuthTokens, replacement: AuthTokens?): Boolean {
        if (currentTokens() != expected) return false
        if (replacement == null) clearTokens() else saveTokens(replacement)
        return true
    }

    suspend fun clearTokens()

    suspend fun savePendingLogin(pending: PendingLogin)

    suspend fun takePendingLogin(): PendingLogin?
}
