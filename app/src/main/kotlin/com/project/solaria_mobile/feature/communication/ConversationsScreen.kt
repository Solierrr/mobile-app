@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.data.Conversation
import com.project.solaria_mobile.feature.communication.data.AvatarTone

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
import com.project.solaria_mobile.core.designsystem.components.SolariaAvatar
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaBlue
import com.project.solaria_mobile.core.designsystem.theme.SolariaGreen
import com.project.solaria_mobile.core.designsystem.theme.SolariaPink
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun ConversationsRoute(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenProject: () -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConversationsScreen(state, viewModel::onQueryChange, onBack, onOpenChat, onOpenProject)
}

@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenProject: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg),
        verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
    ) {
        ScreenTitle("Conversas", onBack, trailingIcon = LucideR.drawable.lucide_ic_at_sign, trailingLabel = "Iniciar conversa", onTrailingClick = onOpenChat)
        SectionHeading("Opções", faded = true)
        ActionRow("Iniciar conversa", LucideR.drawable.lucide_ic_message_circle, onOpenChat, compact = true)
        ActionRow("Conversas de projetos", LucideR.drawable.lucide_ic_folder, onOpenProject, compact = true)
        SectionHeading("Todas as conversas", faded = true)
        SearchField("Busque uma mensagem", state.query, onQueryChange, trailingIcon = LucideR.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
        LazyColumn(verticalArrangement = Arrangement.spacedBy(SolariaSpace.xs)) {
            items(state.conversations) { item ->
                ConversationRow(item = item, onClick = onOpenChat)
            }
        }
    }
}

@Composable
private fun ConversationRow(item: Conversation, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = SolariaWhite) {
        Row(
            modifier = Modifier.padding(vertical = SolariaSpace.xs),
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SolariaAvatar(item.initials, color = when (item.tone) {
                AvatarTone.ORANGE -> SolariaOrange
                AvatarTone.BLUE -> SolariaBlue
                AvatarTone.GREEN -> SolariaGreen
                AvatarTone.PINK -> SolariaPink
            })
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.preview, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(item.time, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        }
    }
}
