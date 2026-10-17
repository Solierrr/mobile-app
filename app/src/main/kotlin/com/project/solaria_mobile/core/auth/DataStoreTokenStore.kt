package com.project.solaria_mobile.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Singleton
class DataStoreTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
) : TokenStore {
    private val mutableTokens = MutableStateFlow<AuthTokens?>(null)
    private var loaded = false

    override val tokens: StateFlow<AuthTokens?>
        get() {
            ensureLoaded()
            return mutableTokens
        }

    override fun currentTokens(): AuthTokens? {
        ensureLoaded()
        return mutableTokens.value
    }

    override suspend fun saveTokens(tokens: AuthTokens) {
        dataStore.edit {
            it[ACCESS] = cipher.encrypt(tokens.accessToken)
            it[REFRESH] = cipher.encrypt(tokens.refreshToken)
            tokens.accessTokenExpiresAt?.let { expiresAt -> it[EXPIRES_AT] = expiresAt } ?: it.remove(EXPIRES_AT)
        }
        loaded = true
        mutableTokens.value = tokens
    }

    override suspend fun clearTokens() {
        dataStore.edit {
            it.remove(ACCESS)
            it.remove(REFRESH)
            it.remove(EXPIRES_AT)
        }
        loaded = true
        mutableTokens.value = null
    }

    override suspend fun savePendingLogin(pending: PendingLogin) {
        dataStore.edit {
            it[PENDING_STATE] = pending.state
            it[PENDING_VERIFIER] = cipher.encrypt(pending.codeVerifier)
        }
    }

    override suspend fun takePendingLogin(): PendingLogin? {
        val preferences = dataStore.data.first()
        val state = preferences[PENDING_STATE]
        val verifier = preferences[PENDING_VERIFIER]?.let(cipher::decrypt)
        dataStore.edit {
            it.remove(PENDING_STATE)
            it.remove(PENDING_VERIFIER)
        }
        return if (state != null && verifier != null) PendingLogin(state, verifier) else null
    }

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        val preferences = runBlocking { dataStore.data.first() }
        val access = preferences[ACCESS]?.let(cipher::decrypt)
        val refresh = preferences[REFRESH]?.let(cipher::decrypt)
        mutableTokens.value = if (access != null && refresh != null) AuthTokens(access, refresh, preferences[EXPIRES_AT]) else null
        loaded = true
    }

    private companion object {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = stringPreferencesKey("access_token_expires_at")
        val PENDING_STATE = stringPreferencesKey("pending_login_state")
        val PENDING_VERIFIER = stringPreferencesKey("pending_login_verifier")
    }
}
