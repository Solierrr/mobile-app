@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun DateTimeAction(label: String, value: OffsetDateTime?, enabled: Boolean = true, onChange: (OffsetDateTime) -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var chosenDay by rememberSaveable { mutableStateOf(0L) }
    val seed = (value ?: OffsetDateTime.now().plusHours(1)).atZoneSameInstant(ZoneId.systemDefault())
    ActionRow(label, AppIcons.calendar_days, { step = 1 }, subtitle = value?.atZoneSameInstant(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")) ?: "Selecionar data e horário", enabled = enabled)
    if (step == 1) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = seed.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { step = 0 }, confirmButton = {
            PrimaryButton("Continuar", {
                picker.selectedDateMillis?.let { chosenDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay(); step = 2 }
            }, enabled = picker.selectedDateMillis != null, compact = true)
        }, dismissButton = { PrimaryButton("Cancelar", { step = 0 }, secondary = true, compact = true) }) { DatePicker(picker) }
    }
    if (step == 2) {
        val picker = rememberTimePickerState(seed.hour, seed.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { step = 0 }, text = { TimePicker(picker) }, confirmButton = {
            PrimaryButton("Confirmar", {
                onChange(LocalDate.ofEpochDay(chosenDay).atTime(picker.hour, picker.minute).atZone(ZoneId.systemDefault()).toOffsetDateTime())
                step = 0
            }, compact = true)
        }, dismissButton = { PrimaryButton("Cancelar", { step = 0 }, secondary = true, compact = true) })
    }
}
