package com.project.solaria_mobile.core.auth

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class StartTransactionRequest(
    val clientId: String,
    val redirectUri: String,
    val state: String,
    val codeChallenge: String,
    val codeChallengeMethod: String = "S256",
)

@Serializable
data class StartTransactionResponse(
    val transactionId: String,
    val authorizationUrl: String,
    val expiresAt: String? = null,
)

@Serializable
data class RedeemRequest(
    val clientId: String,
    val redirectUri: String,
    val code: String,
    val codeVerifier: String,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String? = null,
)

interface AuthApi {
    @POST("auth/mobile/transactions")
    suspend fun startTransaction(@Body body: StartTransactionRequest): StartTransactionResponse

    @POST("auth/mobile/token")
    suspend fun redeem(@Body body: RedeemRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @POST("auth/sessions/current/logout")
    suspend fun logout(): Response<Unit>
}
