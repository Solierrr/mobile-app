package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.profile.data.ProfileApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Descobre o nome de quem participa das conversas. O messenger só conhece o `authId`; o nome vem do summary do api-core,
 * o mesmo que o web-app usa. Cada nome encontrado fica em memória até o processo acabar. Quem falhar fica de fora do
 * resultado e é tentado de novo na próxima chamada, sem derrubar os demais.
 */
@Singleton
class ParticipantNames internal constructor(private val lookup: suspend (String) -> String?) {
    @Inject constructor(api: ProfileApi, executor: ApiCallExecutor) : this({ authId ->
        (executor.execute { api.summary(authId) } as? ApiResult.Success)?.data?.username?.takeIf(String::isNotBlank)
    })

    private val cache = ConcurrentHashMap<String, String>()

    /** Nome por `authId` das pessoas que foram resolvidas. */
    suspend fun resolve(authIds: Collection<String>): Map<String, String> = coroutineScope {
        val gate = Semaphore(CONCURRENCY)
        authIds.distinct().map { id ->
            async {
                val name = cache[id] ?: gate.withPermit { lookup(id) }?.also { cache[id] = it }
                name?.let { id to it }
            }
        }.awaitAll().filterNotNull().toMap()
    }

    private companion object {
        const val CONCURRENCY = 4
    }
}
