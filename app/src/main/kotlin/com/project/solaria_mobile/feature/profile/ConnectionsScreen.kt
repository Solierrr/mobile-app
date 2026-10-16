@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.feature.profile.data.Connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AvatarPalette
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SolariaAvatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius

@Composable
fun ConnectionsRoute(
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectionsScreen(state, viewModel::onQueryChange, onBack, onOpenProfile)
}

@Composable
fun ConnectionsScreen(
    state: ConnectionsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    var filterOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.lg)) {
        ScreenTitle("Conexões", onBack, trailingIcon = LucideR.drawable.lucide_ic_ellipsis, trailingLabel = "Opções", onTrailingClick = { filterOpen = true })
        Row(modifier = Modifier.padding(top = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SearchField("Busque um usuário", state.query, onQueryChange, modifier = Modifier.weight(1f), trailingIcon = LucideR.drawable.lucide_ic_sliders_horizontal, onTrailingClick = { filterOpen = true })
        }
        LazyColumn(modifier = Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(state.connections) { user ->
                ConnectionRow(user, onClick = onOpenProfile)
            }
        }
    }

    if (filterOpen) {
        ModalBottomSheet(
            onDismissRequest = { filterOpen = false }, sheetState = sheetState, containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet), dragHandle = null,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ScreenTitle("Conexões", onBack = { filterOpen = false })
                ActionRow("Aguardando aprovação", LucideR.drawable.lucide_ic_user_round_check, { filterOpen = false })
                ActionRow("Convidar pessoa", LucideR.drawable.lucide_ic_user_round_plus, { filterOpen = false })
                ActionRow("Conexões por projeto", LucideR.drawable.lucide_ic_folder, { filterOpen = false })
            }
        }
    }
}

@Composable
private fun ConnectionRow(connection: Connection, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = AppTheme.colors.surface) {
        Row(modifier = Modifier.padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            SolariaAvatar(connection.initials, color = AvatarPalette(connection.name.length))
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(connection.handle, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
            Icon(painter = painterResource(LucideR.drawable.lucide_ic_ellipsis), contentDescription = "Opções de ${connection.name}", tint = AppTheme.colors.onSurfaceMuted, modifier = Modifier.size(Sizing.icon))
        }
    }
}
