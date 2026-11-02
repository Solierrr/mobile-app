package com.project.solaria_mobile.feature.discovery.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class GeocodeBody(val address: String, val language: String = "pt-BR")

@Serializable
data class AddressDto(
    @SerialName("formatted_address") val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class AddressListDto(val results: List<AddressDto> = emptyList())

@Serializable
data class CalendarConnectDto(@SerialName("authorization_url") val authorizationUrl: String)

@Serializable
data class CalendarSlotDto(val start: String, val end: String)

@Serializable
data class CalendarAvailabilityDto(val busy: List<CalendarSlotDto> = emptyList())

interface RegistryApi {
    @GET("v1/calendar/connect")
    suspend fun calendarConnect(@Query("technician_id") technicianId: String): CalendarConnectDto

    @GET("v1/calendar/technicians/{technicianId}/availability")
    suspend fun calendarAvailability(
        @Path("technicianId") technicianId: String,
        @Query("from") from: String,
        @Query("to") to: String,
    ): CalendarAvailabilityDto

    @DELETE("v1/calendar/technicians/{technicianId}/connection")
    suspend fun calendarDisconnect(@Path("technicianId") technicianId: String): Response<Unit>

    @POST("v1/address/geocode")
    suspend fun geocode(@Body body: GeocodeBody): AddressListDto

    @GET("health")
    suspend fun health(): Response<Unit>
}
