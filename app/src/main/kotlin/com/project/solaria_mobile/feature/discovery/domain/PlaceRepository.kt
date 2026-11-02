package com.project.solaria_mobile.feature.discovery.domain

import com.project.solaria_mobile.core.common.ApiResult

data class GeoPoint(val formattedAddress: String, val latitude: Double, val longitude: Double)

interface PlaceRepository {
    suspend fun geocode(address: String): ApiResult<GeoPoint>

    /** Endereços que casam com o texto (rua, CEP, bairro…), do mais provável ao menos; vazio vira `NOT_FOUND`. */
    suspend fun search(query: String): ApiResult<List<GeoPoint>> = when (val result = geocode(query)) {
        is ApiResult.Success -> ApiResult.Success(listOf(result.data))
        is ApiResult.Failure -> result
    }
    suspend fun isAvailable(): ApiResult<Boolean>
}
