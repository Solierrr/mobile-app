@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@Composable
fun ConversationsRoute(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Ao voltar de uma conversa, recarrega para refletir envio, edição, exclusão e arquivamento; a primeira abertura já carrega no ViewModel.
    var returning by remember { mutableStateOf(false) }
    LifecycleResumeEffect(viewModel) {
        if (returning) viewModel.load() else returning = true
        onPauseOrDispose {}
    }
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Conversas", onBack = null) }, loading = { ListSkeleton(search = true) }) { content ->
        ConversationsScreen(content, viewModel::onQueryChange, onBack, onOpenChat, state as? UiState.Content, viewModel::load, viewModel::toggleArchived)
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
    onToggleArchived: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().appBackground().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle("Conversas", onBack = null, actions = { RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar conversas") })
        SectionHeading(
            if (state.showArchived) "Arquivadas" else "Todas as conversas",
            faded = true,
            action = if (state.showArchived) "Ver ativas" else "Arquivadas",
            onAction = onToggleArchived,
        )
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

/**
 * Linha de uma conversa na lista.
 *
 * Uma conversa a que o usuário perdeu o acesso (removido do grupo ou fora da comunidade) continua na lista, em somente
 * leitura, e a linha mostra o aviso "Você não participa mais". Ao abrir, o chat mostra o histórico salvo, sem campo de
 * envio.
 * - Por quê: o app guarda o histórico no Room e mantém o que o usuário já baixou, como o WhatsApp faz com grupos de que
 *   você saiu. O api-messenger nega a leitura depois da remoção, então essa conversa não sincroniza mais.
 * - De onde vem: `Conversation.accessLost`, lido da coluna `accessLost` da tabela `conversations`. Vira `true` quando a
 *   conversa some do `GET /messaging/conversations/me` ou chega `CONVERSATION_REMOVED`, e volta a `false` se ela
 *   reaparecer no `/me` ou chegar um `CONVERSATION_UPDATED` mais novo.
 * - Contexto completo: `SOLIER/relatorios/mobile-historico-offline-plano-2026-10-28.md`, §8.1.1 e §8.3.
 */
@Composable
private fun ConversationRow(item: Conversation, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier.padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(item.initials, color = item.tone.toColor())
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.preview, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.accessLost) Text("Você não participa mais", color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(item.time, color = if (item.unreadCount > 0) AppTheme.colors.action else AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                if (item.unreadCount > 0) {
                    Surface(shape = CircleShape, color = AppTheme.colors.action) {
                        Text(
                            if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                            Modifier.padding(horizontal = 6.dp, vertical = 1.dp).semantics { contentDescription = "${item.unreadCount} não lidas" },
                            color = AppTheme.colors.onAccent,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}
