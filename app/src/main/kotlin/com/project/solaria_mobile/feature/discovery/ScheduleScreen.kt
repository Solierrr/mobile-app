@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.discovery

import com.project.solaria_mobile.core.designsystem.components.localizedActivityType
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import com.project.solaria_mobile.feature.discovery.domain.CalendarDay

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.feature.discovery.domain.BusySlot
import com.project.solaria_mobile.feature.project.activityTypeOf
import com.project.solaria_mobile.feature.project.icon
import com.project.solaria_mobile.feature.discovery.domain.occursOn
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
import androidx.compose.ui.res.painterResource
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.StatusBadge
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
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
    options: ScheduleOptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val optionState by options.state.collectAsStateWithLifecycle()
    var optionsOpen by rememberSaveable { mutableStateOf(false) }
    var journeyOpen by rememberSaveable { mutableStateOf(false) }
    var googleOpen by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    // A autorização acontece no navegador; ao voltar para o app a conexão é conferida de novo.
    LifecycleResumeEffect(options) {
        options.refreshGoogle()
        onPauseOrDispose {}
    }
    LaunchedEffect(optionState.authorizationUrl) {
        optionState.authorizationUrl?.let { url ->
            uriHandler.openUri(url)
            options.authorizationOpened()
        }
    }
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.pd_agenda), onBack) }, loading = { ScheduleSkeleton() }) { content ->
        LaunchedEffect(content.selectedDate.withDayOfMonth(1)) { options.showMonth(content.selectedDate) }
        ScheduleScreen(content, onBack, { optionsOpen = true }, viewModel::selectDay, viewModel::previousMonth, viewModel::nextMonth, viewModel::selectToday, onOpenProject, viewModel::load, state as? UiState.Content, optionState)
    }
    if (optionsOpen) BottomSheet(onDismiss = { optionsOpen = false }) {
        ScheduleOptionsSheet(optionState, onJourney = { journeyOpen = true }, onGoogle = { googleOpen = true }, onSettings = onOpenSettings)
    }
    if (journeyOpen) BottomSheet(onDismiss = { journeyOpen = false; options.clearSaveError() }) {
        WorkJourneySheet(optionState, onSave = options::saveJourney)
    }
    if (googleOpen) BottomSheet(onDismiss = { googleOpen = false }) {
        GoogleCalendarSheet(optionState, options::connectGoogle, options::disconnectGoogle, options::refreshGoogle)
    }
}

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenProject: (String) -> Unit,
    onRefresh: () -> Unit,
    resource: UiState.Content<*>? = null,
    options: ScheduleOptionsState = ScheduleOptionsState(),
) {
    val zone = ZoneId.systemDefault()
    val journey = options.journey?.takeIf { options.independent && it.days.isNotEmpty() }
    val busy = options.busy.filter { it.occursOn(state.selectedDate, zone) }.sortedBy { it.start }
    Column(
        modifier = Modifier.fillMaxSize().appBackground().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(top = Spacing.lg, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        ScreenTitle(stringResource(R.string.pd_agenda), onBack, actions = {
            RoundIconButton(AppIcons.sliders_horizontal, stringResource(R.string.so_opcoes_da_agenda), onOpenOptions)
            RefreshButton(onRefresh, resource?.refreshing == true, stringResource(R.string.sc_atualizar_agenda))
        })
        if (resource != null) ResourceStatus(resource)
        CalendarCard(
            month = state.monthLabel,
            days = state.calendarDays,
            selectedDay = state.selectedDay,
            onSelectDay = onSelectDay,
            onPrevious = onPrevious,
            onNext = onNext,
            onToday = onToday,
        )
        SectionHeading(state.selectedLabel, faded = true)
        if (journey != null) {
            val shift = journey.on(state.selectedDate.dayOfWeek)
            val hour = DateTimeFormatter.ofPattern("HH:mm")
            Text(
                if (shift == null) stringResource(R.string.sc_dia_de_folga_na_sua) else "Jornada: ${shift.start.format(hour)} às ${shift.end.format(hour)}",
                color = AppTheme.colors.onSurfaceMuted,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.selectedEvents.isEmpty() && busy.isEmpty()) {
            StateNotice(stringResource(R.string.sc_dia_livre), "Nenhuma atividade dos seus projetos está agendada para esta data.", icon = AppIcons.calendar_days)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                state.selectedEvents.forEach { event ->
                    val start = event.start(zone)
                    val outside = journey != null && start.toLocalDate() == state.selectedDate &&
                        journey.on(state.selectedDate.dayOfWeek)?.contains(start.toLocalTime()) != true
                    ScheduleEventRow(event, state.selectedDate, onOpenProject, outsideJourney = outside)
                }
                busy.forEach { BusyRow(it, state.selectedDate, zone) }
            }
        }
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
    onToday: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = com.project.solaria_mobile.core.designsystem.theme.containerFill(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(AppIcons.chevron_left, stringResource(R.string.sc_mes_anterior), onPrevious, size = Sizing.compactMapControl)
                Text(month, Modifier.clip(RoundedCornerShape(Radius.medium)).clickable(onClickLabel = stringResource(R.string.sc_voltar_para_hoje), onClick = onToday).padding(horizontal = Spacing.sm, vertical = Spacing.xxs), color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                RoundIconButton(AppIcons.chevron_right, stringResource(R.string.sc_proximo_mes), onNext, size = Sizing.compactMapControl)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(stringResource(R.string.sc_dom), stringResource(R.string.sc_seg), stringResource(R.string.sc_ter), stringResource(R.string.sc_qua), stringResource(R.string.sc_qui), stringResource(R.string.sc_sex), stringResource(R.string.sc_sab)).forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), color = if (day == stringResource(R.string.sc_sab)) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
            val dayFormat = stringResource(R.string.sc_day_format)
            val withActivities = stringResource(R.string.sc_com_atividades)
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(Sizing.calendarDay)
                                .semantics(mergeDescendants = true) {
                                    if (day.inCurrentMonth) {
                                        contentDescription = dayFormat.format(day.number, month) + if (day.hasEvents) withActivities else ""
                                        selected = day.number == selectedDay
                                    }
                                }
                                .then(if (day.inCurrentMonth) Modifier.clickable { onSelectDay(day.number) } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day.inCurrentMonth && day.number == selectedDay) {
                                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(AppTheme.colors.action), contentAlignment = Alignment.Center) {
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
private fun ScheduleEventRow(event: ScheduleEvent, date: LocalDate, onOpenProject: (String) -> Unit, outsideJourney: Boolean = false) {
    val zone = ZoneId.systemDefault()
    val start = event.start(zone)
    val end = event.end(zone)?.takeIf { it.isAfter(start) }
    val hour = DateTimeFormatter.ofPattern("HH:mm")
    val time = if (start.toLocalDate() < date) stringResource(R.string.sc_em_curso) else start.format(hour)
    val until = end?.let { "até " + it.format(DateTimeFormatter.ofPattern(if (it.toLocalDate() == date) "HH:mm" else "dd/MM HH:mm")) }
    val canceled = event.status == "CANCELED"
    val type = activityTypeOf(event.type)
    DayCard(
        time = time,
        until = until,
        title = event.title,
        subtitle = event.projectName,
        onClick = { onOpenProject(event.projectId) },
        clickLabel = stringResource(R.string.sc_click_label, localizedActivityType(type.label), event.projectName),
        badges = listOfNotNull(stringResource(R.string.sc_cancelada).takeIf { canceled }, stringResource(R.string.sc_fora_da_sua_jornada).takeIf { outsideJourney }),
        icon = {
            Icon(painterResource(type.icon), null, Modifier.size(Sizing.icon), tint = if (canceled) AppTheme.colors.onSurfaceMuted else AppTheme.colors.onSurface)
        },
    )
}

/** Faixa ocupada no Google Agenda: só o intervalo, sem título nem detalhes do compromisso. */
@Composable
private fun BusyRow(slot: BusySlot, date: LocalDate, zone: ZoneId) {
    val start = slot.start.atZone(zone)
    val end = slot.end.atZone(zone)
    val hour = DateTimeFormatter.ofPattern("HH:mm")
    DayCard(
        time = if (start.toLocalDate() < date) stringResource(R.string.sc_em_curso) else start.format(hour),
        until = stringResource(R.string.sc_ate) + if (end.toLocalDate() > date) stringResource(R.string.sc_o_fim_do_dia) else end.format(hour),
        title = stringResource(R.string.sc_ocupado),
        subtitle = stringResource(R.string.sc_google_agenda),
        onClick = null,
        clickLabel = null,
        badges = emptyList(),
        icon = { Image(painterResource(R.drawable.ic_google_calendar), null, Modifier.size(Sizing.icon)) },
    )
}

/** Cartão de um item do dia: ícone do tipo à esquerda e, à direita, horário, título, origem e etiquetas. */
@Composable
private fun DayCard(
    time: String,
    until: String?,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    clickLabel: String?,
    badges: List<String>,
    icon: @Composable () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Row(Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(time, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    if (until != null) Text(until, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                }
                Text(title, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Text(subtitle, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                if (badges.isNotEmpty()) Row(Modifier.padding(top = Spacing.xxs), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) { badges.forEach { StatusBadge(it) } }
            }
        }
    }
    val shape = RoundedCornerShape(Radius.large)
    if (onClick == null) Surface(Modifier.fillMaxWidth(), shape = shape, color = com.project.solaria_mobile.core.designsystem.theme.containerFill(), content = content)
    else Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { if (clickLabel != null) contentDescription = clickLabel }, shape = shape, color = com.project.solaria_mobile.core.designsystem.theme.containerFill(), content = content)
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
