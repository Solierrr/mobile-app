package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.presentation.navigation.AppDestination

@Composable
fun ProjectCommunitySection(onNavigate: (AppDestination) -> Unit, viewModel: ProjectCommunityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var allMembers by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(viewModel) { viewModel.load() }
    UiStateHost(state, viewModel::load, inline = true, loading = { ListSkeleton(rows = 3) }) { content ->
        LaunchedEffect(content.notice) { if (content.notice != null) { showForm = false; title = ""; selected = emptyList(); allMembers = true } }
        val community = content.community
        val busy = content.busy || (state as? UiState.Content)?.refreshing == true
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            RefreshButton(viewModel::load, (state as? UiState.Content)?.refreshing == true, "Atualizar conversas do projeto")
            (state as? UiState.Content)?.let { ResourceStatus(it) }
            content.error?.let { error -> Text(error.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
            content.notice?.let { Text(it, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium) }
            if (community.id == null) {
                StateNotice("As conversas começam aqui", "A comunidade reúne os integrantes deste projeto e seus grupos.", icon = AppIcons.messages_square)
                if (community.canCreate) PrimaryButton("Ativar comunidade do projeto", viewModel::create, enabled = !busy)
                else Text("O responsável pelo projeto pode ativar a comunidade.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
            } else {
                Text("${community.members} integrantes", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                if (!community.active) StateNotice("Comunidade em consulta", "O projeto ou a comunidade está encerrado. As conversas disponíveis continuam abaixo.", icon = AppIcons.messages_square)
                if (community.groups.isEmpty()) StateNotice("Nenhuma conversa ainda", "Crie a conversa Geral para reunir a comunidade ou um grupo para organizar o trabalho.", icon = AppIcons.messages_square)
                community.groups.forEach { group ->
                    if (group.canOpen) ActionRow(group.title, AppIcons.messages_square, { onNavigate(AppDestination.Chat(group.id)) },
                        subtitle = "${group.participants} integrantes${if (group.active) "" else " · Inativa"}")
                    else Column(Modifier.padding(vertical = Spacing.xs)) {
                        Text(group.title, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
                        Text("Você não participa deste grupo", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                    }
                }
                if (community.active && community.members >= 2) {
                    if (showForm) {
                        FormField("Nome do grupo", title, { title = it }, maxLength = 120, enabled = !busy)
                        CommunityOption("Todos os integrantes", allMembers, !busy) { allMembers = it; selected = emptyList() }
                        if (!allMembers) community.people.filter { it.joined && !it.self }.forEach { person ->
                            CommunityOption(person.name, person.authId in selected, !busy) { checked ->
                                selected = if (checked) selected + person.authId else selected - person.authId
                            }
                        }
                        Text("Você participa do grupo automaticamente.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                        PrimaryButton("Criar grupo", { viewModel.createGroup(title, if (allMembers) null else selected.toSet()) }, enabled = !busy && title.isNotBlank() && (allMembers || selected.isNotEmpty()))
                        PrimaryButton("Cancelar", { showForm = false; title = "" }, enabled = !busy, secondary = true, compact = true)
                    } else {
                        if (community.groups.isEmpty()) PrimaryButton("Criar conversa Geral", { viewModel.createGroup("Geral") }, enabled = !busy)
                        PrimaryButton("Novo grupo", { viewModel.clearNotice(); showForm = true }, enabled = !busy, secondary = true, compact = true)
                    }
                }
                if (community.canManage && community.active) {
                    val candidates = community.people.filterNot { it.joined }
                    if (candidates.isNotEmpty()) SectionHeading("Integrantes do projeto")
                    candidates.forEach { person -> PrimaryButton("Adicionar ${person.name}", { viewModel.addMember(person.authId) }, enabled = !busy, secondary = true, compact = true) }
                }
                if (community.active && community.members < 2) Text("Adicione outro integrante à comunidade para iniciar uma conversa em grupo.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
            }
        }
    }
}

@Composable
private fun CommunityOption(label: String, selected: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = Sizing.touchTarget).toggleable(selected, enabled = enabled, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Checkbox(selected, onCheckedChange = null, enabled = enabled, colors = CheckboxDefaults.colors(checkedColor = AppTheme.colors.action))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
    }
}
