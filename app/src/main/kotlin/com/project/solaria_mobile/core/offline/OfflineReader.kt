package com.project.solaria_mobile.core.offline

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.NetworkMonitor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer

@Singleton
class OfflineReader(
    private val store: CacheStore,
    private val network: NetworkMonitor,
    private val executor: ApiCallExecutor,
    private val identity: CacheScope,
    private val clock: () -> Long,
) {
    @Inject
    constructor(store: CacheStore, network: NetworkMonitor, executor: ApiCallExecutor, identity: CacheIdentity) :
        this(store, network, executor, identity, System::currentTimeMillis)

    private val writeLock = Mutex()
    @Volatile private var generation = 0L
    private val requests = java.util.concurrent.ConcurrentHashMap<String, Mutex>()
    private val revisions = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong>()

    fun <T> observe(cacheKey: String, serializer: KSerializer<T>, call: suspend () -> T): Flow<ReadResult<T>> = channelFlow {
        network.isOnline.distinctUntilChanged().collectLatest { online ->
            updates(cacheKey, serializer, online, call).collect { send(it) }
        }
    }

    suspend fun <T> read(cacheKey: String, serializer: KSerializer<T>, call: suspend () -> T): ApiResult<T> =
        readResult(cacheKey, serializer, call).result

    suspend fun <T> readResult(cacheKey: String, serializer: KSerializer<T>, call: suspend () -> T): ReadResult<T> =
        updates(cacheKey, serializer, network.isOnline.first(), call).last()

        suspend fun <T> cached(cacheKey: String, serializer: KSerializer<T>): ReadResult<T>? {
        val scope = identity.current() ?: return null
        val version = generation
        val entry = store.get("$scope:$cacheKey") ?: return null
        val data = runCatching { ApiJson.instance.decodeFromString(serializer, entry.json) }.getOrNull() ?: return null
        if (scope != identity.current() || version != generation) return null
        return ReadResult(ApiResult.Success(data), entry.syncedAt, cached = true)
    }

    private fun <T> updates(cacheKey: String, serializer: KSerializer<T>, online: Boolean, call: suspend () -> T): Flow<ReadResult<T>> = flow {
        val scope = identity.current()
        if (scope == null) {
            emit(ReadResult(ApiResult.Failure(ErrorKind.UNAUTHORIZED)))
            return@flow
        }
        val version = generation
        val key = "$scope:$cacheKey"
        val revision = revisions.getOrPut(key) { java.util.concurrent.atomic.AtomicLong() }
        val requestRevision = revision.get()
        val entry = store.get(key)
        val cached = entry?.let { runCatching { ApiJson.instance.decodeFromString(serializer, it.json) }.getOrNull() }
        if (cached != null && identity.current() == scope && version == generation) {
            emit(ReadResult(ApiResult.Success(cached), entry.syncedAt, cached = true, refreshing = online))
        }
        if (!online) {
            if (cached == null) emit(ReadResult(ApiResult.Failure(ErrorKind.CONNECTIVITY)))
            return@flow
        }
        var syncedAt = entry?.syncedAt
        val result = requests.getOrPut(key) { Mutex() }.withLock {
            val newer = store.get(key)
            val saved = if (newer != null && newer != entry) runCatching {
                ApiJson.instance.decodeFromString(serializer, newer.json)
            }.getOrNull() else null
            if (saved != null) {
                syncedAt = newer?.syncedAt
                ApiResult.Success(saved)
            } else {
                val response = withTimeoutOrNull(NETWORK_LIMIT_MILLIS) { executor.execute(call) }
                    ?: ApiResult.Failure(ErrorKind.TIMEOUT)
                if (response is ApiResult.Success) {
                    writeLock.withLock {
                        if (identity.current() == scope && version == generation && requestRevision == revision.get()) {
                            syncedAt = clock()
                            store.put(key, ApiJson.instance.encodeToString(serializer, response.data), syncedAt!!)
                        }
                    }
                }
                response
            }
        }
        if (identity.current() != scope || version != generation) return@flow
        if (requestRevision != revision.get()) {
            emit(ReadResult(ApiResult.Failure(ErrorKind.CONFLICT)))
            return@flow
        }
        when (result) {
            is ApiResult.Success -> emit(ReadResult(result, syncedAt))
            is ApiResult.Failure -> {
                if (result.kind in setOf(ErrorKind.FORBIDDEN, ErrorKind.NOT_FOUND, ErrorKind.UNAUTHORIZED)) {
                    writeLock.withLock { if (version == generation) store.remove(key) }
                }
                val temporary = result.kind in setOf(ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT, ErrorKind.SERVER, ErrorKind.RATE_LIMITED)
                if (cached != null && temporary) {
                    emit(ReadResult(ApiResult.Success(cached), entry.syncedAt, cached = true, refreshError = result.kind))
                } else emit(ReadResult(result))
            }
        }
    }

    suspend fun invalidatePrefix(prefix: String) {
        val scope = identity.current() ?: return
        writeLock.withLock {
            revisions.filterKeys { it.startsWith("$scope:$prefix") }.values.forEach { it.incrementAndGet() }
            store.removePrefix("$scope:$prefix")
        }
    }

    suspend fun invalidate(cacheKey: String) {
        val scope = identity.current() ?: return
        writeLock.withLock {
            revisions["$scope:$cacheKey"]?.incrementAndGet()
            store.remove("$scope:$cacheKey")
        }
    }

    suspend fun clear() {
        writeLock.withLock {
            generation++
            store.clear()
        }
    }

    private companion object {
        const val NETWORK_LIMIT_MILLIS = 60_000L
    }
}
