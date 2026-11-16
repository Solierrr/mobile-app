@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.images.readPickedImage
import com.project.solaria_mobile.feature.profile.domain.ProfilePhotoType
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
    LaunchedEffect(viewModel) { viewModel.openChat.collect { onNavigate(AppDestination.Chat(it)) } }
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Perfil", onBack) }, loading = { MediaPageSkeleton(280.dp, Modifier.background(AppTheme.colors.surface)) }, loadingHeader = false) { content ->
        ProfileScreen(content, onBack, onNavigate, viewModel::toggleConnection, images::bytes, onStartChat = viewModel::startChat) { ProfileEditControls(content.profile.avatarUrl != null, viewModel) }
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onToggleConnection: () -> Unit,
    loadImage: (suspend (String) -> ByteArray?)? = null,
    onStartChat: () -> Unit = {},
    editControls: @Composable () -> Unit = {},
) {
    val self = state.self
    val profile = state.profile
    var infoOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(280.dp).background(AppTheme.colors.inputFill)) {
            if (profile.bannerUrl != null && loadImage != null) {
                RemoteImage(profile.bannerUrl, loadImage, null, Modifier.fillMaxSize())
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(AppIcons.chevron_left, "Voltar", onBack)
                RoundIconButton(
                    AppIcons.info,
                    "Ver informações",
                    onClick = { infoOpen = true },
                )
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
            if (profile.state.isNotBlank()) ProfileField("Estado", profile.state)
            if (profile.occupation.isNotBlank()) ProfileField("Profissão", profile.occupation)
            if (profile.phone.isNotBlank()) ProfileField("Telefone", profile.phone)
            state.actionError?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) }
            SectionHeading("Opções", faded = true)
            if (self) {
                editControls()
                ActionRow("Informações do perfil", AppIcons.info, { infoOpen = true })
                ActionRow("Privacidade", AppIcons.shield, { onNavigate(AppDestination.Settings) })
                ActionRow("Configurações", AppIcons.settings, { onNavigate(AppDestination.Settings) })
            } else {
                ActionRow("Informações", AppIcons.info, { infoOpen = true })
                if (profile.authId != null) ActionRow("Conversar", AppIcons.message_circle, onStartChat)
                state.chatFailure?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) }
                ActionRow(if (state.connecting) "Atualizando conexão…" else if (state.connected) "Remover conexão" else "Adicionar conexão", AppIcons.link, { if (!state.connecting) onToggleConnection() })
            }
        }
    }

    if (infoOpen) {
        BottomSheet(onDismiss = { infoOpen = false }, state = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(profile.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text("Perfil público", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                if (profile.state.isNotBlank()) ProfileField("Estado", profile.state)
                if (profile.occupation.isNotBlank()) ProfileField("Profissão", profile.occupation)
                if (profile.phone.isNotBlank()) ProfileField("Telefone", profile.phone)
            }
        }
    }
}

/** Edições do perfil próprio aceitas pelo api-core: foto, capa e nome de usuário. */
@Composable
private fun ProfileEditControls(hasPhoto: Boolean, viewModel: ProfileViewModel) {
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

    ActionRow("Alterar foto", AppIcons.images, { viewModel.clearEdit(); photoPicker.launch(imageOnly) })
    ActionRow("Alterar capa", AppIcons.images, { viewModel.clearEdit(); bannerPicker.launch(imageOnly) })
    if (hasPhoto) {
        if (confirmRemoval) {
            Text("Remover a foto do perfil?", color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            PrimaryButton("Remover foto", { confirmRemoval = false; viewModel.removePhoto() }, enabled = edit != ProfileEdit.Working)
            PrimaryButton("Cancelar", { confirmRemoval = false }, secondary = true)
        } else {
            ActionRow("Remover foto", AppIcons.images, { viewModel.clearEdit(); confirmRemoval = true })
        }
    }
    ActionRow("Alterar nome de usuário", AppIcons.at_sign, { viewModel.clearEdit(); usernameOpen = true })
    when (val current = edit) {
        ProfileEdit.Working -> Text("Salvando…", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        is ProfileEdit.Failed -> if (!usernameOpen) Text(current.message(), color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        is ProfileEdit.Done -> Text("Perfil atualizado.", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        null -> Unit
    }
    if (usernameOpen) UsernameDialog(edit, viewModel) { usernameOpen = false }
}

@Composable
private fun UsernameDialog(edit: ProfileEdit?, viewModel: ProfileViewModel, onClose: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(edit) { if ((edit as? ProfileEdit.Done)?.operation == ProfileOperation.USERNAME) onClose() }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Nome de usuário") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FormField(label = "Novo nome de usuário", value = username, onValueChange = { viewModel.clearEdit(); username = it.lowercase() }, maxLength = 30,
                    error = (edit as? ProfileEdit.Failed)?.takeIf { it.operation == ProfileOperation.USERNAME }?.message())
                Text("De 3 a 30 caracteres: letras minúsculas, números ou sublinhado.", color = AppTheme.colors.onSurfaceMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton({ viewModel.changeUsername(username) }, enabled = edit != ProfileEdit.Working) { Text("Salvar") } },
        dismissButton = { TextButton(onClose) { Text("Cancelar") } },
    )
}
