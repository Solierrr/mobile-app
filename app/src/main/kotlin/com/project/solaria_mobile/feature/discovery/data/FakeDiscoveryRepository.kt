package com.project.solaria_mobile.feature.discovery.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent

@Singleton
class FakeDiscoveryRepository @Inject constructor() : DiscoveryRepository {
    override val events = MutableStateFlow(
        listOf(
            ScheduleEvent(4, Calendar.JANUARY, "09:30", "Visita técnica", "Fortech Enterprise"),
            ScheduleEvent(4, Calendar.JANUARY, "13:00", "Alinhamento do projeto", "Conversa com equipe"),
            ScheduleEvent(4, Calendar.JANUARY, "16:15", "Revisar proposta", "Solar Tech"),
        )
    )
    override val savedLocations = MutableStateFlow(
        listOf("São Paulo, SP", "São Paulo, SP", "São Paulo, SP", "Fortaleza, CE", "Sorocaba, SP")
    )
    private val mutableLocationSaved = MutableStateFlow(false)
    override val locationSaved: StateFlow<Boolean> = mutableLocationSaved
    override fun toggleLocationSaved() { mutableLocationSaved.value = !mutableLocationSaved.value }
}
