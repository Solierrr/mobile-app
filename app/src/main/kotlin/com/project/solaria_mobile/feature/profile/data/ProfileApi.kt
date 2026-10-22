package com.project.solaria_mobile.feature.profile.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class UserDto(
    val id: String,
    val name: String? = null,
    val connected: Boolean = false,
    val banner: String? = null,
    val username: String? = null,
    val avatar: String? = null,
)

@Serializable
data class SelfPersonDto(val name: String? = null, val phone: String? = null)

@Serializable
data class SelfProfessionDto(val id: String? = null, val name: String? = null)

@Serializable
data class SelfTechnicianDto(val id: String? = null, val professions: List<SelfProfessionDto> = emptyList())

@Serializable
data class SelfMembershipDto(val companyId: String? = null, val companyName: String? = null, val position: String? = null)

@Serializable
data class SelfProfileDto(
    val id: String,
    val authId: String? = null,
    val avatar: String? = null,
    val banner: String? = null,
    val person: SelfPersonDto? = null,
    val technician: SelfTechnicianDto? = null,
    val companyMemberships: List<SelfMembershipDto> = emptyList(),
)

@Serializable
data class PeoplePageDto(val items: List<UserDto> = emptyList(), val hasMore: Boolean = false)

@Serializable
data class ConnectionRequest(val connectedUserId: String)

interface ProfileApi {
    @GET("api/connections/people")
    suspend fun people(@Query("query") query: String, @Query("page") page: Int,
        @Query("connectedOnly") connectedOnly: Boolean): PeoplePageDto

    @GET("api/users/me")
    suspend fun me(): SelfProfileDto

    @GET("api/connections")
    suspend fun connections(): List<UserDto>

    @GET("api/connections/people/{id}")
    suspend fun connection(@Path("id") id: String): UserDto

    @POST("api/connections")
    suspend fun addConnection(@Body body: ConnectionRequest): UserDto

    @DELETE("api/connections/{id}")
    suspend fun removeConnection(@Path("id") id: String): Response<Unit>
}
