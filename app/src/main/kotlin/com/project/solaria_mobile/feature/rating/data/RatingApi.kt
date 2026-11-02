package com.project.solaria_mobile.feature.rating.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** `typeRate` é `PROFISSIONAL` ou `PRODUTO`; `rating` vai de 0.0 a 5.0; o comentário tem até 1000 caracteres. */
@Serializable
data class RatingRequest(val evaluatedId: String, val typeRate: String, val rating: Double, val comment: String? = null)

@Serializable
data class RatingUpdateRequest(val rating: Double? = null, val comment: String? = null)

/** `status` é `ACTIVE`, `EXCLUDED` ou `DOWN`; o app só usa `EXCLUDED`, que equivale a apagar. */
@Serializable
data class RatingStatusRequest(val status: String)

@Serializable
data class RatingDto(
    val id: String,
    val evaluatorId: String,
    val evaluatedId: String,
    val typeRate: String = "PROFISSIONAL",
    val comment: String? = null,
    val rating: Double = 0.0,
    val status: String = "ACTIVE",
    val timestamp: String? = null,
)

interface RatingApi {
    @POST("messaging/ratings")
    suspend fun create(@Body body: RatingRequest): RatingDto

    @GET("messaging/ratings/evaluated/{id}")
    suspend fun evaluated(@Path("id") id: String): List<RatingDto>

    @GET("messaging/ratings/evaluator/{id}")
    suspend fun evaluator(@Path("id") id: String): List<RatingDto>

    @PATCH("messaging/ratings/{id}")
    suspend fun update(@Path("id") id: String, @Body body: RatingUpdateRequest): RatingDto

    @PATCH("messaging/ratings/{id}/status")
    suspend fun status(@Path("id") id: String, @Body body: RatingStatusRequest): RatingDto
}
