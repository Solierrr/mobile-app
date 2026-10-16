package com.project.solaria_mobile.feature.discovery.domain

import com.project.solaria_mobile.core.common.ApiResult

data class GeoPoint(val formattedAddress: String, val latitude: Double, val longitude: Double)

interface PlaceRepository {
    suspend fun geocode(address: String): ApiResult<GeoPoint>
    suspend fun isAvailable(): ApiResult<Boolean>
}
