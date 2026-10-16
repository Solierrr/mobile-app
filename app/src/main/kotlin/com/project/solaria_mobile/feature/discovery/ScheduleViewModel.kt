package com.project.solaria_mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.feature.discovery.domain.CalendarDay
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val monthOffset: Int = 0,
    val selectedDay: Int = 4,
    val monthLabel: String = "Janeiro, 2026",
    val selectedLabel: String = "4 de Janeiro",
    val calendarDays: List<CalendarDay> = emptyList(),
    val selectedEvents: List<ScheduleEvent> = emptyList(),
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(private val repository: DiscoveryRepository) : ViewModel() {
    private data class Selection(val monthOffset: Int = 0, val selectedDay: Int = 4)

    private val selection = MutableStateFlow(Selection())
    private val source = MutableStateFlow<UiState<List<ScheduleEvent>>>(UiState.Loading)
    private val months = listOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")

    val uiState: StateFlow<UiState<ScheduleUiState>> = combine(source, selection) { loaded, current ->
        loaded.map { events -> buildState(events, current.monthOffset, current.selectedDay) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch { source.load { repository.events() } }
    }

    fun selectDay(day: Int) { selection.value = selection.value.copy(selectedDay = day) }
    fun previousMonth() { selection.value = Selection(selection.value.monthOffset - 1, 1) }
    fun nextMonth() { selection.value = Selection(selection.value.monthOffset + 1, 1) }
    fun resetToDemoToday() { selection.value = Selection() }

    private fun buildState(events: List<ScheduleEvent>, offset: Int, day: Int): ScheduleUiState {
        val firstDay = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 1); add(Calendar.MONTH, offset) }
        val month = firstDay.get(Calendar.MONTH)
        val first = (firstDay.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -(get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)) }
        val days = List(42) { index ->
            val date = (first.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
            CalendarDay(date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.MONTH) == month)
        }
        return ScheduleUiState(
            monthOffset = offset,
            selectedDay = day,
            monthLabel = "${months[month]}, ${firstDay.get(Calendar.YEAR)}",
            selectedLabel = "$day de ${months[month]}",
            calendarDays = days,
            selectedEvents = events.filter { it.day == day && it.month == month },
        )
    }
}
