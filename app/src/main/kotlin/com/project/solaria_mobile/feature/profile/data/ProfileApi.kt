package com.project.solaria_mobile.feature.profile.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class UserDto(
    val id: String,
    val authId: String? = null,
    val username: String? = null,
    val avatar: String? = null,
)

@Serializable
data class ConnectionRequest(val connectedUserId: String)

interface ProfileApi {
    @GET("api/users/me")
    suspend fun me(): UserDto

    @GET("api/connections")
    suspend fun connections(): List<UserDto>

    @GET("api/connections/{id}")
    suspend fun connection(@Path("id") id: String): UserDto

    @POST("api/connections")
    suspend fun addConnection(@Body body: ConnectionRequest): UserDto

    @DELETE("api/connections/{id}")
    suspend fun removeConnection(@Path("id") id: String): Response<Unit>
}
