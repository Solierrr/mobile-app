@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.home

import com.project.solaria_mobile.core.designsystem.theme.Elevation
import androidx.compose.ui.draw.clip
import com.project.solaria_mobile.feature.project.RecentProjectItem
import com.project.solaria_mobile.core.designsystem.components.ListItemDivider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.images.ImageLoaderViewModel
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.feature.communication.CallsBottomSheet
import com.project.solaria_mobile.presentation.navigation.AppDestination
import com.project.solaria_mobile.core.designsystem.components.UiStateHost

@Composable
fun HomeRoute(
    onOpenPage: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    images: ImageLoaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, modifier) { content ->
        HomeScreen(content, viewModel::onCallsQueryChange, onOpenPage, modifier, images::bytes)
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onCallsQueryChange: (String) -> Unit,
    onOpenPage: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    loadImage: (suspend (String) -> ByteArray?)? = null,
) {
    var callsOpen by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen).padding(top = Spacing.screenTop, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Avatar(initials = state.profile?.initials.orEmpty(), modifier = Modifier.clickable { onOpenPage(AppDestination.ProfileSelf) }, color = AppTheme.colors.inputFill, textColor = AppTheme.colors.onSurface, imageUrl = state.profile?.avatarUrl, loadImage = loadImage)
                    Column {
                        Text("Olá,", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                        Text(state.profile?.name?.substringBefore(" ").orEmpty().ifBlank { "Sua conta" }, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
                    }
                }
                RoundIconButton(AppIcons.bell, "Notificações", { onOpenPage(AppDestination.Notifications) }, background = AppTheme.colors.inputFill)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeading("Projetos recentes", action = "Ver todos", navigation = true, onAction = { onOpenPage(AppDestination.Projects) })
                if (state.recentProjects.isEmpty()) Text("Seus projetos aparecerão aqui.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                else Column(Modifier.clip(RoundedCornerShape(Radius.large)).background(AppTheme.colors.inputFill)) {
                    state.recentProjects.take(3).forEachIndexed { index, project ->
                        if (index > 0) ListItemDivider(Modifier.padding(horizontal = Spacing.md))
                        RecentProjectItem(project, { onOpenPage(AppDestination.Project(project.id)) })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeading("Seu espaço")
                ActionRow("Agenda", AppIcons.calendar_days, { onOpenPage(AppDestination.Schedule) })
                ActionRow("Chamados", AppIcons.phone, { callsOpen = true })
                ActionRow("Conexões", AppIcons.users, { onOpenPage(AppDestination.Connections) })
                state.profile?.companyId?.let { id -> ActionRow("Sua empresa", AppIcons.building, { onOpenPage(AppDestination.Enterprise(id)) }) }
                ActionRow("Novo projeto", AppIcons.folder_plus, { onOpenPage(AppDestination.NewProject) })
                ActionRow("Configurações", AppIcons.settings, { onOpenPage(AppDestination.Settings) })
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = Spacing.screen, vertical = Spacing.md),
            shape = RoundedCornerShape(Radius.pill), color = AppTheme.colors.surface.copy(alpha = 0.98f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.outline), shadowElevation = Elevation.floating,
        ) {
            Row(Modifier.fillMaxWidth().padding(Spacing.xs), horizontalArrangement = Arrangement.SpaceEvenly) {
                HomeNavItem("Início", AppIcons.home, true, {})
                HomeNavItem("Projetos", AppIcons.folder, false, { onOpenPage(AppDestination.Projects) })
                HomeNavItem("Conversas", AppIcons.message_circle, false, { onOpenPage(AppDestination.Chats) })
            }
        }
    }

    if (callsOpen) {
        CallsBottomSheet(
            calls = state.recentCalls,
            query = state.callsQuery,
            failure = state.callsFailure,
            onQueryChange = onCallsQueryChange,
            onDismiss = { callsOpen = false },
            onOpenCall = { id -> callsOpen = false; onOpenPage(AppDestination.Call(id)) },
            onOpenAll = { callsOpen = false; onOpenPage(AppDestination.Calls) },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HomeNavItem(label: String, icon: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(Radius.pill), color = if (selected) AppTheme.colors.inputFill else androidx.compose.ui.graphics.Color.Transparent) {
        Column(Modifier.padding(vertical = Spacing.xs), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(icon), null, Modifier.size(Sizing.icon), tint = if (selected) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceMuted)
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceMuted)
        }
    }
}
