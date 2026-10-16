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
import androidx.compose.material3.ModalBottomSheet
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
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AvatarPalette
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.presentation.navigation.AppDestination
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(
    self: Boolean,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by (if (self) viewModel.ownState else viewModel.otherState).collectAsStateWithLifecycle()
    ProfileScreen(state, onBack, onNavigate, viewModel::toggleConnection)
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onToggleConnection: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(LucideR.drawable.lucide_ic_chevron_left, "Voltar", onBack)
                RoundIconButton(
                    if (self) LucideR.drawable.lucide_ic_pencil else LucideR.drawable.lucide_ic_share,
                    if (self) "Editar perfil" else "Compartilhar perfil",
                    onClick = { if (self) infoOpen = true },
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().offset(y = -Spacing.xxl).clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)).background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.xl, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile.initials, size = 52.dp, color = AvatarPalette(if (self) 0 else 1), online = !self)
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(profile.handle, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            ProfileField("Estado", profile.state)
            ProfileField("Profissão", profile.occupation)
            ProfileField("Telefone", profile.phone)
            SectionHeading("Opções", faded = true)
            if (self) {
                ActionRow("Editar informações", LucideR.drawable.lucide_ic_info, { infoOpen = true })
                ActionRow("Privacidade", LucideR.drawable.lucide_ic_shield, { onNavigate(AppDestination.Settings) })
                ActionRow("Compartilhar", LucideR.drawable.lucide_ic_share, {})
                ActionRow("Configurações", LucideR.drawable.lucide_ic_settings, { onNavigate(AppDestination.Settings) })
            } else {
                ActionRow("Informações", LucideR.drawable.lucide_ic_info, { infoOpen = true })
                ActionRow("Ver agenda", LucideR.drawable.lucide_ic_calendar_days, { onNavigate(AppDestination.Schedule) })
                ActionRow(if (state.connected) "Conectado" else "Adicionar conexão", LucideR.drawable.lucide_ic_link, onToggleConnection)
                ActionRow("Conversar", LucideR.drawable.lucide_ic_message_circle, { onNavigate(AppDestination.Chat) })
                ActionRow("Compartilhar", LucideR.drawable.lucide_ic_share, {})
            }
        }
    }

    if (infoOpen) {
        ModalBottomSheet(
            onDismissRequest = { infoOpen = false },
            sheetState = sheetState,
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
            dragHandle = null,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ScreenTitle("Informações", onBack = { scope.launch { sheetState.hide(); infoOpen = false } })
                Text(profile.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text("Perfil público", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                ProfileField("Estado", profile.state)
                ProfileField("Profissão", profile.occupation)
                ProfileField("Telefone", profile.phone)
                ActionRow("Compartilhar perfil", LucideR.drawable.lucide_ic_share, {})
            }
        }
    }
}
