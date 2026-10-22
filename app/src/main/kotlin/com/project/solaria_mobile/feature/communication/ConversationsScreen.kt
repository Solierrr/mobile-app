@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.domain.Conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.common.UiState

@Composable
fun ConversationsRoute(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Conversas", onBack) }) { content ->
        ConversationsScreen(content, viewModel::onQueryChange, onBack, onOpenChat, state as? UiState.Content, viewModel::load)
    }
}

@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    resource: UiState.Content<*>? = null,
    onRefresh: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle("Conversas", onBack, actions = { RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar conversas") })
        SectionHeading("Todas as conversas", faded = true)
        SearchField("Buscar conversa", state.query, onQueryChange)
        if (resource != null) ResourceStatus(resource)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (state.conversations.isEmpty()) item { StateNotice(
                        if (state.query.isBlank()) "Nenhuma conversa ainda" else "Nenhuma conversa encontrada",
                        if (state.query.isBlank()) "As conversas da sua conta aparecerão aqui." else "Tente buscar por outro nome ou limpe a busca.",
                        icon = AppIcons.messages_square,
                        actions = if (state.query.isNotBlank()) { { PrimaryButton("Limpar busca", { onQueryChange("") }, secondary = true, compact = true) } } else null,
                    ) }
            items(state.conversations) { item ->
                ConversationRow(item = item, onClick = { onOpenChat(item.id) })
            }
        }
    }
}

@Composable
private fun ConversationRow(item: Conversation, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = AppTheme.colors.surface) {
        Row(
            modifier = Modifier.padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(item.initials, color = item.tone.toColor())
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.preview, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(item.time, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        }
    }
}
