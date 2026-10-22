@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.discovery

import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import com.project.solaria_mobile.feature.discovery.domain.CalendarDay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.components.UiStateHost

@Composable
fun ScheduleRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Agenda", onBack) }, unavailableMessage = "Por enquanto, consulte as atividades na agenda de cada projeto.") { content ->
        ScheduleScreen(content, onBack, onOpenSettings, viewModel::selectDay, viewModel::previousMonth, viewModel::nextMonth, viewModel::selectToday)
    }
}

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(top = Spacing.lg, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        ScreenTitle("Agenda", onBack, trailingIcon = AppIcons.ellipsis, trailingLabel = "Mais opções", onTrailingClick = onOpenSettings)
        CalendarCard(
            month = state.monthLabel,
            days = state.calendarDays,
            selectedDay = state.selectedDay,
            onSelectDay = onSelectDay,
            onPrevious = onPrevious,
            onNext = onNext,
        )
        SectionHeading(state.selectedLabel, faded = true, action = "Hoje", onAction = onToday)
        if (state.selectedEvents.isEmpty()) {
            Text(
                text = "Nenhum evento para esta data",
                color = AppTheme.colors.onSurfaceMuted,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        } else {
            state.selectedEvents.forEach { event ->
                ScheduleEventRow(event)
            }
        }
        SectionHeading("Opções", faded = true)
        ActionRow("Configurações", AppIcons.settings, onOpenSettings)
    }
}

@Composable
private fun CalendarCard(
    month: String,
    days: List<CalendarDay>,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = AppTheme.colors.surface,
        shadowElevation = com.project.solaria_mobile.core.designsystem.theme.Elevation.floating,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(AppIcons.chevron_left, "Mês anterior", onPrevious, size = Sizing.compactMapControl)
                Text(month, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                RoundIconButton(AppIcons.chevron_right, "Próximo mês", onNext, size = Sizing.compactMapControl)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), color = if (day == "Sáb") AppTheme.colors.onSurface else AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(Sizing.calendarDay)
                                .then(if (day.inCurrentMonth) Modifier.clickable { onSelectDay(day.number) } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day.inCurrentMonth && day.number == selectedDay) {
                                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(AppTheme.colors.onSurface), contentAlignment = Alignment.Center) {
                                    Text(day.number.toString(), color = AppTheme.colors.onAccent, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                                }
                            } else {
                                Text(day.number.toString(), color = if (day.inCurrentMonth) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceMuted.copy(alpha = 0.55f), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleEventRow(event: ScheduleEvent) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Text(event.time, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.width(2.dp).height(34.dp).background(AppTheme.colors.onSurface))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(event.title, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(event.place, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
    }
}
