package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.core.designsystem.components.Appear
import com.project.solaria_mobile.core.designsystem.components.AppCheckboxRow
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
            (state as? UiState.Content)?.let { ResourceStatus(it) }
            content.error?.let { error -> Text(error.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
            content.notice?.let { notice -> Text(if (notice.rejected > 0) stringResource(R.string.pc_grupo_criado_rejeitados, notice.rejected) else stringResource(R.string.pc_grupo_criado), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium) }
            if (community.id == null) {
                // Quem não pode ativar a comunidade lê isso na própria descrição, em vez de num aviso solto abaixo.
                StateNotice(
                    stringResource(R.string.pc_as_conversas_comecam_aqui),
                    stringResource(if (community.canCreate) R.string.pc_a_comunidade_reune_os_integrantes else R.string.pc_a_comunidade_reune_e_responsavel_ativa),
                    icon = AppIcons.messages_square,
                )
                if (community.canCreate) PrimaryButton(stringResource(R.string.pc_ativar_comunidade_do_projeto), viewModel::create, enabled = !busy)
            } else {
                Text("${community.members} integrantes", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                if (!community.active) StateNotice(stringResource(R.string.pc_comunidade_em_consulta), stringResource(R.string.pc_o_projeto_ou_a_comunidade), icon = AppIcons.messages_square)
                if (community.groups.isEmpty()) StateNotice(stringResource(R.string.pc_nenhuma_conversa_ainda), stringResource(R.string.pc_crie_a_conversa_geral_para), icon = AppIcons.messages_square)
                community.groups.forEach { group ->
                    if (group.canOpen) ActionRow(group.title, AppIcons.messages_square, { onNavigate(AppDestination.Chat(group.id)) },
                        subtitle = stringResource(R.string.pc_n_integrantes, group.participants) + if (group.active) "" else stringResource(R.string.pc_inativa))
                    else Column(Modifier.padding(vertical = Spacing.xs)) {
                        Text(group.title, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
                        Text(stringResource(R.string.pc_voce_nao_participa_deste_grupo), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                    }
                }
                if (community.active && community.members >= 2) {
                    if (showForm) {
                        Appear { FormField(stringResource(R.string.pc_nome_do_grupo), title, { title = it }, maxLength = 120, enabled = !busy) }
                        CommunityOption(stringResource(R.string.pc_todos_os_integrantes), allMembers, !busy) { allMembers = it; selected = emptyList() }
                        if (!allMembers) community.people.filter { it.joined && !it.self }.forEach { person ->
                            CommunityOption(person.name, person.authId in selected, !busy) { checked ->
                                selected = if (checked) selected + person.authId else selected - person.authId
                            }
                        }
                        Text(stringResource(R.string.pc_voce_participa_do_grupo_automaticamente), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                        PrimaryButton(stringResource(R.string.pc_criar_grupo), { viewModel.createGroup(title, if (allMembers) null else selected.toSet()) }, enabled = !busy && title.isNotBlank() && (allMembers || selected.isNotEmpty()))
                        PrimaryButton(stringResource(R.string.ch_cancelar), { showForm = false; title = "" }, enabled = !busy, secondary = true, compact = true)
                    } else {
                        if (community.groups.isEmpty()) PrimaryButton(stringResource(R.string.pc_criar_conversa_geral), { viewModel.createGroup("Geral") }, enabled = !busy)
                        PrimaryButton(stringResource(R.string.pc_novo_grupo), { viewModel.clearNotice(); showForm = true }, enabled = !busy, secondary = true, compact = true)
                    }
                }
                if (community.canManage && community.active) {
                    val candidates = community.people.filterNot { it.joined }
                    if (candidates.isNotEmpty()) SectionHeading(stringResource(R.string.pc_integrantes_do_projeto))
                    candidates.forEach { person -> PrimaryButton("Adicionar ${person.name}", { viewModel.addMember(person.authId) }, enabled = !busy, secondary = true, compact = true) }
                }
                if (community.active && community.members < 2) Text(stringResource(R.string.pc_adicione_outro_integrante_a_comunidade), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
            }
        }
    }
}

@Composable
private fun CommunityOption(label: String, selected: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) =
    AppCheckboxRow(label, selected, onChange, enabled = enabled)
