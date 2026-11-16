@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.feature.profile.domain.Connection

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
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
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
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AvatarPalette
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.common.UiState

@Composable
fun ConnectionsRoute(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Conexões", onBack) }) { content -> ConnectionsScreen(content, viewModel::onQueryChange, onBack, onOpenProfile, state as? UiState.Content, viewModel::load) }
}

@Composable
fun ConnectionsScreen(
    state: ConnectionsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    resource: UiState.Content<*>? = null,
    onRefresh: () -> Unit = {},
) {

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.lg)) {
        ScreenTitle("Conexões", onBack, actions = { RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar conexões") })
        Row(modifier = Modifier.padding(top = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SearchField("Busque um usuário", state.query, onQueryChange, modifier = Modifier.weight(1f))
        }
        if (resource != null) ResourceStatus(resource)
        LazyColumn(modifier = Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (state.connections.isEmpty()) item { StateNotice(
                        if (state.query.isBlank()) "Nenhuma conexão ainda" else "Nenhuma conexão encontrada",
                        if (state.query.isBlank()) "Suas conexões aparecerão aqui quando estiverem disponíveis." else "Tente buscar por outro nome ou limpe a busca.",
                        modifier = Modifier.padding(top = Spacing.md), icon = AppIcons.users,
                        actions = if (state.query.isNotBlank()) { { PrimaryButton("Limpar busca", { onQueryChange("") }, secondary = true, compact = true) } } else null,
                    ) }
            items(state.connections) { user ->
                ConnectionRow(user, onClick = { onOpenProfile(user.id) })
            }
        }
    }


}

@Composable
private fun ConnectionRow(connection: Connection, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = AppTheme.colors.surface) {
        Row(modifier = Modifier.padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Avatar(connection.initials, color = AvatarPalette(connection.name.length))
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                Text(connection.handle, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }

        }
    }
}
