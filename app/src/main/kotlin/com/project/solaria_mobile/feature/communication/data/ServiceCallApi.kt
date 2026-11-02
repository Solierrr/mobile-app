package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.feature.project.data.ProjectLocationDto
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class ServiceCallDto(
    val id: String, val projectId: String, val projectName: String, val purpose: String, val status: String,
    val scheduledDate: String? = null, val acceptedAt: String? = null, val endDate: String? = null,
    val location: ProjectLocationDto? = null, val canManage: Boolean = false,
    val hasExecutors: Boolean = false, val managedByAgenda: Boolean = false,
)

@Serializable data class ServiceScheduleRequest(val scheduledDate: String)

interface ServiceCallApi {
    @GET("api/projects/services") suspend fun list(): List<ServiceCallDto>
    @GET("api/projects/services/{id}") suspend fun detail(@Path("id") id: String): ServiceCallDto
    @POST("api/projects/services/{id}/acceptance") suspend fun accept(@Path("id") id: String): ServiceCallDto
    @POST("api/projects/services/{id}/completion") suspend fun complete(@Path("id") id: String): ServiceCallDto
    @POST("api/projects/services/{id}/cancellation") suspend fun cancel(@Path("id") id: String): ServiceCallDto
    @PATCH("api/projects/services/{id}/schedule") suspend fun schedule(@Path("id") id: String, @Body body: ServiceScheduleRequest): ServiceCallDto
}
