package com.project.solaria_mobile.presentation.discovery

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.MapBackdrop
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite
import kotlinx.coroutines.launch
import java.util.Calendar

private data class ScheduleEvent(val day: Int, val month: Int, val time: String, val title: String, val place: String)
private data class CalendarDay(val number: Int, val inCurrentMonth: Boolean)

private val dayEvents = listOf(
    ScheduleEvent(4, Calendar.JANUARY, "09:30", "Visita técnica", "Fortech Enterprise"),
    ScheduleEvent(4, Calendar.JANUARY, "13:00", "Alinhamento do projeto", "Conversa com equipe"),
    ScheduleEvent(4, Calendar.JANUARY, "16:15", "Revisar proposta", "Solar Tech"),
)

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var month by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableIntStateOf(4) }
    val months = listOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")
    val firstDay = remember(month) {
        Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1)
            add(Calendar.MONTH, month)
        }
    }
    val currentMonth = months[firstDay.get(Calendar.MONTH)]
    val selectedEvents = dayEvents.filter {
        it.day == selectedDay && it.month == firstDay.get(Calendar.MONTH)
    }
    val calendarDays = remember(month) {
        val first = firstDay.clone() as Calendar
        val offset = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        first.add(Calendar.DAY_OF_MONTH, -offset)
        List(42) { index ->
            val date = first.clone() as Calendar
            date.add(Calendar.DAY_OF_MONTH, index)
            CalendarDay(
                number = date.get(Calendar.DAY_OF_MONTH),
                inCurrentMonth = date.get(Calendar.MONTH) == firstDay.get(Calendar.MONTH),
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(SolariaWhite).verticalScroll(rememberScrollState()).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg, bottom = SolariaSpace.screenBottom),
        verticalArrangement = Arrangement.spacedBy(SolariaSpace.lg),
    ) {
        ScreenTitle("Agenda", onBack, trailingIcon = R.drawable.lucide_ic_ellipsis, trailingLabel = "Mais opções", onTrailingClick = onOpenSettings)
        CalendarCard(
            month = "$currentMonth, ${firstDay.get(Calendar.YEAR)}",
            days = calendarDays,
            selectedDay = selectedDay,
            onSelectDay = { selectedDay = it },
            onPrevious = { month -= 1; selectedDay = 1 },
            onNext = { month += 1; selectedDay = 1 },
        )
        SectionHeading("$selectedDay de $currentMonth", faded = true, action = "Hoje", onAction = { month = 0; selectedDay = 4 })
        if (selectedEvents.isEmpty()) {
            Text(
                text = "Nenhum evento para esta data",
                color = SolariaInkMuted,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        } else {
            selectedEvents.forEach { event ->
                ScheduleEventRow(event)
            }
        }
        SectionHeading("Opções", faded = true)
        ActionRow("Personalizar jornada", R.drawable.lucide_ic_sun, {})
        ActionRow("Google Agenda", R.drawable.lucide_ic_calendar_days, {}, badge = "10")
        ActionRow("Configurações", R.drawable.lucide_ic_settings, onOpenSettings)
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
                RoundIconButton(R.drawable.lucide_ic_chevron_left, "Mês anterior", onPrevious, size = 40.dp)
                Text(month, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                RoundIconButton(R.drawable.lucide_ic_chevron_right, "Próximo mês", onNext, size = 40.dp)
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
                                .height(40.dp)
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

private val savedLocations = listOf("São Paulo, SP", "São Paulo, SP", "São Paulo, SP", "Fortaleza, CE", "Sorocaba, SP")

@Composable
fun MapScreen(onBack: () -> Unit) {
    var locationsOpen by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(SolariaWhite)) {
        MapBackdrop(modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoundIconButton(R.drawable.lucide_ic_chevron_left, "Voltar", onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                RoundIconButton(R.drawable.lucide_ic_search, "Buscar local", {})
                RoundIconButton(R.drawable.lucide_ic_ellipsis, "Locais salvos", { locationsOpen = true })
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(bottom = SolariaSpace.screenBottom),
            shape = RoundedCornerShape(SolariaRadius.sheet),
            color = SolariaWhite,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(SolariaSpace.xl), verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(R.drawable.lucide_ic_chevron_up, "Expandir detalhes", { locationsOpen = true }, size = 40.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                        RoundIconButton(R.drawable.lucide_ic_map_pin, "Minha localização", {}, size = 40.dp)
                        RoundIconButton(R.drawable.lucide_ic_maximize, "Expandir mapa", {}, size = 40.dp)
                    }
                }
                Text("Há 22 km da localização atual", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Text("Santana de Parnaíba, SP", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                    ActionRow("Google Maps", R.drawable.lucide_ic_map_pin, {}, compact = true, modifier = Modifier.weight(1f))
                    ActionRow(if (saved) "Salvo" else "Salvar", R.drawable.lucide_ic_bookmark, { saved = !saved }, compact = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (locationsOpen) {
        ModalBottomSheet(
            onDismissRequest = { locationsOpen = false },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
            dragHandle = null,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom), verticalArrangement = Arrangement.spacedBy(SolariaSpace.md)) {
                ScreenTitle("Locais salvos", onBack = { scope.launch { sheetState.hide(); locationsOpen = false } })
                LazyColumn(verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                    items(savedLocations) { location ->
                        ActionRow(location, R.drawable.lucide_ic_map_pin, { locationsOpen = false }, trailing = "···")
                    }
                }
            }
        }
    }
}
