package com.project.solaria_mobile.feature.discovery

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AppButton
import com.project.solaria_mobile.core.designsystem.components.AppTimeRangePickerDialog
import com.project.solaria_mobile.core.designsystem.components.ButtonVariant
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.containerFill
import com.project.solaria_mobile.feature.discovery.domain.WorkJourney
import com.project.solaria_mobile.feature.discovery.domain.WorkShift
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HOUR = DateTimeFormatter.ofPattern("HH:mm")

internal typealias Hours = Pair<LocalTime, LocalTime>

/** Opções da agenda: jornada (só para autônomos), Google Agenda e configurações. */
@Composable
internal fun ScheduleOptionsSheet(
    state: ScheduleOptionsState,
    onJourney: () -> Unit,
    onGoogle: () -> Unit,
    onSettings: () -> Unit,
) {
    val dismiss = LocalSheetDismiss.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(stringResource(R.string.so_opcoes_da_agenda), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
        if (state.independent) {
            ActionRow(stringResource(R.string.journey_sheet_title), AppIcons.cloud_sun, onJourney)
        }
        if (state.hasOptions) GoogleCalendarRow(state.google, onGoogle)
        ActionRow(stringResource(R.string.settings_title), AppIcons.settings, { dismiss(); onSettings() })
    }
}

@Composable
private fun GoogleCalendarRow(link: GoogleLink, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.medium), color = containerFill()) {
        Row(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Image(painterResource(R.drawable.ic_google_calendar), null, Modifier.size(Sizing.icon))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(stringResource(R.string.sc_google_agenda), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge)
                Text(googleStatusLabel(link), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
internal fun googleStatusLabel(link: GoogleLink): String = when (link) {
    GoogleLink.CHECKING -> stringResource(R.string.so_verificando)
    GoogleLink.CONNECTED -> stringResource(R.string.so_conectado)
    GoogleLink.DISCONNECTED -> stringResource(R.string.so_conectar_a_sua_agenda)
    GoogleLink.UNAVAILABLE -> stringResource(R.string.so_indisponivel_no_momento)
}

/** Autorização do Google Agenda: o app só lê quando a pessoa está ocupada, nunca o conteúdo dos compromissos. */
@Composable
internal fun GoogleCalendarSheet(state: ScheduleOptionsState, onConnect: () -> Unit, onDisconnect: () -> Unit, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Image(painterResource(R.drawable.ic_google_calendar), null, Modifier.size(Sizing.avatar))
            Column {
                Text(stringResource(R.string.sc_google_agenda), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(googleStatusLabel(state.google), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            stringResource(R.string.so_ao_conectar_os_horarios_em) +
                stringResource(R.string.so_o_app_nao_le_o),
            color = AppTheme.colors.onSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        state.googleFailed?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
        when (state.google) {
            GoogleLink.CONNECTED -> AppButton(stringResource(R.string.so_desconectar), onDisconnect, variant = ButtonVariant.Secondary, loading = state.googleBusy)
            GoogleLink.DISCONNECTED -> AppButton(stringResource(R.string.so_conectar_com_google), onConnect, loading = state.googleBusy)
            GoogleLink.CHECKING -> AppButton(stringResource(R.string.so_verificando), {}, enabled = false)
            GoogleLink.UNAVAILABLE -> AppButton(stringResource(R.string.pr_tentar_de_novo), onRetry, variant = ButtonVariant.Secondary)
        }
    }
}

/** Um dia de atendimento na jornada em edição; enquanto [hours] for `null`, o horário ainda não foi especificado. */
data class DaySlot(val hours: Pair<LocalTime, LocalTime>? = null)

/** Jornada em edição: por dia, o atendimento ([DaySlot]) ou `null` na folga. Fica incompleta até todo dia ter horário. */
internal typealias JourneyDraft = Map<DayOfWeek, DaySlot?>

/** Dias de atendimento sem horário ou com o fim antes do início: a jornada só pode ser salva sem eles. */
internal fun pendingDays(draft: JourneyDraft): Set<DayOfWeek> = draft.filterValues { slot ->
    slot != null && slot.hours.let { it == null || !it.second.isAfter(it.first) }
}.keys

/** Primeiro horário já definido na jornada, usado para preencher os demais dias. */
internal fun JourneyDraft.referenceHours(): Hours? = DayOfWeek.entries.firstNotNullOfOrNull { this[it]?.hours }

/** Liga ou desliga o [day]; ao ligar, volta com o último horário que ele tinha ([previous]) ou, sem ele, o dos outros dias. */
internal fun JourneyDraft.toggled(day: DayOfWeek, previous: Hours? = null): JourneyDraft =
    this + (day to if (this[day] == null) DaySlot(previous ?: referenceHours()) else null)

/** Define o horário só do [day]; os outros dias não mudam. */
internal fun JourneyDraft.withHours(day: DayOfWeek, hours: Hours): JourneyDraft = mapValues { (other, slot) ->
    if (other == day && slot != null) DaySlot(hours) else slot
}

/** Define o mesmo horário em todos os dias de atendimento; os dias de folga continuam de folga. */
internal fun JourneyDraft.withAllHours(hours: Hours): JourneyDraft = mapValues { (_, slot) -> slot?.let { DaySlot(hours) } }

/** Jornada semanal numa sheet da Agenda: o editor mais o botão de salvar. */
@Composable
internal fun WorkJourneySheet(state: ScheduleOptionsState, onSave: (WorkJourney, () -> Unit) -> Unit) {
    val dismiss = LocalSheetDismiss.current
    val initial = remember(state.journey) { state.journey?.takeIf { it.days.isNotEmpty() }?.let(::draftOf) ?: unspecifiedWeekdays() }
    var draft by remember(initial) { mutableStateOf(initial) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(stringResource(R.string.journey_sheet_title), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.journey_sheet_hint), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        WorkJourneyEditor(draft, { draft = it })
        state.saveFailed?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
        AppButton(stringResource(R.string.journey_sheet_save), { onSave(journeyOf(draft), dismiss) }, enabled = pendingDays(draft).isEmpty(), loading = state.saving)
    }
}

/** Editor da jornada semanal: bolinhas ligam os dias e cada dia ligado mostra seu horário, que o relógio ajusta. */
@Composable
internal fun WorkJourneyEditor(draft: JourneyDraft, onChange: (JourneyDraft) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf<DayOfWeek?>(null) }
    // Quem desliga um dia e liga de novo espera o horário de antes, e não o de outro dia.
    val lastHours = remember { mutableMapOf<DayOfWeek, Hours>() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.journey_question), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
        JourneyPreset.entries.forEach { preset ->
            ActionRow(stringResource(preset.label), preset.icon, { onChange(draft.withPreset(preset)) }, trailing = if (draft.preset() == preset) stringResource(R.string.journey_preset_selected) else null)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
            WEEK.forEach { day ->
                DayCircle(day, selected = draft[day] != null) {
                    draft[day]?.hours?.let { lastHours[day] = it }
                    onChange(draft.toggled(day, lastHours[day]))
                }
            }
        }
        WEEK.forEach { day ->
            val slot = draft[day] ?: return@forEach
            val changeHours = stringResource(R.string.journey_change_hours, dayName(day), hoursLabel(slot.hours))
            Surface(
                onClick = { picking = day },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = changeHours },
                shape = RoundedCornerShape(Radius.medium),
                color = containerFill(),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = DAY_ROW_MIN_HEIGHT).padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(dayName(day), Modifier.weight(1f), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge, maxLines = 1, softWrap = false)
                    Text(
                        hoursLabel(slot.hours),
                        color = if (slot.hours == null) AppTheme.colors.onSurfaceMuted else AppTheme.colors.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Icon(painterResource(AppIcons.clock), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurface)
                }
            }
        }
    }
    picking?.let { day ->
        val slot = draft[day]
        if (slot == null) picking = null
        else AppTimeRangePickerDialog(
            initial = slot.hours ?: draft.referenceHours(),
            onConfirm = { start, end ->
                onChange(draft.withHours(day, start to end))
                picking = null
            },
            onConfirmAll = { start, end ->
                onChange(draft.withAllHours(start to end))
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

/** Bolinha com a inicial do dia; ligada quando a pessoa atende naquele dia. */
@Composable
private fun DayCircle(day: DayOfWeek, selected: Boolean, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val name = dayName(day)
    val state = stringResource(if (selected) R.string.journey_state_works else R.string.journey_state_off)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(Sizing.touchTarget).semantics { contentDescription = name; stateDescription = state },
        shape = CircleShape,
        color = if (selected) AppTheme.colors.action else containerFill(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                color = if (selected) AppTheme.colors.onAccent else AppTheme.colors.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private val DAY_ROW_MIN_HEIGHT = 64.dp

/** Mesma ordem do calendário da tela: domingo primeiro. */
private val WEEK = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

/** "08:00 às 12:45", ou "Não especificado" enquanto o dia não tem horário. */
@Composable
internal fun hoursLabel(hours: Hours?): String =
    hours?.let { stringResource(R.string.hours_range, it.first.format(HOUR), it.second.format(HOUR)) } ?: stringResource(R.string.hours_unspecified)

@Composable
private fun dayName(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> R.string.day_monday
        DayOfWeek.TUESDAY -> R.string.day_tuesday
        DayOfWeek.WEDNESDAY -> R.string.day_wednesday
        DayOfWeek.THURSDAY -> R.string.day_thursday
        DayOfWeek.FRIDAY -> R.string.day_friday
        DayOfWeek.SATURDAY -> R.string.day_saturday
        DayOfWeek.SUNDAY -> R.string.day_sunday
    },
)

internal fun draftOf(journey: WorkJourney): JourneyDraft =
    DayOfWeek.entries.associateWith { day -> journey.on(day)?.let { DaySlot(it.start to it.end) } }

/** Dias úteis ligados, ainda sem horário: o ponto de partida de quem não tem jornada salva. */
internal fun unspecifiedWeekdays(): JourneyDraft = DayOfWeek.entries.associateWith { day -> DaySlot().takeIf { day in WorkJourney.Weekdays } }

/** Só os dias ligados do rascunho, tenham ou não horário. */
internal fun JourneyDraft.daysSummary(): DaysSummary = daysSummaryOf(filterValues { it != null }.keys)

/** A jornada que o rascunho descreve; dias sem horário definido ficam de fora. */
internal fun journeyOf(draft: JourneyDraft): WorkJourney =
    WorkJourney(draft.mapNotNull { (day, slot) -> slot?.hours?.let { day to WorkShift(it.first, it.second) } }.toMap())

/** Quais dias a jornada cobre, sem texto: quem mostra escolhe as palavras do idioma atual. */
internal sealed interface DaysSummary {
    data object None : DaysSummary
    data object Weekdays : DaysSummary
    data object EveryDay : DaysSummary
    data class Some(val days: List<DayOfWeek>) : DaysSummary
}

internal fun daysSummaryOf(days: Set<DayOfWeek>): DaysSummary {
    val ordered = DayOfWeek.entries.filter { it in days }
    return when {
        ordered.isEmpty() -> DaysSummary.None
        ordered == WorkJourney.Weekdays -> DaysSummary.Weekdays
        ordered.size == 7 -> DaysSummary.EveryDay
        else -> DaysSummary.Some(ordered)
    }
}

internal fun WorkJourney.daysSummary(): DaysSummary = daysSummaryOf(days.keys)

/** "Seg a Sex", "Todos os dias" ou "Seg, Qua, Sex"; vazio sem nenhum dia. */
@Composable
internal fun DaysSummary.label(): String = when (this) {
    DaysSummary.None -> ""
    DaysSummary.Weekdays -> stringResource(R.string.days_weekdays_range)
    DaysSummary.EveryDay -> stringResource(R.string.preset_every_day)
    is DaysSummary.Some -> {
        val locale = LocalConfiguration.current.locales[0]
        days.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').replaceFirstChar { char -> char.titlecase(locale) } }
    }
}
