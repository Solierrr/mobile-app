package com.project.solaria_mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.feature.discovery.domain.CalendarDay
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val selectedDay: Int = 1,
    val monthLabel: String = "",
    val selectedLabel: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val calendarDays: List<CalendarDay> = emptyList(),
    val selectedEvents: List<ScheduleEvent> = emptyList(),
)

@HiltViewModel
class ScheduleViewModel internal constructor(private val repository: DiscoveryRepository, private val today: LocalDate) : ViewModel() {
    @Inject constructor(repository: DiscoveryRepository) : this(repository, LocalDate.now())

    private val selection = MutableStateFlow(today)
    private val source = MutableStateFlow<UiState<List<ScheduleEvent>>>(UiState.Loading)
    private var loading: Job? = null

    val uiState: StateFlow<UiState<ScheduleUiState>> = combine(source, selection) { loaded, date ->
        loaded.map { buildScheduleState(it, date, ZoneId.systemDefault()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { load() }

    fun load() {
        loading?.cancel()
        loading = viewModelScope.launch { source.collectRead(repository.observeEvents()) }
    }

    fun selectDay(day: Int) {
        if (day in 1..selection.value.lengthOfMonth()) selection.value = selection.value.withDayOfMonth(day)
    }
    fun previousMonth() { selection.value = selection.value.minusMonths(1).withDayOfMonth(1) }
    fun nextMonth() { selection.value = selection.value.plusMonths(1).withDayOfMonth(1) }
    fun selectToday() { selection.value = today }
}

internal fun buildScheduleState(events: List<ScheduleEvent>, date: LocalDate, zone: ZoneId): ScheduleUiState {
    val locale = Locale.forLanguageTag("pt-BR")
    val month = date.withDayOfMonth(1)
    val first = month.minusDays((month.dayOfWeek.value % 7).toLong())
    return ScheduleUiState(
        selectedDay = date.dayOfMonth,
        monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM, yyyy", locale)).replaceFirstChar { it.titlecase(locale) },
        selectedLabel = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", locale)),
        selectedDate = date,
        calendarDays = List(42) { index ->
            val day = first.plusDays(index.toLong())
            CalendarDay(day.dayOfMonth, day.month == month.month && day.year == month.year, events.any { it.occursOn(day, zone) })
        },
        selectedEvents = events.filter { it.occursOn(date, zone) }.sortedBy { it.start(zone).toInstant() },
    )
}
