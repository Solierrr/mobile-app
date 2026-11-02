package com.project.solaria_mobile.feature.profile.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.Part

@Serializable
data class UserDto(
    val id: String,
    val name: String? = null,
    val connected: Boolean = false,
    val banner: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val authId: String? = null,
)

@Serializable
data class UserSummaryDto(val username: String? = null, val avatar: String? = null)

@Serializable
data class SelfPersonDto(val name: String? = null, val phone: String? = null)

@Serializable
data class SelfProfessionDto(val id: String? = null, val name: String? = null)

@Serializable
data class SelfTechnicianDto(val id: String? = null, val professions: List<SelfProfessionDto> = emptyList())

@Serializable
data class SelfMembershipDto(val companyId: String? = null, val companyName: String? = null, val position: String? = null)

@Serializable
data class SelfAffiliationDto(val type: String? = null, val companyId: String? = null, val companyName: String? = null)

@Serializable
data class SelfProfileDto(
    val id: String,
    val authId: String? = null,
    val avatar: String? = null,
    val banner: String? = null,
    val person: SelfPersonDto? = null,
    val technician: SelfTechnicianDto? = null,
    val activeAffiliations: List<SelfAffiliationDto> = emptyList(),
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

    @Multipart
    @PUT("api/user-photos/user/{userId}/type/{type}")
    suspend fun uploadPhoto(@Path("userId") userId: String, @Path("type") type: String, @Part file: MultipartBody.Part): kotlinx.serialization.json.JsonElement

    @DELETE("api/user-photos/user/{userId}/type/{type}")
    suspend fun removePhoto(@Path("userId") userId: String, @Path("type") type: String)

    @PATCH("api/users/me")
    suspend fun changeUsername(@Body body: UsernameRequest): kotlinx.serialization.json.JsonElement

    @GET("api/catalog/companies")
    suspend fun companyCatalog(): List<CatalogCompanyDto>

    @GET("api/company-photos/company/{companyId}")
    suspend fun companyPhotos(@Path("companyId") companyId: String): List<CompanyPhotoDto>

    @GET("api/users/me")
    suspend fun me(): SelfProfileDto

    /** Nome de exibição e foto de uma conta pelo `authId`, o identificador que o messenger usa para as pessoas. */
    @GET("api/users/auth/{authId}/summary")
    suspend fun summary(@Path("authId") authId: String): UserSummaryDto

    @GET("api/connections")
    suspend fun connections(): List<UserDto>

    @GET("api/connections/people/{id}")
    suspend fun connection(@Path("id") id: String): UserDto

    @POST("api/connections")
    suspend fun addConnection(@Body body: ConnectionRequest): UserDto

    @DELETE("api/connections/{id}")
    suspend fun removeConnection(@Path("id") id: String): Response<Unit>
}

@Serializable
data class UsernameRequest(val username: String)

/** Foto da empresa no api-core: `PROFILE` é o logo e `BANNER` a capa. */
@Serializable
data class CompanyPhotoDto(val id: String? = null, val companyId: String? = null, val type: String? = null, val url: String? = null)

@Serializable
data class CatalogCompanyDto(
    val id: String,
    val tradeName: String? = null,
    val slug: String? = null,
    val city: String? = null,
    val state: String? = null,
)
