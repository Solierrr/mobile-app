@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius

@Composable
fun ChatRoute(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(state, viewModel::onDraftChange, viewModel::send, onBack)
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.md, bottom = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            RoundIconButton(LucideR.drawable.lucide_ic_chevron_left, "Voltar", onBack, size = Sizing.compactHeaderControl)
            Avatar("RD", size = Sizing.compactHeaderControl, color = AppTheme.colors.secondary, online = true)
            Column(modifier = Modifier.weight(1f)) {
                Text("Ricardo Dostoiévski", color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text("Disponível", color = AppTheme.colors.primary, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
            RoundIconButton(LucideR.drawable.lucide_ic_phone, "Ligar", {}, size = Sizing.compactHeaderControl)
            RoundIconButton(LucideR.drawable.lucide_ic_ellipsis, "Mais opções", {}, size = Sizing.compactHeaderControl)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppTheme.colors.outline))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Hoje", modifier = Modifier.align(Alignment.CenterHorizontally), color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            state.messages.forEach { message ->
                MessageBubble(message = message.text, mine = message.mine, time = message.time)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(LucideR.drawable.lucide_ic_link, "Anexar arquivo", {}, size = Sizing.messageAction)
            Surface(shape = RoundedCornerShape(Radius.pill), color = AppTheme.colors.inputFill, modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 14.dp),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = AppTheme.colors.onSurface),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.draft.isEmpty()) Text("Insira sua mensagem aqui", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                inner()
                            }
                            if (state.draft.isEmpty()) {
                                Icon(painter = painterResource(LucideR.drawable.lucide_ic_mic), contentDescription = "Mensagem de áudio", tint = AppTheme.colors.onSurfaceMuted, modifier = Modifier.size(Sizing.iconSmall))
                            }
                        }
                    },
                )
            }
            Surface(
                modifier = Modifier.size(Sizing.messageAction),
                onClick = onSend,
                shape = CircleShape,
                color = AppTheme.colors.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(LucideR.drawable.lucide_ic_send), contentDescription = "Enviar mensagem", tint = AppTheme.colors.onAccent, modifier = Modifier.size(Sizing.icon))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: String, mine: Boolean, time: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = if (mine) AppTheme.colors.ownMessage else AppTheme.colors.inputFill,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp),
        ) {
            Text(message, modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm), color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        Text(time, modifier = Modifier.padding(top = 3.dp), color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
    }
}
