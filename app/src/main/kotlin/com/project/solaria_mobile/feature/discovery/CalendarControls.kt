@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.DateTimeAction
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import java.time.ZoneId
import java.time.OffsetDateTime

@Composable
internal fun GoogleCalendarControls(state: GoogleCalendarState, connect: () -> Unit, disconnect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (state.connection?.configured == true) {
            ActionRow(if (state.connection.connected) "Desconectar Google Agenda" else if (state.connection.reconnectRequired) "Reconectar Google Agenda" else "Conectar Google Agenda", AppIcons.calendar_days, if (state.connection.connected) disconnect else connect, subtitle = "Eventos pessoais e exportação de atividades", enabled = !state.busy)
        }
        val message = state.message ?: if (state.connection?.configured == false) "Google Agenda ainda não está configurado neste ambiente." else null
        if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
    }
}

@Composable
internal fun CalendarExportSheet(event: ScheduleEvent, busy: Boolean, dismiss: () -> Unit, confirm: (OffsetDateTime) -> Unit) {
    var end by remember(event.id) { mutableStateOf(event.end(ZoneId.systemDefault())?.toOffsetDateTime()) }
    val start = event.start(ZoneId.systemDefault()).toOffsetDateTime()
    BottomSheet(dismiss) {
        Column(Modifier.padding(Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SectionHeading("Exportar atividade")
            Text(event.title, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.onSurface)
            Text("A exportação cria ou atualiza o evento no Google. Alterações feitas lá não modificam o projeto.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
            DateTimeAction("Término", end, !busy) { end = it }
            if (end != null && !end!!.isAfter(start)) Text("O término precisa ser após o início da atividade.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
            PrimaryButton("Exportar", { end?.let(confirm) }, enabled = !busy && end?.isAfter(start) == true, gradient = true)
        }
    }
}
