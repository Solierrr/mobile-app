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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import com.project.solaria_mobile.core.designsystem.components.SkeletonGroup
import com.project.solaria_mobile.core.designsystem.components.SkeletonListRow

@Composable
fun ScheduleRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProject: (String) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
    google: GoogleCalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val googleState by google.state.collectAsStateWithLifecycle()
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val snackbar = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
    var export by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ScheduleEvent?>(null) }
    androidx.lifecycle.compose.LifecycleResumeEffect(date) {
        google.load(date)
        onPauseOrDispose { }
    }
    androidx.compose.runtime.LaunchedEffect(google) {
        google.authorization.collect { url ->
            val parsed = android.net.Uri.parse(url)
            if (parsed.scheme == "https" && parsed.host == "accounts.google.com") {
                try { uri.openUri(url) } catch (_: android.content.ActivityNotFoundException) { snackbar.showSnackbar("Nenhum navegador disponível para conectar o Google Agenda.") }
            } else snackbar.showSnackbar("O serviço retornou um endereço de autorização inválido.")
        }
    }
    Box(Modifier.fillMaxSize()) {
    val display = if (state is UiState.Error && googleState.connection != null) UiState.Content(buildScheduleState(emptyList(), date, ZoneId.systemDefault())) else state
    UiStateHost(display, viewModel::load, onBack = onBack, header = { ScreenTitle("Agenda", onBack) }, loading = { ScheduleSkeleton() }) { content ->
        val merged = buildScheduleState(content.events + googleState.events, content.selectedDate, ZoneId.systemDefault())
        ScheduleScreen(merged, onBack, onOpenSettings, viewModel::selectDay, viewModel::previousMonth, viewModel::nextMonth, viewModel::selectToday, onOpenProject, { viewModel.load(); google.load(date) }, state as? UiState.Content,
            solariaUnavailable = state is UiState.Error, googleState = googleState, onConnectGoogle = google::connect, onDisconnectGoogle = google::disconnect, onExport = { export = it },
            onOpenGoogle = { url -> val parsed = android.net.Uri.parse(url); if (parsed.scheme == "https" && parsed.host in setOf("calendar.google.com", "www.google.com")) runCatching { uri.openUri(url) } })
    }
    androidx.compose.material3.SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(Spacing.md))
    }
    export?.let { event -> CalendarExportSheet(event, googleState.busy, { export = null }) { end -> google.export(event, end); export = null } }
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
    onOpenProject: (String) -> Unit,
    onRefresh: () -> Unit,
    resource: UiState.Content<*>? = null,
    solariaUnavailable: Boolean = false,
    googleState: GoogleCalendarState = GoogleCalendarState(),
    onConnectGoogle: () -> Unit = {},
    onDisconnectGoogle: () -> Unit = {},
    onExport: (ScheduleEvent) -> Unit = {},
    onOpenGoogle: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().appBackground().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(top = Spacing.lg, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        ScreenTitle("Agenda", onBack, actions = { RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar agenda") })
        if (resource != null) ResourceStatus(resource)
        if (solariaUnavailable) Text("Não foi possível atualizar as atividades dos projetos. Tente atualizar a agenda.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
        CalendarCard(
            month = state.monthLabel,
            days = state.calendarDays,
            selectedDay = state.selectedDay,
            onSelectDay = onSelectDay,
            onPrevious = onPrevious,
            onNext = onNext,
        )
        GoogleCalendarControls(googleState, onConnectGoogle, onDisconnectGoogle)
        SectionHeading(state.selectedLabel, faded = true, action = "Hoje", onAction = onToday)
        if (state.selectedEvents.isEmpty()) {
            StateNotice("Dia livre", "Nenhuma atividade dos seus projetos está agendada para esta data.", icon = AppIcons.calendar_days)
        } else {
            state.selectedEvents.forEach { event ->
                Column {
                    ScheduleEventRow(event, state.selectedDate) { if (event.source == "google") event.externalUrl?.let(onOpenGoogle) else onOpenProject(it) }
                    if (event.source == "solaria" && googleState.connection?.connected == true && event.status != "CANCELED") {
                        ActionRow("Exportar para Google Agenda", AppIcons.calendar_days, { onExport(event) }, compact = true, enabled = !googleState.busy)
                    }
                }
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
                                .semantics(mergeDescendants = true) {
                                    if (day.inCurrentMonth) {
                                        contentDescription = "${day.number} de $month${if (day.hasEvents) ", com atividades" else ""}"
                                        selected = day.number == selectedDay
                                    }
                                }
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
                            if (day.hasEvents && day.inCurrentMonth) {
                                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.xxs).size(4.dp).clip(RoundedCornerShape(Radius.large)).background(AppTheme.colors.onSurfaceMuted))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleEventRow(event: ScheduleEvent, date: LocalDate, onOpenProject: (String) -> Unit) {
    val zone = ZoneId.systemDefault()
    val start = event.start(zone)
    val end = event.end(zone)?.takeIf { it.isAfter(start) }
    val time = if (event.allDay) "Dia inteiro" else if (start.toLocalDate() < date) "Em curso" else start.format(DateTimeFormatter.ofPattern("HH:mm"))
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.large)).clickable(onClickLabel = "Abrir projeto ${event.projectName}") { onOpenProject(event.projectId) }.padding(vertical = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Text(time, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.width(2.dp).height(34.dp).background(AppTheme.colors.onSurface))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(event.title, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(event.projectName, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            if (end != null) {
                val format = if (end.toLocalDate() == date) "HH:mm" else "dd/MM/yyyy 'às' HH:mm"
                Text("Até ${end.format(DateTimeFormatter.ofPattern(format))}", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
            if (event.status == "CANCELED") Text("Cancelada", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
    }
}

/** Cartão do calendário mensal e atividades do dia, sem datas falsas. */
@Composable
private fun ScheduleSkeleton() {
    SkeletonGroup(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Skeleton(Modifier.fillMaxWidth().height(CALENDAR_SKELETON_HEIGHT), RoundedCornerShape(Radius.large))
        repeat(3) { SkeletonListRow(circle = false) }
    }
}

private val CALENDAR_SKELETON_HEIGHT = 320.dp
