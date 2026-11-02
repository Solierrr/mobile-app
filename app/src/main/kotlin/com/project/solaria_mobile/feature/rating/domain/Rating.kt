package com.project.solaria_mobile.feature.rating.domain

import com.project.solaria_mobile.core.common.ApiResult

enum class RatingType { PROFISSIONAL, PRODUTO }

/**
 * Avaliação de uma pessoa ou produto. [stars] vai de 0.0 a 5.0. [evaluatedName] só vem em "minhas avaliações", resolvido pelo
 * api-core; nulo quando não foi possível.
 */
data class Rating(
    val id: String,
    val evaluatorId: String,
    val evaluatedId: String,
    val type: RatingType,
    val stars: Double,
    val comment: String,
    val timestamp: String? = null,
    val evaluatedName: String? = null,
)

const val MAX_RATING_COMMENT = 1000

/** Média das avaliações, ou nulo quando não há nenhuma. */
fun List<Rating>.average(): Double? = takeIf { it.isNotEmpty() }?.let { list -> list.sumOf { it.stars } / list.size }

interface RatingRepository {
    /** Identificador de login da conta; as avaliações próprias são as que têm esse `evaluatorId`. */
    fun currentUserId(): String?

    /** Avaliações ativas que [evaluatedId] recebeu. */
    suspend fun received(evaluatedId: String): ApiResult<List<Rating>>

    /** Avaliações ativas que a conta fez, com o nome de quem foi avaliado. */
    suspend fun given(): ApiResult<List<Rating>>

    suspend fun rate(evaluatedId: String, type: RatingType, stars: Double, comment: String): ApiResult<Rating>

    suspend fun edit(id: String, stars: Double, comment: String): ApiResult<Rating>

    /** Exclui a avaliação (status `EXCLUDED`); ela deixa de aparecer nas listas. */
    suspend fun remove(id: String): ApiResult<Rating>
}
