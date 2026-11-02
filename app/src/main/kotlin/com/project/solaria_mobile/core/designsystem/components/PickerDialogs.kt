@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Seletor de data no popup padrão; devolve o dia escolhido. */
@Composable
fun AppDatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Confirmar",
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    AppDialog(
        onDismissRequest = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = { state.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } },
        confirmEnabled = state.selectedDateMillis != null,
        wideContent = true,
    ) { DatePicker(state) }
}
