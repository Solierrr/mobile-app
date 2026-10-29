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
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.components.UiStateHost

@Composable
fun ConversationsRoute(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onStartChat: () -> Unit,
    onOpenProject: () -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load) { content ->
        ConversationsScreen(content, viewModel::onQueryChange, onBack, onOpenChat, onStartChat, onOpenProject)
    }
}

@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onStartChat: () -> Unit,
    onOpenProject: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle("Conversas", onBack, trailingIcon = LucideR.drawable.lucide_ic_at_sign, trailingLabel = "Iniciar conversa", onTrailingClick = onStartChat)
        SectionHeading("Opções", faded = true)
        ActionRow("Iniciar conversa", LucideR.drawable.lucide_ic_message_circle, onStartChat, compact = true)
        ActionRow("Conversas de projetos", LucideR.drawable.lucide_ic_folder, onOpenProject, compact = true)
        SectionHeading("Todas as conversas", faded = true)
        SearchField("Busque uma mensagem", state.query, onQueryChange, trailingIcon = LucideR.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = AppTheme.colors.surface) {
        Row(
            modifier = Modifier.padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(item.initials, color = item.tone.toColor())
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.preview, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.accessLost) Text("Você não participa mais", color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(item.time, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        }
    }
}
