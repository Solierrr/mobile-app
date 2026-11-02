package com.project.solaria_mobile.feature.discovery.domain

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.readOnce
import kotlinx.coroutines.flow.Flow

interface DiscoveryRepository {
    suspend fun events(): ApiResult<List<ScheduleEvent>>
    fun observeEvents(): Flow<ReadResult<List<ScheduleEvent>>> = readOnce { events() }
    suspend fun savedLocations(): ApiResult<List<String>>
    suspend fun isLocationSaved(): ApiResult<Boolean>
    suspend fun toggleLocationSaved(): ApiResult<Boolean>
}
