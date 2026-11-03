package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.profile.data.ProfileApi
import com.project.solaria_mobile.feature.profile.data.UserSummaryDto
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
class ParticipantNames internal constructor(
    private val summaryLookup: (suspend (String) -> UserSummaryDto?)? = null,
    private val lookup: suspend (String) -> String?,
) {
    @Inject constructor(api: ProfileApi, executor: ApiCallExecutor) : this({ authId ->
        (executor.execute { api.summary(authId) } as? ApiResult.Success)?.data
    }, { null })

    private val cache = ConcurrentHashMap<String, UserSummaryDto>()
    private val gate = Semaphore(CONCURRENCY)

    /** Nome por `authId` das pessoas que foram resolvidas. */
    suspend fun resolve(authIds: Collection<String>): Map<String, String> = summaries(authIds).mapNotNull { (id, summary) ->
        summary.username?.takeIf(String::isNotBlank)?.let { id to it }
    }.toMap()

    /** Nome e foto do mesmo summary; quem não tem foto continua com a silhueta. */
    suspend fun summaries(authIds: Collection<String>): Map<String, UserSummaryDto> = coroutineScope {
        authIds.map(String::lowercase).distinct().map { id ->
            async {
                val summary = cache[id] ?: gate.withPermit { summaryLookup?.invoke(id) ?: lookup(id)?.let { UserSummaryDto(username = it) } }
                    ?.also { cache[id] = it }
                summary?.let { id to it }
            }
        }.awaitAll().filterNotNull().toMap()
    }

    private companion object {
        const val CONCURRENCY = 4
    }
}
