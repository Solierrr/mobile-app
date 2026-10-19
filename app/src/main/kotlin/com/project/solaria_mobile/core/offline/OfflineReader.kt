package com.project.solaria_mobile.core.offline

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import javax.inject.Inject
import javax.inject.Singleton

/** [lastSyncedAt] é o instante da última leitura vinda do servidor; [stale] indica que a tela mostra dados guardados. */
data class OfflineStatus(val lastSyncedAt: Long? = null, val stale: Boolean = false)

/**
 * Leitura com cache: tenta o servidor e guarda a resposta; sem internet, ou se o servidor demorar ou cair,
 * devolve a última resposta guardada. Erros do próprio servidor (401, 403, 404...) nunca são escondidos.
 */
@Singleton
class OfflineReader(
    private val store: CacheStore,
    private val network: NetworkMonitor,
    private val executor: ApiCallExecutor,
    private val clock: () -> Long,
    private val networkLimitMillis: Long,
) {
    @Inject
    constructor(store: CacheStore, network: NetworkMonitor, executor: ApiCallExecutor) :
        this(store, network, executor, System::currentTimeMillis, NETWORK_LIMIT_MILLIS)

    private val mutableStatus = MutableStateFlow(OfflineStatus())
    val status: StateFlow<OfflineStatus> = mutableStatus

    suspend fun <T> read(cacheKey: String, serializer: KSerializer<T>, call: suspend () -> T): ApiResult<T> {
        if (!network.isOnline.first()) return fromCache(cacheKey, serializer) ?: ApiResult.Failure(ErrorKind.CONNECTIVITY)
        val fresh = withTimeoutOrNull(networkLimitMillis) { executor.execute(call) }
            ?: ApiResult.Failure(ErrorKind.TIMEOUT)
        return when {
            fresh is ApiResult.Success -> {
                val now = clock()
                store.put(cacheKey, ApiJson.instance.encodeToString(serializer, fresh.data), now)
                mutableStatus.value = OfflineStatus(lastSyncedAt = now, stale = false)
                fresh
            }
            fresh is ApiResult.Failure && fresh.kind.isNetworkFailure() -> fromCache(cacheKey, serializer) ?: fresh
            else -> fresh
        }
    }

    /** Apaga tudo o que foi guardado; chamado ao entrar de novo e ao sair da conta. */
    suspend fun clear() {
        store.clear()
        mutableStatus.value = OfflineStatus()
    }

    private suspend fun <T> fromCache(cacheKey: String, serializer: KSerializer<T>): ApiResult<T>? {
        val entry = store.get(cacheKey) ?: return null
        val data = runCatching { ApiJson.instance.decodeFromString(serializer, entry.json) }.getOrNull() ?: return null
        mutableStatus.value = OfflineStatus(lastSyncedAt = entry.syncedAt, stale = true)
        return ApiResult.Success(data)
    }

    private fun ErrorKind.isNetworkFailure() = this == ErrorKind.CONNECTIVITY || this == ErrorKind.TIMEOUT

    private companion object {
        const val NETWORK_LIMIT_MILLIS = 10_000L
    }
}
