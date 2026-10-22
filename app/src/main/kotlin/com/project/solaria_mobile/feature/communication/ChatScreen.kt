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
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.userMessage

@Composable
fun ChatRoute(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Conversa", onBack) }) { content -> ChatScreen(content, viewModel::onDraftChange, viewModel::send, onBack) }
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
            RoundIconButton(AppIcons.chevron_left, "Voltar", onBack, size = Sizing.compactHeaderControl)
            Avatar(state.conversation.initials, size = Sizing.compactHeaderControl, color = state.conversation.tone.toColor(), online = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(state.conversation.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppTheme.colors.outline))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            state.messages.forEach { message ->
                MessageBubble(message = message.text, mine = message.mine, time = message.time)
            }
        }
        state.sendFailure?.let { failure ->
            Text(failure.userMessage(), Modifier.padding(horizontal = Spacing.screen), color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(Radius.large), color = AppTheme.colors.inputFill, modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 14.dp),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = AppTheme.colors.onSurface),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.draft.isEmpty()) Text("Mensagem", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                inner()
                            }

                        }
                    },
                )
            }
            Surface(
                modifier = Modifier.size(Sizing.messageAction),
                onClick = onSend,
                enabled = !state.sending && state.draft.isNotBlank(),
                shape = CircleShape,
                color = AppTheme.colors.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(AppIcons.send), contentDescription = "Enviar mensagem", tint = AppTheme.colors.onAccent, modifier = Modifier.size(Sizing.icon))
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
