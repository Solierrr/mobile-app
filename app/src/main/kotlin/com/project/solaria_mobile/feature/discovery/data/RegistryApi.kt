package com.project.solaria_mobile.feature.discovery.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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

interface RegistryApi {
    @POST("v1/address/geocode")
    suspend fun geocode(@Body body: GeocodeBody): AddressListDto

    @GET("health")
    suspend fun health(): Response<Unit>
}
