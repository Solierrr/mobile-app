@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.PaddingValues
import com.project.solaria_mobile.core.designsystem.components.ListRow
import com.project.solaria_mobile.feature.communication.domain.Conversation

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
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@Composable
fun ConversationsRoute(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel<ConversationsViewModel>(),
    assistant: Boolean = false,
    onNewSession: () -> Unit = {},
    onOpenConnections: () -> Unit = {},
    onStartConversation: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Ao voltar de uma conversa, recarrega para refletir envio, edição, exclusão e arquivamento; a primeira abertura já carrega no ViewModel.
    var returning by remember { mutableStateOf(false) }
    LifecycleResumeEffect(viewModel) {
        if (returning) viewModel.load() else returning = true
        onPauseOrDispose {}
    }
    val title = stringResource(if (assistant) R.string.as_assistente else R.string.pd_conversas)
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(title, onBack = if (assistant) onBack else null) }, loading = { ListSkeleton(search = true) }) { content ->
        ConversationsScreen(content, viewModel::onQueryChange, onBack, onOpenChat, state as? UiState.Content, assistant, onNewSession, onOpenConnections, onStartConversation)
    }
}

@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    resource: UiState.Content<*>? = null,
    assistant: Boolean = false,
    onNewSession: () -> Unit = {},
    onOpenConnections: () -> Unit = {},
    onStartConversation: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().appBackground().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle(stringResource(if (assistant) R.string.as_assistente else R.string.pd_conversas), onBack = if (assistant) onBack else null)
        if (assistant) com.project.solaria_mobile.core.designsystem.components.ActionRow(stringResource(R.string.as_nova_sessao), AppIcons.sparkles, onNewSession)
        else Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            // Não há botão para começar uma conversa nova: os dois atalhos ficam no topo da lista.
            com.project.solaria_mobile.core.designsystem.components.ActionTile(stringResource(R.string.ps_minhas_conexoes), AppIcons.users, onOpenConnections, Modifier.weight(1f).fillMaxHeight())
            com.project.solaria_mobile.core.designsystem.components.ActionTile(stringResource(R.string.cv_iniciar_conversa), AppIcons.message_circle, onStartConversation, Modifier.weight(1f).fillMaxHeight())
        }
        // Sem nenhuma conversa não há o que buscar; a busca volta quando houver conversa ou texto digitado.
        if (state.conversations.isNotEmpty() || state.query.isNotBlank()) SearchField(stringResource(R.string.cv_buscar_conversa), state.query, onQueryChange)
        if (resource != null) ResourceStatus(resource)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs), contentPadding = PaddingValues(bottom = com.project.solaria_mobile.core.designsystem.theme.LocalBottomBarInset.current)) {
            if (state.conversations.isEmpty()) item { androidx.compose.foundation.layout.Box(Modifier.fillParentMaxHeight(0.8f).fillMaxWidth(), contentAlignment = Alignment.Center) { StateNotice(
                        if (state.query.isNotBlank()) stringResource(R.string.cv_nenhuma_conversa_encontrada) else stringResource(if (assistant) R.string.as_nenhuma_sessao else R.string.pc_nenhuma_conversa_ainda),
                        if (state.query.isNotBlank()) stringResource(R.string.cv_tente_buscar_por_outro_nome) else stringResource(if (assistant) R.string.as_nenhuma_sessao_desc else R.string.cv_as_conversas_da_sua_conta),
                        icon = if (assistant) AppIcons.sparkles else AppIcons.messages_square,
                        actions = if (state.query.isNotBlank()) { { PrimaryButton(stringResource(R.string.settings_clear_search), { onQueryChange("") }, secondary = true, compact = true) } } else null,
                    ) } }
            items(state.conversations) { item ->
                ConversationRow(item = item, onClick = { onOpenChat(item.id) })
            }
        }
    }
}

@Composable
private fun ConversationRow(item: Conversation, onClick: () -> Unit) {
    ListRow(
        title = item.name.ifBlank { stringResource(R.string.as_sessao) },
        supporting = if (item.geo) stringResource(R.string.geo_assistente) else null,
        subtitle = item.preview,
        onClick = onClick,
        leading = { Avatar(item.initials, color = item.tone.toColor(), textColor = item.tone.toContentColor(), placeholderIcon = if (item.geo) AppIcons.globe else if (item.assistant) AppIcons.sparkles_filled else if (item.group) AppIcons.group_silhouette else AppIcons.user_silhouette) },
        trailing = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(item.time, color = if (item.unread > 0) AppTheme.colors.brandStrong else AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                if (item.unread > 0) {
                    val unreadDescription = stringResource(R.string.cv_n_nao_lidas, item.unread)
                    Surface(shape = CircleShape, color = AppTheme.colors.action) {
                        Text(
                            if (item.unread > 99) "99+" else item.unread.toString(),
                            Modifier.padding(horizontal = 6.dp, vertical = 1.dp).semantics { contentDescription = unreadDescription },
                            color = AppTheme.colors.onAccent,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        },
        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs),
    )
}
