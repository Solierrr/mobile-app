package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnavailableDiscoveryRepository @Inject constructor() : DiscoveryRepository {
    override suspend fun events(): ApiResult<List<ScheduleEvent>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    override suspend fun savedLocations(): ApiResult<List<String>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    override suspend fun isLocationSaved(): ApiResult<Boolean> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    override suspend fun toggleLocationSaved(): ApiResult<Boolean> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
}
