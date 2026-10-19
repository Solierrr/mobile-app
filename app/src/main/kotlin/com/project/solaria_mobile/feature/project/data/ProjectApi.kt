package com.project.solaria_mobile.feature.project.data

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

@Serializable
data class ProjectLocationDto(
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
    @GET("api/projects")
    suspend fun projects(): List<ProjectDto>

    @GET("api/projects/{id}")
    suspend fun project(@Path("id") id: String): ProjectDto

    @POST("api/projects")
    suspend fun create(@Body body: ProjectRequestDto): ProjectDto

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
}
