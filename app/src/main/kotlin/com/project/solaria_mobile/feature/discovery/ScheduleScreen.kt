@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.discovery

import com.project.solaria_mobile.feature.discovery.data.ScheduleEvent
import com.project.solaria_mobile.feature.discovery.data.CalendarDay

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
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaSize
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun ScheduleRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleScreen(state, onBack, onOpenSettings, viewModel::selectDay, viewModel::previousMonth, viewModel::nextMonth, viewModel::resetToDemoToday)
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
        modifier = Modifier.fillMaxSize().background(SolariaWhite).verticalScroll(rememberScrollState()).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg, bottom = SolariaSpace.screenBottom),
        verticalArrangement = Arrangement.spacedBy(SolariaSpace.lg),
    ) {
        ScreenTitle("Agenda", onBack, trailingIcon = LucideR.drawable.lucide_ic_ellipsis, trailingLabel = "Mais opções", onTrailingClick = onOpenSettings)
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
                color = SolariaInkMuted,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        } else {
            state.selectedEvents.forEach { event ->
                ScheduleEventRow(event)
            }
        }
        SectionHeading("Opções", faded = true)
        ActionRow("Personalizar jornada", LucideR.drawable.lucide_ic_sun, {})
        ActionRow("Google Agenda", LucideR.drawable.lucide_ic_calendar_days, {}, badge = "10")
        ActionRow("Configurações", LucideR.drawable.lucide_ic_settings, onOpenSettings)
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
        shape = RoundedCornerShape(SolariaRadius.large),
        color = SolariaWhite,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(SolariaSpace.md),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(LucideR.drawable.lucide_ic_chevron_left, "Mês anterior", onPrevious, size = SolariaSize.compactMapControl)
                Text(month, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                RoundIconButton(LucideR.drawable.lucide_ic_chevron_right, "Próximo mês", onNext, size = SolariaSize.compactMapControl)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), color = if (day == "Sáb") SolariaOrange else SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(SolariaSize.calendarDay)
                                .then(if (day.inCurrentMonth) Modifier.clickable { onSelectDay(day.number) } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day.inCurrentMonth && day.number == selectedDay) {
                                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(SolariaOrange), contentAlignment = Alignment.Center) {
                                    Text(day.number.toString(), color = SolariaWhite, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                                }
                            } else {
                                Text(day.number.toString(), color = if (day.inCurrentMonth) SolariaInk else SolariaInkMuted.copy(alpha = 0.55f), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SolariaSpace.md), verticalAlignment = Alignment.CenterVertically) {
        Text(event.time, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.width(2.dp).height(34.dp).background(SolariaOrange))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(event.title, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(event.place, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
    }
}
