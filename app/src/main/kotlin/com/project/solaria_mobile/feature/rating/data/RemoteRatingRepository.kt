package com.project.solaria_mobile.feature.rating.data

import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.feature.communication.data.ParticipantNames
import com.project.solaria_mobile.feature.rating.domain.MAX_RATING_COMMENT
import com.project.solaria_mobile.feature.rating.domain.Rating
import com.project.solaria_mobile.feature.rating.domain.RatingRepository
import com.project.solaria_mobile.feature.rating.domain.RatingType
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteRatingRepository @Inject constructor(
    private val api: RatingApi,
    private val executor: ApiCallExecutor,
    private val credentials: CredentialProvider,
    private val names: ParticipantNames,
) : RatingRepository {
    override fun currentUserId(): String? = credentials.accessToken()?.let(::accessTokenSubject)

    override suspend fun received(evaluatedId: String): ApiResult<List<Rating>> =
        executor.execute { api.evaluated(evaluatedId) }.map { list -> list.filter { it.status == "ACTIVE" }.map { it.toRating() } }

    override suspend fun given(): ApiResult<List<Rating>> {
        val me = currentUserId() ?: return ApiResult.Failure(ErrorKind.UNAUTHORIZED)
        return when (val result = executor.execute { api.evaluator(me) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val active = result.data.filter { it.status == "ACTIVE" }
                val resolved = names.resolve(active.map { it.evaluatedId })
                ApiResult.Success(active.map { it.toRating(resolved[it.evaluatedId]) })
            }
        }
    }

    override suspend fun rate(evaluatedId: String, type: RatingType, stars: Double, comment: String): ApiResult<Rating> {
        if (!valid(stars, comment) || evaluatedId.equals(currentUserId(), ignoreCase = true)) return ApiResult.Failure(ErrorKind.VALIDATION)
        return executor.execute { api.create(RatingRequest(evaluatedId, type.name, stars, comment.trim().ifEmpty { null })) }.map { it.toRating() }
    }

    override suspend fun edit(id: String, stars: Double, comment: String): ApiResult<Rating> {
        if (!valid(stars, comment)) return ApiResult.Failure(ErrorKind.VALIDATION)
        return executor.execute { api.update(id, RatingUpdateRequest(stars, comment.trim())) }.map { it.toRating() }
    }

    override suspend fun remove(id: String): ApiResult<Rating> =
        executor.execute { api.status(id, RatingStatusRequest("EXCLUDED")) }.map { it.toRating() }

    private fun valid(stars: Double, comment: String) = stars in 0.0..5.0 && comment.trim().length <= MAX_RATING_COMMENT
}

internal fun RatingDto.toRating(evaluatedName: String? = null) = Rating(
    id, evaluatorId, evaluatedId, runCatching { RatingType.valueOf(typeRate) }.getOrDefault(RatingType.PROFISSIONAL),
    rating, comment.orEmpty(), timestamp, evaluatedName,
)

@Module
@InstallIn(SingletonComponent::class)
abstract class RatingModule {
    @Binds abstract fun repository(implementation: RemoteRatingRepository): RatingRepository
}
