package com.project.solaria_mobile.feature.discovery.domain

import kotlinx.coroutines.flow.StateFlow

interface DiscoveryRepository {
    val events: StateFlow<List<ScheduleEvent>>
    val savedLocations: StateFlow<List<String>>
    val locationSaved: StateFlow<Boolean>
    fun toggleLocationSaved()
}
