package com.project.solaria_mobile.feature.discovery.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.offline.CacheIdentity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.catch

@Singleton
class SavedMapLocations @Inject constructor(
    private val preferences: DataStore<Preferences>,
    private val identity: CacheIdentity,
    private val tokens: TokenStore,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<Set<String>> = tokens.tokens.map { identity.current() }.distinctUntilChanged().flatMapLatest { account ->
        if (account == null) flowOf(emptySet()) else preferences.data.map { it[key(account)].orEmpty() }
    }.catch { error -> if (error is java.io.IOException) emit(emptySet()) else throw error }

    suspend fun toggle(projectId: String): Boolean {
        val account = checkNotNull(identity.current())
        var saved = false
        preferences.edit { values ->
            check(account == identity.current())
            val current = values[key(account)].orEmpty()
            saved = projectId !in current
            values[key(account)] = if (saved) current + projectId else current - projectId
        }
        return saved
    }

    private fun key(account: String) = stringSetPreferencesKey("map_saved_$account")
}
