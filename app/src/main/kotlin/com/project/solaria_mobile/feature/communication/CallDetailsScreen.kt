@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.DateTimeAction
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.StatusBadge
import com.project.solaria_mobile.core.designsystem.components.TitleBlockSkeleton
import com.project.solaria_mobile.core.designsystem.components.TransientFeedback
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.domain.ServiceDecision
import com.project.solaria_mobile.presentation.navigation.AppDestination
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CallDetailsRoute(onBack: () -> Unit, onNavigate: (AppDestination) -> Unit = {}, viewModel: CallDetailsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.load() }
    Box(Modifier.fillMaxSize()) {
        UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Serviço técnico", onBack) }, loading = { TitleBlockSkeleton() }) { call ->
            CallDetailsScreen(call, onBack, onNavigate, action, viewModel::change, viewModel::load, state as? UiState.Content)
        }
        TransientFeedback(action.message, viewModel::clearMessage, Modifier.align(Alignment.BottomCenter).padding(Spacing.md))
    }
}

@Composable
fun CallDetailsScreen(
    call: ServiceCall, onBack: () -> Unit, onNavigate: (AppDestination) -> Unit = {},
    action: ServiceActionState = ServiceActionState(), onChange: (ServiceDecision, OffsetDateTime?) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {}, resource: UiState.Content<*>? = null,
) {
    var confirmation by rememberSaveable { mutableStateOf<ServiceDecision?>(null) }
    var schedule by rememberSaveable { mutableStateOf<String?>(null) }
    val busy = action.busy || resource?.refreshing == true
    Column(Modifier.fillMaxSize().appBackground().verticalScroll(rememberScrollState()).padding(Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ScreenTitle("Serviço técnico", onBack, actions = { RefreshButton(onRefresh, resource?.refreshing == true) })
        resource?.let { ResourceStatus(it) }
        call.badge?.let { StatusBadge(it, tint = if (call.status == "COMPLETED") AppTheme.colors.success else AppTheme.colors.action) }
        Text(call.title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.headlineSmall)
        if (call.projectName.isNotBlank()) Text(call.projectName, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyLarge)
        Text(call.scheduledAt?.let(::serviceDate) ?: "Sem agendamento definido", color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        call.acceptedAt?.let { Text("Aceito em ${serviceDate(it)}", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted) }
        call.endedAt?.let { Text("Concluído em ${serviceDate(it)}", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted) }
        if (call.address.isNotBlank()) Text(call.address, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
        call.projectId?.let { project ->
            ActionRow("Abrir projeto", AppIcons.folder, { onNavigate(AppDestination.Project(project)) }, enabled = !busy)
            ActionRow("Ver local no mapa", AppIcons.map_pin, { onNavigate(AppDestination.Map(project)) }, enabled = !busy)
        }
        if (call.managedByAgenda) StateNotice("Gerenciado pela agenda", "O agendamento e a situação deste serviço são controlados pela atividade do projeto.", icon = AppIcons.calendar_days)
        action.error?.let { StateNotice("Não foi possível concluir", it.userMessage(), icon = AppIcons.info) }
        if (call.canManage && !call.managedByAgenda && call.status in setOf("OPEN", "IN_PROGRESS")) {
            DateTimeAction("Reagendar serviço", call.scheduledAt, enabled = !busy) { schedule = it.toString(); confirmation = ServiceDecision.SCHEDULE }
            if (call.status == "OPEN") {
                if (call.hasExecutors) PrimaryButton("Aceitar serviço", { confirmation = ServiceDecision.ACCEPT }, enabled = !busy)
                else StateNotice("Executores ainda não definidos", "Defina os executores do serviço antes do aceite.", icon = AppIcons.users)
            }
            if (call.status == "IN_PROGRESS") PrimaryButton("Concluir serviço", { confirmation = ServiceDecision.COMPLETE }, enabled = !busy)
            PrimaryButton("Cancelar serviço", { confirmation = ServiceDecision.CANCEL }, enabled = !busy, secondary = true)
        }
        if (action.busy) Text("Salvando alteração…", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
    }
    confirmation?.let { decision ->
        val title = when (decision) {
            ServiceDecision.ACCEPT -> "Aceitar serviço?"
            ServiceDecision.COMPLETE -> "Concluir serviço?"
            ServiceDecision.CANCEL -> "Cancelar serviço?"
            ServiceDecision.SCHEDULE -> "Confirmar agendamento?"
        }
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(title) }, text = {
            Text(if (decision == ServiceDecision.SCHEDULE) schedule?.let { serviceDate(OffsetDateTime.parse(it)) }.orEmpty()
                else "A alteração será salva no serviço técnico deste projeto.")
        }, confirmButton = { PrimaryButton("Confirmar", {
            onChange(decision, if (decision == ServiceDecision.SCHEDULE) schedule?.let(OffsetDateTime::parse) else null)
            confirmation = null
        }, enabled = !busy, compact = true) }, dismissButton = { PrimaryButton("Voltar", { confirmation = null }, secondary = true, compact = true) })
    }
}

private fun serviceDate(value: OffsetDateTime): String = value.atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm"))
