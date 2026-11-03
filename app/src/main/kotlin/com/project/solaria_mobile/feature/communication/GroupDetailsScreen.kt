package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AppDialog
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.ListRow
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.core.format.initialsOf

@Composable
fun GroupDetailsRoute(onBack: () -> Unit, onLeft: () -> Unit, viewModel: GroupDetailsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.gp_detalhes), onBack) }, loading = { ListSkeleton() }) { content ->
        LaunchedEffect(content.left) { if (content.left) onLeft() }
        GroupDetailsScreen(content, onBack, viewModel)
    }
}

@Composable
private fun GroupDetailsScreen(ui: GroupDetailsUi, onBack: () -> Unit, viewModel: GroupDetailsViewModel, images: com.project.solaria_mobile.core.images.ImageLoaderViewModel = hiltViewModel()) {
    var picking by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<GroupMember?>(null) }
    var removing by remember { mutableStateOf<GroupMember?>(null) }
    var transferring by remember { mutableStateOf<GroupMember?>(null) }
    var leaving by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    val unnamed = stringResource(R.string.gp_participante)
    val youLabel = stringResource(R.string.gp_voce)
    val managerLabel = stringResource(R.string.gp_gestor)
    val failure = ui.failure
    // O servidor pode pedir o sucessor mesmo quando o app achou que o grupo estava só com o gestor.
    LaunchedEffect(failure?.errorCode) { if (failure?.errorCode == "SUCCESSOR_REQUIRED" && ui.successors.isNotEmpty()) choosing = true }
    fun GroupMember.label() = name ?: unnamed

    Column(Modifier.fillMaxSize().appBackground().padding(horizontal = Spacing.screen).padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ScreenTitle(ui.conversation.name, onBack)
        Text(stringResource(R.string.gp_n_participantes, ui.members.size), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        failure?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
        if (ui.needsVerification) com.project.solaria_mobile.core.designsystem.components.PrimaryButton(stringResource(R.string.ch_conferir_estado), viewModel::load, secondary = true)
        if (ui.rejected > 0) Text(stringResource(R.string.gp_rejeitados, ui.rejected), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        if (ui.rejectedDetails.isNotEmpty()) Text(ui.rejectedDetails.description(androidx.compose.ui.platform.LocalContext.current), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        if (ui.canManage) ActionRow(stringResource(R.string.gp_adicionar), AppIcons.user_round_plus, { picking = true }, enabled = !ui.busy && !ui.needsVerification)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs), contentPadding = PaddingValues(bottom = Spacing.md)) {
            items(ui.members, key = { it.authId }) { member ->
                ListRow(
                    title = member.label(),
                    subtitle = when { member.manager -> managerLabel; member.self -> youLabel; else -> null },
                    // O gestor toca em outra pessoa para tirá-la ou passar a gestão; ninguém age sobre si por aqui (sair é o botão abaixo).
                    onClick = { if (ui.canManage && !member.self) selected = member },
                    enabled = ui.canManage && !member.self && !ui.busy && !ui.needsVerification,
                    leading = { Avatar(initialsOf(member.name.orEmpty()), imageUrl = member.avatar, loadImage = images::bytes, placeholderIcon = AppIcons.user_silhouette) },
                )
            }
        }
        ActionRow(stringResource(R.string.gp_sair), AppIcons.log_out, { if (ui.canManage && ui.successors.isNotEmpty()) choosing = true else leaving = true }, enabled = !ui.busy && !ui.needsVerification)
    }

    if (picking) PeoplePickerSheet(
        title = stringResource(R.string.gp_adicionar), confirmLabel = stringResource(R.string.gp_adicionar_confirmar),
        exclude = ui.members.map { it.authId }.toSet(), onConfirm = { ids, _ -> viewModel.add(ids) }, onDismiss = { picking = false },
    )
    selected?.let { member ->
        BottomSheet(onDismiss = { selected = null }) {
            val dismiss = LocalSheetDismiss.current
            Column(Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(member.label(), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
                ActionRow(stringResource(R.string.gp_tornar_gestor), AppIcons.user_round_check, { dismiss(); transferring = member }, compact = true)
                ActionRow(stringResource(R.string.gp_remover), AppIcons.trash, { dismiss(); removing = member }, compact = true)
            }
        }
    }
    removing?.let { member ->
        AppDialog(
            onDismissRequest = { removing = null }, title = stringResource(R.string.gp_remover_pergunta, member.label()),
            confirmLabel = stringResource(R.string.gp_remover), dismissLabel = stringResource(R.string.ch_cancelar), destructive = true,
            onConfirm = { removing = null; viewModel.remove(member.authId) },
        )
    }
    transferring?.let { member ->
        AppDialog(
            onDismissRequest = { transferring = null }, title = stringResource(R.string.gp_transferir_pergunta, member.label()),
            message = stringResource(R.string.gp_transferir_desc), confirmLabel = stringResource(R.string.gp_tornar_gestor), dismissLabel = stringResource(R.string.ch_cancelar),
            onConfirm = { transferring = null; viewModel.transfer(member.authId) },
        )
    }
    if (leaving) {
        AppDialog(
            onDismissRequest = { leaving = false }, title = stringResource(R.string.gp_sair_pergunta, ui.conversation.name),
            message = stringResource(R.string.gp_sair_desc), confirmLabel = stringResource(R.string.gp_sair), dismissLabel = stringResource(R.string.ch_cancelar), destructive = true,
            onConfirm = { leaving = false; viewModel.leave() },
        )
    }
    if (choosing) {
        BottomSheet(onDismiss = { choosing = false }) {
            val dismiss = LocalSheetDismiss.current
            Column(Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.gp_escolha_sucessor), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.gp_escolha_sucessor_desc), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                ui.successors.forEach { member ->
                    ActionRow(member.label(), AppIcons.user_silhouette, {
                        dismiss()
                        viewModel.leave(member.authId)
                    }, compact = true)
                }
            }
        }
    }
}
