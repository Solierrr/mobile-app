package com.project.solaria_mobile.feature.project.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class ProjectLocationDto(val city: String? = null, val state: String? = null)

@Serializable
data class ProjectDto(
    val id: String,
    val name: String,
    val ownerName: String? = null,
    val status: String? = null,
    val location: ProjectLocationDto? = null,
)

interface ProjectApi {
    @GET("api/projects")
    suspend fun projects(): List<ProjectDto>

    @GET("api/projects/{id}")
    suspend fun project(@Path("id") id: String): ProjectDto
}
