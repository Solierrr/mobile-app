package com.project.solaria_mobile.feature.discovery.domain

import com.project.solaria_mobile.core.common.ApiResult

interface DiscoveryRepository {
    suspend fun events(): ApiResult<List<ScheduleEvent>>
    suspend fun savedLocations(): ApiResult<List<String>>
    suspend fun isLocationSaved(): ApiResult<Boolean>
    suspend fun toggleLocationSaved(): ApiResult<Boolean>
}
