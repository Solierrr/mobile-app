package com.project.solaria_mobile.feature.project.data

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.PUT

@Serializable
data class ProjectLocationDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geocodedAt: String? = null,
    val city: String? = null,
    val state: String? = null,
    val neighborhood: String? = null,
    val zipCode: String? = null,
    val street: String? = null,
    val number: String? = null,
    val complement: String? = null,
)

@Serializable
data class ProjectDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerName: String? = null,
    val ownerCompanyId: String? = null,
    val ownerCompanyLogoUrl: String? = null,
    val createdAt: String? = null,
    val status: String? = null,
    val location: ProjectLocationDto? = null,
    val role: String? = null,
    val archived: Boolean = false,
)

@Serializable
data class ProjectRequestDto(
    val name: String,
    val description: String? = null,
    val location: ProjectLocationDto? = null,
)

@Serializable
data class ProjectMemberDto(
    val id: String,
    val userId: String,
    val name: String? = null,
    val role: String? = null,
    val functionName: String? = null,
    val authId: String? = null,
)

@Serializable
data class ProjectActivityDto(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val status: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val onsite: Boolean = false,
    val assigneeIds: List<String> = emptyList(),
)

@Serializable
data class ProjectHistoryDto(
    val id: String,
    val action: String? = null,
    val detail: String? = null,
    val actor: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class ProjectPhotoDto(
    val id: String,
    val cover: Boolean = false,
)

interface ProjectApi {
    @POST("api/projects/{id}/map-location")
    suspend fun resolveLocation(@Path("id") id: String): ProjectDto

    @GET("api/projects")
    suspend fun projects(): List<ProjectDto>

    @GET("api/projects/{id}")
    suspend fun project(@Path("id") id: String): ProjectDto

    @POST("api/projects")
    suspend fun create(@Body body: ProjectRequestDto): ProjectDto

    @PUT("api/projects/{id}")
    suspend fun update(@Path("id") id: String, @Body body: ProjectRequestDto): ProjectDto

    @GET("api/projects/{id}/members")
    suspend fun members(@Path("id") id: String): List<ProjectMemberDto>

    @GET("api/projects/{id}/activities")
    suspend fun activities(@Path("id") id: String): List<ProjectActivityDto>

    @GET("api/projects/{id}/history")
    suspend fun history(@Path("id") id: String): List<ProjectHistoryDto>

    @GET("api/projects/{id}/photos")
    suspend fun photos(@Path("id") id: String): List<ProjectPhotoDto>

    @Streaming
    @GET("api/projects/{id}/photos/{photoId}/content")
    suspend fun photoContent(@Path("id") id: String, @Path("photoId") photoId: String): ResponseBody

    @PATCH("api/projects/{id}/status")
    suspend fun changeStatus(@Path("id") id: String, @Body body: ProjectStatusRequest): ProjectDto

    @DELETE("api/projects/{id}")
    suspend fun archive(@Path("id") id: String)

    @Multipart
    @POST("api/projects/{id}/photos")
    suspend fun uploadPhoto(@Path("id") id: String, @Part file: MultipartBody.Part, @Query("cover") cover: Boolean = false): ProjectPhotoDto

    @DELETE("api/projects/{id}/photos/{photoId}")
    suspend fun removePhoto(@Path("id") id: String, @Path("photoId") photoId: String)

    @POST("api/projects/{id}/activities")
    suspend fun createActivity(@Path("id") id: String, @Body body: ProjectActivityRequestDto): ProjectActivityDto

    @PUT("api/projects/{id}/activities/{activityId}")
    suspend fun updateActivity(@Path("id") id: String, @Path("activityId") activityId: String, @Body body: ProjectActivityRequestDto): ProjectActivityDto

    @PATCH("api/projects/{id}/activities/{activityId}/status")
    suspend fun changeActivityStatus(@Path("id") id: String, @Path("activityId") activityId: String, @Body body: ProjectStatusRequest): ProjectActivityDto

    @GET("api/projects/{id}/invitations")
    suspend fun invitations(@Path("id") id: String): List<ProjectInvitationDto>

    @POST("api/projects/{id}/invitations")
    suspend fun invite(@Path("id") id: String, @Body body: ProjectInvitationRequestDto): List<ProjectInvitationDto>

    @DELETE("api/projects/invitations/{id}")
    suspend fun revokeInvitation(@Path("id") id: String): ProjectInvitationDto

    @POST("api/projects/invitations/{id}/resend")
    suspend fun resendInvitation(@Path("id") id: String): ProjectInvitationDto

    @GET("api/projects/invitations/me")
    suspend fun myInvitations(): List<ProjectInvitationDto>

    @POST("api/projects/invitations/{id}/response")
    suspend fun respondInvitation(@Path("id") id: String, @Query("accept") accept: Boolean): ProjectInvitationDto

    @DELETE("api/projects/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String)

    @PATCH("api/projects/{id}/photos/{photoId}/cover")
    suspend fun setCover(@Path("id") id: String, @Path("photoId") photoId: String): ProjectPhotoDto
}

@Serializable
data class ProjectStatusRequest(val status: String)

@Serializable
data class ProjectInvitationDto(
    val id: String,
    val projectId: String? = null,
    val projectName: String? = null,
    val email: String? = null,
    val role: String? = null,
    val functionName: String? = null,
    val status: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class ProjectInvitationRequestDto(
    val email: String,
    val role: String,
    val functionName: String? = null,
    val locale: String = "pt-BR",
)

@Serializable
data class ProjectActivityRequestDto(
    val title: String,
    val description: String? = null,
    val type: String,
    val startsAt: String,
    val endsAt: String,
    val onsite: Boolean,
    val assigneeIds: List<String>,
)
