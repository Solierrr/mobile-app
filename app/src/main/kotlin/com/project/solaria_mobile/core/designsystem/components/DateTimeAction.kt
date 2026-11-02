package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DateTimeAction(label: String, value: OffsetDateTime?, enabled: Boolean = true, onChange: (OffsetDateTime) -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var chosenDay by rememberSaveable { mutableStateOf(0L) }
    val seed = (value ?: OffsetDateTime.now().plusHours(1)).atZoneSameInstant(ZoneId.systemDefault())
    ActionRow(label, AppIcons.calendar_days, { step = 1 }, subtitle = value?.atZoneSameInstant(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")) ?: "Selecionar data e horário", enabled = enabled)
    if (step == 1) {
        AppDatePickerDialog(
            initial = seed.toLocalDate(),
            onConfirm = { day -> chosenDay = day.toEpochDay(); step = 2 },
            onDismiss = { step = 0 },
            confirmLabel = stringResource(R.string.common_continue),
        )
    }
    if (step == 2) {
        AppTimePickerDialog(
            initial = seed.toLocalTime(),
            onConfirm = { time ->
                onChange(LocalDate.ofEpochDay(chosenDay).atTime(time).atZone(ZoneId.systemDefault()).toOffsetDateTime())
                step = 0
            },
            onDismiss = { step = 0 },
        )
    }
}
