package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDiscoveryRepository @Inject constructor() : DiscoveryRepository {
    private val events = listOf(
        ScheduleEvent(4, Calendar.JANUARY, "09:30", "Visita técnica", "Fortech Enterprise"),
        ScheduleEvent(4, Calendar.JANUARY, "13:00", "Alinhamento do projeto", "Conversa com equipe"),
        ScheduleEvent(4, Calendar.JANUARY, "16:15", "Revisar proposta", "Solar Tech"),
    )
    private val savedLocations = listOf("São Paulo, SP", "São Paulo, SP", "São Paulo, SP", "Fortaleza, CE", "Sorocaba, SP")
    private var locationSaved = false

    override suspend fun events(): ApiResult<List<ScheduleEvent>> = ApiResult.Success(events)

    override suspend fun savedLocations(): ApiResult<List<String>> = ApiResult.Success(savedLocations)

    override suspend fun isLocationSaved(): ApiResult<Boolean> = ApiResult.Success(locationSaved)

    override suspend fun toggleLocationSaved(): ApiResult<Boolean> {
        locationSaved = !locationSaved
        return ApiResult.Success(locationSaved)
    }
}
