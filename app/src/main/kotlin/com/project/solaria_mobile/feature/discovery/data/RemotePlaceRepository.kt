package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.discovery.domain.GeoPoint
import com.project.solaria_mobile.feature.discovery.domain.PlaceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePlaceRepository @Inject constructor(
    private val api: RegistryApi,
    private val executor: ApiCallExecutor,
) : PlaceRepository {
    private val enabled = BuildConfig.GOOGLE_REGISTRY_BASE_URL.isNotBlank()

    override suspend fun geocode(address: String): ApiResult<GeoPoint> {
        if (!enabled) return ApiResult.Failure(ErrorKind.FORBIDDEN)
        return when (val result = executor.execute { api.geocode(GeocodeBody(address)) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> result.data.results.firstOrNull()
                ?.let { ApiResult.Success(GeoPoint(it.formattedAddress, it.latitude, it.longitude)) }
                ?: ApiResult.Failure(ErrorKind.NOT_FOUND)
        }
    }

    override suspend fun isAvailable(): ApiResult<Boolean> {
        if (!enabled) return ApiResult.Failure(ErrorKind.FORBIDDEN)
        return when (val result = executor.execute { api.health() }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(result.data.isSuccessful)
        }
    }
}
