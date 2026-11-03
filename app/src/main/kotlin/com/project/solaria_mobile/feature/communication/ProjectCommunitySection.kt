package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.core.designsystem.components.Appear
import com.project.solaria_mobile.core.designsystem.components.AppCheckboxRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AppDialog
import com.project.solaria_mobile.core.designsystem.components.AppMenu
import com.project.solaria_mobile.core.designsystem.components.AppMenuItem
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.ListRow
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.SelectableChip
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.communication.domain.CommunityPerson
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunity
import com.project.solaria_mobile.feature.communication.domain.canChangeAdmin
import com.project.solaria_mobile.feature.communication.domain.canRemove
import com.project.solaria_mobile.feature.communication.domain.groupsNeedingSuccessor
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
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
import com.project.solaria_mobile.presentation.navigation.AppDestination

@Composable
fun ProjectCommunitySection(onNavigate: (AppDestination) -> Unit, viewModel: ProjectCommunityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var allMembers by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var removing by remember { mutableStateOf<CommunityPerson?>(null) }
    var confirmArchive by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.load() }
    UiStateHost(state, viewModel::load, inline = true, loading = { ListSkeleton(rows = 3) }) { content ->
        LaunchedEffect(content.notice) { if (content.notice != null) { showForm = false; title = ""; selected = emptyList(); allMembers = true } }
        val community = content.community
        val busy = content.busy || content.needsVerification || (state as? UiState.Content)?.refreshing == true
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            (state as? UiState.Content)?.let { ResourceStatus(it) }
            content.error?.let { error -> Text(ApiResult.Failure(error, errorCode = content.errorCode).userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
            if (content.needsVerification) PrimaryButton(stringResource(R.string.ch_conferir_estado), viewModel::load, secondary = true)
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
                val joined = community.people.filter { it.joined }
                if (joined.isNotEmpty()) {
                    SectionHeading(stringResource(R.string.cm_integrantes))
                    joined.forEach { person -> MemberRow(person, community, busy, viewModel::setAdmin) { removing = person } }
                }
                if (community.canManage && community.active) {
                    val candidates = community.people.filterNot { it.joined }
                    if (candidates.isNotEmpty()) SectionHeading(stringResource(R.string.pc_integrantes_do_projeto))
                    candidates.forEach { person -> PrimaryButton("Adicionar ${person.name}", { viewModel.addMember(person.authId) }, enabled = !busy, secondary = true, compact = true) }
                }
                if (community.canManage) {
                    if (community.archived) PrimaryButton(stringResource(R.string.cm_reativar), { viewModel.setArchived(false) }, enabled = !busy, secondary = true, compact = true)
                    else PrimaryButton(stringResource(R.string.cm_arquivar), { confirmArchive = true }, enabled = !busy, secondary = true, compact = true)
                }
                removing?.let { person ->
                    RemoveMemberSheet(person, community, onDismiss = { removing = null }) { successors -> removing = null; viewModel.removeMember(person.authId, successors) }
                }
                if (confirmArchive) AppDialog(
                    onDismissRequest = { confirmArchive = false }, title = stringResource(R.string.cm_arquivar_pergunta), message = stringResource(R.string.cm_arquivar_desc),
                    confirmLabel = stringResource(R.string.cm_arquivar), dismissLabel = stringResource(R.string.ch_cancelar), destructive = true,
                    onConfirm = { confirmArchive = false; viewModel.setArchived(true) },
                )
                if (community.active && community.members < 2) Text(stringResource(R.string.pc_adicione_outro_integrante_a_comunidade), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
            }
        }
    }
}

@Composable
private fun CommunityOption(label: String, selected: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) =
    AppCheckboxRow(label, selected, onChange, enabled = enabled)

/** Um integrante com o papel e, para quem pode agir sobre ele, o menu de conceder/retirar administrador e remover. */
@Composable
private fun MemberRow(person: CommunityPerson, community: ProjectCommunity, busy: Boolean, onAdmin: (String, Boolean) -> Unit, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val canAdmin = !community.archived && community.canChangeAdmin(person)
    val canRemove = !community.archived && community.canRemove(person)
    val role = stringResource(when (person.role) { "OWNER" -> R.string.cm_dono; "ADMIN" -> R.string.cm_admin; else -> R.string.cm_membro })
    ListRow(
        title = person.name, subtitle = if (person.self) role + " · " + stringResource(R.string.gp_voce) else role, onClick = {},
        enabled = false,
        trailing = if (!canAdmin && !canRemove) null else { {
            Box {
                RoundIconButton(AppIcons.ellipsis, stringResource(R.string.cm_opcoes_integrante), { open = true }, enabled = !busy)
                AppMenu(expanded = open, onDismissRequest = { open = false }) {
                    if (canAdmin) {
                        val admin = person.role == "ADMIN"
                        AppMenuItem(stringResource(if (admin) R.string.cm_retirar_admin else R.string.cm_tornar_admin), { open = false; onAdmin(person.authId, !admin) }, icon = AppIcons.user_round_check)
                    }
                    if (canRemove) AppMenuItem(stringResource(if (person.self) R.string.cm_sair else R.string.cm_remover), { open = false; onRemove() }, destructive = true, icon = AppIcons.log_out)
                }
            }
        } },
    )
}

/**
 * Confirmação de saída de um integrante. Se ele gere subgrupos que continuam com gente, cada um precisa de um sucessor entre os
 * participantes que ficam; só então o botão confirma.
 */
@Composable
internal fun RemoveMemberSheet(person: CommunityPerson, community: ProjectCommunity, onDismiss: () -> Unit, onConfirm: (Map<String, String>) -> Unit) {
    val groups = community.groupsNeedingSuccessor(person.authId)
    var chosen by remember { mutableStateOf(emptyMap<String, String>()) }
    val names = community.people.associate { it.authId.lowercase() to it.name }
    BottomSheet(onDismiss = onDismiss) {
        val dismiss = LocalSheetDismiss.current
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                if (person.self) stringResource(R.string.cm_sair_pergunta) else stringResource(R.string.cm_remover_pergunta, person.name),
                color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.cm_remover_desc), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            if (groups.isNotEmpty()) {
                Text(stringResource(R.string.cm_sucessores_desc), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                groups.forEach { group ->
                    Text(group.title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        group.participantIds.filterNot { it.equals(person.authId, ignoreCase = true) }.forEach { candidate ->
                            SelectableChip(names[candidate.lowercase()] ?: stringResource(R.string.gp_participante), chosen[group.id] == candidate, { chosen = chosen + (group.id to candidate) })
                        }
                    }
                }
            }
            PrimaryButton(
                stringResource(if (person.self) R.string.cm_sair else R.string.cm_remover), { dismiss(); onConfirm(chosen) },
                enabled = groups.all { it.id in chosen },
            )
        }
    }
}
