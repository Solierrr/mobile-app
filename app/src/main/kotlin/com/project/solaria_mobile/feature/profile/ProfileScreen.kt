@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.core.designsystem.components.AppDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import androidx.compose.material3.Text
import com.project.solaria_mobile.core.designsystem.components.rememberSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AvatarPalette
import com.project.solaria_mobile.core.designsystem.components.ExpressiveShapes
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.components.RemoteImage
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.images.ImageLoaderViewModel
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.presentation.navigation.AppDestination
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.components.MediaPageSkeleton
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.images.readPickedImage
import com.project.solaria_mobile.feature.profile.domain.ProfilePhotoType
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import com.project.solaria_mobile.feature.profile.domain.ProfileStats
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    images: ImageLoaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.pf_perfil), onBack) }, loading = { MediaPageSkeleton(Sizing.profileBannerHeight, Modifier.background(AppTheme.colors.surface)) }, loadingHeader = false) { content ->
        ProfileScreen(content, onBack, onNavigate, viewModel::toggleConnection, images::bytes, viewModel::clearEdit) { close -> ProfileEditControls(content.profile.avatarUrl != null, viewModel, close) }
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onToggleConnection: () -> Unit,
    loadImage: (suspend (String) -> ByteArray?)? = null,
    onEditOpen: () -> Unit = {},
    editControls: @Composable (onClose: () -> Unit) -> Unit = {},
) {
    val self = state.self
    val profile = state.profile
    var infoOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    val sheetState = rememberSheetState()
    val editSheetState = rememberSheetState()

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).verticalScroll(rememberScrollState())) {
        var bannerFailed by remember { mutableStateOf(false) }
        val noBanner = profile.bannerUrl == null || loadImage == null || bannerFailed
        // Sem imagem, o botão padrão tem a mesma cor do fundo e some; por isso fica sólido.
        val bannerButton = if (noBanner) AppTheme.colors.surface else AppTheme.colors.inputFill
        Box(modifier = Modifier.fillMaxWidth().height(Sizing.profileBannerHeight).background(AppTheme.colors.inputFill)) {
            if (profile.bannerUrl != null && loadImage != null) {
                RemoteImage(profile.bannerUrl, loadImage, null, Modifier.fillMaxSize(), onMissing = { bannerFailed = it })
            } else MissingImage(Modifier.fillMaxSize(), AppTheme.colors.inputFill)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(AppIcons.chevron_left, stringResource(R.string.common_back), onBack, background = bannerButton)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    if (self) RoundIconButton(AppIcons.pencil, stringResource(R.string.pf_editar_perfil), { onEditOpen(); editOpen = true }, background = bannerButton)
                    RoundIconButton(AppIcons.info, stringResource(R.string.pf_ver_informacoes), onClick = { infoOpen = true }, background = bannerButton)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().offset(y = -Spacing.xxl).clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)).background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.xl, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile.initials, size = Sizing.avatarLarge, color = AvatarPalette(if (self) 0 else 1), imageUrl = profile.avatarUrl, loadImage = loadImage, shape = if (self) ExpressiveShapes.cookie else ExpressiveShapes.clover)
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (profile.handle.isNotBlank()) {
                        Text(profile.handle, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            ProfileInfoFields(profile, state.stats, full = false)
            state.actionError?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) }
            SectionHeading(stringResource(R.string.ps_opcoes), faded = true)
            if (self) {
                ActionRow(stringResource(R.string.pf_informacoes_do_perfil), AppIcons.info, { infoOpen = true })
                ActionRow(stringResource(R.string.pf_privacidade), AppIcons.shield, { onNavigate(AppDestination.Settings) })
                ActionRow(stringResource(R.string.settings_title), AppIcons.settings, { onNavigate(AppDestination.Settings) })
            } else {
                ActionRow(stringResource(R.string.pf_informacoes), AppIcons.info, { infoOpen = true })
                ActionRow(if (state.connecting) stringResource(R.string.pf_atualizando_conexao) else if (state.connected) stringResource(R.string.pf_remover_conexao) else stringResource(R.string.pf_adicionar_conexao), AppIcons.link, { if (!state.connecting) onToggleConnection() })
            }
        }
    }

    if (infoOpen) {
        BottomSheet(onDismiss = { infoOpen = false }, state = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(profile.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.pf_perfil_publico), color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                ProfileInfoFields(profile, state.stats, full = true)
            }
        }
    }

    if (editOpen) {
        BottomSheet(onDismiss = { editOpen = false }, state = editSheetState) {
            val dismiss = LocalSheetDismiss.current
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(stringResource(R.string.pf_editar_perfil), color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                editControls(dismiss)
            }
        }
    }
}

/**
 * As informações do perfil, cada uma só aparece se o valor existir. A página mostra o essencial (profissão, telefone e
 * e-mail); [full] acrescenta vínculo, projetos e conexões, que ficam na sheet do ícone de informação.
 */
@Composable
private fun ProfileInfoFields(profile: UserProfile, stats: ProfileStats?, full: Boolean) {
    if (profile.occupation.isNotBlank()) ProfileField(stringResource(R.string.pf_profissao), profile.occupation)
    if (full) {
        if (profile.affiliation.isNotBlank()) ProfileField(stringResource(R.string.pf_vinculo), profile.affiliation)
        stats?.projects?.let { ProfileField(stringResource(R.string.pl_projetos), projectsLabel(it, stats.activeProjects)) }
        stats?.connections?.let { ProfileField(stringResource(R.string.pf_conexoes), peopleLabel(it)) }
    }
    if (profile.phone.isNotBlank()) ProfileField(stringResource(R.string.pf_telefone), profile.phone)
    if (profile.email.isNotBlank()) ProfileField(stringResource(R.string.ps_e_mail), profile.email)
}

internal fun projectsLabel(total: Int, active: Int?, resources: android.content.res.Resources): String {
    if (total == 0) return resources.getString(R.string.pf_nenhum)
    val base = if (total == 1) resources.getString(R.string.pf_1_projeto) else resources.getString(R.string.pf_n_projetos, total)
    return if (active != null && active in 1..total) "$base · " + resources.getString(if (active == 1) R.string.pf_n_ativo_um else R.string.pf_n_ativo_varios, active) else base
}

internal fun peopleLabel(count: Int, resources: android.content.res.Resources): String = when (count) {
    0 -> resources.getString(R.string.pf_nenhuma)
    1 -> resources.getString(R.string.pf_1_pessoa)
    else -> resources.getString(R.string.pf_n_pessoas, count)
}

@androidx.compose.runtime.Composable
internal fun projectsLabel(total: Int, active: Int?): String = projectsLabel(total, active, androidx.compose.ui.platform.LocalContext.current.resources)

@androidx.compose.runtime.Composable
internal fun peopleLabel(count: Int): String = peopleLabel(count, androidx.compose.ui.platform.LocalContext.current.resources)

/** Edições do perfil próprio aceitas pelo api-core: foto, capa e nome de usuário. */
@Composable
private fun ProfileEditControls(hasPhoto: Boolean, viewModel: ProfileViewModel, onClose: () -> Unit) {
    val edit by viewModel.edit.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmRemoval by remember { mutableStateOf(false) }
    var usernameOpen by remember { mutableStateOf(false) }
    fun pickerFor(type: ProfilePhotoType, operation: ProfileOperation) = { uri: Uri? ->
        if (uri != null) scope.launch {
            val picked = withContext(Dispatchers.IO) { readPickedImage(context, uri) }
            if (picked == null) viewModel.rejectImage(operation) else viewModel.uploadPhoto(type, picked.first, picked.second)
        }
        Unit
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), pickerFor(ProfilePhotoType.PROFILE, ProfileOperation.PHOTO))
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), pickerFor(ProfilePhotoType.BANNER, ProfileOperation.BANNER))
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    ActionRow(stringResource(R.string.pf_alterar_foto), AppIcons.camera, { viewModel.clearEdit(); photoPicker.launch(imageOnly) })
    ActionRow(stringResource(R.string.pf_alterar_capa), AppIcons.cover, { viewModel.clearEdit(); bannerPicker.launch(imageOnly) })
    ActionRow(stringResource(R.string.pf_alterar_nome_de_usuario), AppIcons.at_sign, { viewModel.clearEdit(); usernameOpen = true })
    if (hasPhoto) ActionRow(stringResource(R.string.pf_remover_foto), AppIcons.trash, { viewModel.clearEdit(); confirmRemoval = true })
    when (val current = edit) {
        ProfileEdit.Working -> Text(stringResource(R.string.ps_salvando), color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        is ProfileEdit.Failed -> if (!usernameOpen) Text(current.message(), color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        // Depois de salvar, a sheet se fecha e o perfil recarregado já mostra a mudança.
        is ProfileEdit.Done -> LaunchedEffect(current) { onClose() }
        null -> Unit
    }
    if (confirmRemoval) AppDialog(
        onDismissRequest = { confirmRemoval = false },
        title = stringResource(R.string.pf_remover_a_foto_do_perfil),
        message = stringResource(R.string.pf_voce_volta_a_aparecer_com),
        confirmLabel = stringResource(R.string.ps_remover),
        onConfirm = { confirmRemoval = false; viewModel.removePhoto() },
        destructive = true,
        loading = edit == ProfileEdit.Working,
    )
    if (usernameOpen) UsernameDialog(edit, viewModel) { usernameOpen = false }
}

@Composable
private fun UsernameDialog(edit: ProfileEdit?, viewModel: ProfileViewModel, onClose: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(edit) { if ((edit as? ProfileEdit.Done)?.operation == ProfileOperation.USERNAME) onClose() }
    AppDialog(
        onDismissRequest = onClose,
        title = stringResource(R.string.pf_nome_de_usuario),
        confirmLabel = stringResource(R.string.pf_salvar),
        onConfirm = { viewModel.changeUsername(username) },
        loading = edit == ProfileEdit.Working,
    ) {
        FormField(label = stringResource(R.string.pf_novo_nome_de_usuario), value = username, onValueChange = { viewModel.clearEdit(); username = it.lowercase() }, maxLength = 30,
            error = (edit as? ProfileEdit.Failed)?.takeIf { it.operation == ProfileOperation.USERNAME }?.message())
        Text(stringResource(R.string.pf_de_3_a_30_caracteres), color = AppTheme.colors.onSurfaceMuted,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
}

