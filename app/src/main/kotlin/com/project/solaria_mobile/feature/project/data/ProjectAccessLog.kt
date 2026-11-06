package com.project.solaria_mobile.feature.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.offline.CacheIdentity
import com.project.solaria_mobile.core.offline.LocalDataCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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

/** Último acesso a cada projeto pela conta atual, para a Home mostrar o que a pessoa realmente abriu. */
interface ProjectAccessLog {
    fun observe(): Flow<Map<String, Long>>
    suspend fun record(projectId: String)
}

/**
 * Guarda os acessos no aparelho, separados por conta, ambiente e modo mock, como as preferências de visualização.
 * Mantém só os mais recentes e é apagado no logout junto com o cache.
 */
@Singleton
class DataStoreProjectAccessLog @Inject constructor(
    private val preferences: DataStore<Preferences>,
    private val identity: CacheIdentity,
    private val tokens: TokenStore,
    private val mockMode: MockModeStore,
) : ProjectAccessLog, LocalDataCleaner {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<Map<String, Long>> = combine(tokens.tokens, mockMode.enabled) { _, _ -> scope() }
        .distinctUntilChanged().flatMapLatest { account ->
            if (account == null) flowOf(emptyMap()) else preferences.data.map { decode(it[key(account)]) }
        }.catch { error -> if (error is java.io.IOException) emit(emptyMap()) else throw error }

    override suspend fun record(projectId: String) {
        val account = scope() ?: return
        preferences.edit { values ->
            if (account != scope()) return@edit
            val updated = decode(values[key(account)]) + (projectId to System.currentTimeMillis())
            values[key(account)] = encode(updated.entries.sortedByDescending { it.value }.take(MAX_ENTRIES).associate { it.key to it.value })
        }
    }

    override suspend fun clear() {
        preferences.edit { values -> values.asMap().keys.filter { it.name.startsWith(KEY_PREFIX) }.forEach { values.remove(it) } }
    }

    private fun scope(): String? = identity.current()?.let { "${mockMode.isEnabled()}:$it" }
    private fun key(account: String) = stringPreferencesKey("$KEY_PREFIX$account")

    private companion object {
        const val KEY_PREFIX = "project_access_"
        const val MAX_ENTRIES = 20
    }
}

internal fun encode(access: Map<String, Long>): String = access.entries.joinToString(",") { "${it.key}=${it.value}" }

internal fun decode(value: String?): Map<String, Long> = value.orEmpty().split(',').mapNotNull { entry ->
    val parts = entry.split('=')
    val time = parts.getOrNull(1)?.toLongOrNull()
    if (parts.size == 2 && parts[0].isNotBlank() && time != null) parts[0] to time else null
}.toMap()

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectAccessLogModule {
    @Binds abstract fun bindAccessLog(log: DataStoreProjectAccessLog): ProjectAccessLog

    @Binds @IntoSet abstract fun bindAccessLogCleaner(log: DataStoreProjectAccessLog): LocalDataCleaner
}
