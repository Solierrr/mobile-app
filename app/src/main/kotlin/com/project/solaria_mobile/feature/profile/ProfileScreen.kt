@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
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
import kotlinx.coroutines.launch
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    images: ImageLoaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Perfil", onBack) }) { content ->
        ProfileScreen(content, onBack, onNavigate, viewModel::toggleConnection, images::bytes)
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onToggleConnection: () -> Unit,
    loadImage: (suspend (String) -> ByteArray?)? = null,
) {
    val self = state.self
    val profile = state.profile
    var infoOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val banner = if (self) R.drawable.profile_waterfall else R.drawable.profile_night_sky

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            Image(painter = painterResource(banner), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (profile.bannerUrl != null && loadImage != null) {
                RemoteImage(profile.bannerUrl, loadImage, null, Modifier.fillMaxSize(), placeholder = false)
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
                Avatar(profile.initials, size = Sizing.avatarLarge, color = AvatarPalette(if (self) 0 else 1), online = !self, imageUrl = profile.avatarUrl, loadImage = loadImage, shape = if (self) ExpressiveShapes.cookie else ExpressiveShapes.clover)
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
                ActionRow("Informações do perfil", AppIcons.info, { infoOpen = true })
                ActionRow("Privacidade", AppIcons.shield, { onNavigate(AppDestination.Settings) })
                ActionRow("Configurações", AppIcons.settings, { onNavigate(AppDestination.Settings) })
            } else {
                ActionRow("Informações", AppIcons.info, { infoOpen = true })
                ActionRow("Ver agenda", AppIcons.calendar_days, { onNavigate(AppDestination.Schedule) })
                ActionRow(if (state.connecting) "Atualizando conexão…" else if (state.connected) "Remover conexão" else "Adicionar conexão", AppIcons.link, { if (!state.connecting) onToggleConnection() })
            }
        }
    }

    if (infoOpen) {
        BottomSheet(onDismiss = { infoOpen = false }, state = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ScreenTitle("Informações", onBack = { scope.launch { sheetState.hide(); infoOpen = false } })
                Text(profile.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text("Perfil público", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                if (profile.state.isNotBlank()) ProfileField("Estado", profile.state)
                if (profile.occupation.isNotBlank()) ProfileField("Profissão", profile.occupation)
                if (profile.phone.isNotBlank()) ProfileField("Telefone", profile.phone)
            }
        }
    }
}
