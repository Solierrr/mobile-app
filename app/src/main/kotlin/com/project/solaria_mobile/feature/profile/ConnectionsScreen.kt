package com.project.solaria_mobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SegmentedControl
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.images.ImageLoaderViewModel
import com.project.solaria_mobile.feature.profile.domain.Connection

@Composable
fun ConnectionsRoute(onBack: () -> Unit, onOpenProfile: (String) -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel(), images: ImageLoaderViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val discover by viewModel.discover.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { viewModel.load(); onDispose { } }
    val resource = state as? UiState.Content
    Column(Modifier.fillMaxSize().background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ScreenTitle("Conexões", onBack, actions = { RefreshButton(viewModel::load, resource?.refreshing == true, "Atualizar conexões") })
        SegmentedControl(listOf("Minhas conexões", "Encontrar pessoas"), if (discover) 1 else 0,
            { viewModel.selectDiscover(it == 1) }, Modifier.fillMaxWidth())
        SearchField(if (discover) "Busque pessoas" else "Busque nas suas conexões", query, viewModel::onQueryChange)
        resource?.let { ResourceStatus(it) }
        UiStateHost(state, viewModel::load, modifier = Modifier.weight(1f), inline = true) { content ->
            ConnectionsList(content, onOpenProfile, viewModel::more, { viewModel.onQueryChange("") }, images::bytes)
        }
    }
}

@Composable
private fun ConnectionsList(state: ConnectionsUiState, onOpenProfile: (String) -> Unit, onMore: () -> Unit,
    onClear: () -> Unit, loadImage: suspend (String) -> ByteArray?) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(bottom = Spacing.screenBottom)) {
        if (state.connections.isEmpty()) item {
            StateNotice(if (state.discover) "Nenhuma pessoa encontrada" else "Nenhuma conexão encontrada",
                if (state.query.isNotBlank()) "Tente outro nome ou limpe a busca."
                else if (state.discover) "Não encontramos pessoas disponíveis para sua conta."
                else "Use Encontrar pessoas para adicionar sua primeira conexão.", icon = AppIcons.users,
                actions = if (state.query.isNotBlank()) { { PrimaryButton("Limpar busca", onClear, secondary = true, compact = true) } } else null)
        }
        items(state.connections, key = { it.id }) { user -> ConnectionRow(user, { onOpenProfile(user.id) }, loadImage) }
        if (state.hasMore) item {
            PrimaryButton(if (state.loadingMore) "Carregando…" else "Mostrar mais", onMore, enabled = !state.loadingMore, secondary = true, compact = true)
        }
    }
}

@Composable
private fun ConnectionRow(connection: Connection, onClick: () -> Unit, loadImage: suspend (String) -> ByteArray?) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = AppTheme.colors.surface) {
        Row(Modifier.padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Avatar(connection.initials, imageUrl = connection.avatarUrl, loadImage = loadImage)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(connection.name, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
                if (connection.handle.isNotBlank()) Text(connection.handle, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
