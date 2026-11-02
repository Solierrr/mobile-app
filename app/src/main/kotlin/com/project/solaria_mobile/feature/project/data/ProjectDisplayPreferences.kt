package com.project.solaria_mobile.feature.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.offline.CacheIdentity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class ProjectDisplayPreferences @Inject constructor(
    private val preferences: DataStore<Preferences>,
    private val identity: CacheIdentity,
    private val tokens: TokenStore,
    private val mockMode: MockModeStore,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeGrid(): Flow<Boolean> = combine(tokens.tokens, mockMode.enabled) { _, _ -> scope() }
        .distinctUntilChanged().flatMapLatest { account ->
            if (account == null) flowOf(false) else preferences.data.map { it[key(account)] == true }
        }.catch { error -> if (error is java.io.IOException) emit(false) else throw error }

    suspend fun setGrid(grid: Boolean) {
        val account = scope() ?: return
        preferences.edit { values -> if (account == scope()) values[key(account)] = grid }
    }

    private fun scope(): String? = identity.current()?.let { "${mockMode.isEnabled()}:$it" }
    private fun key(account: String) = booleanPreferencesKey("project_grid_$account")
}
