package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDiscoveryRepository @Inject constructor() : DiscoveryRepository {
    private val events = listOf(
        ScheduleEvent("a1", "p1", "Fortech Enterprise", "Visita técnica", "2026-01-04T12:30:00Z"),
        ScheduleEvent("a2", "p1", "Conversa com equipe", "Alinhamento do projeto", "2026-01-04T16:00:00Z"),
        ScheduleEvent("a3", "p2", "Solar Tech", "Revisar proposta", "2026-01-04T19:15:00Z"),
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
