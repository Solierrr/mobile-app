package com.project.solaria_mobile.feature.discovery.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

@Serializable
data class ShiftRequestDto(val technicianId: String, val dayWeek: String, val startDate: String, val endDate: String)

@Serializable
data class ShiftDto(
    val id: String,
    val technicianId: String? = null,
    val dayWeek: String,
    val startDate: String,
    val endDate: String,
)

/** Turnos do profissional no api-core: um registro por dia da semana. */
interface ShiftApi {
    @GET("api/shifts/technician/{technicianId}")
    suspend fun byTechnician(@Path("technicianId") technicianId: String): List<ShiftDto>

    @POST("api/shifts")
    suspend fun create(@Body body: ShiftRequestDto): ShiftDto

    @PUT("api/shifts/{id}")
    suspend fun update(@Path("id") id: String, @Body body: ShiftRequestDto): ShiftDto

    @DELETE("api/shifts/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
