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
import com.project.solaria_mobile.core.designsystem.components.SolariaAvatar
import com.project.solaria_mobile.core.designsystem.theme.SolariaGreen
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaInput
import com.project.solaria_mobile.core.designsystem.theme.SolariaLine
import com.project.solaria_mobile.core.designsystem.theme.SolariaMessageOwn
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaSize
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

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
    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.md, bottom = SolariaSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
        ) {
            RoundIconButton(LucideR.drawable.lucide_ic_chevron_left, "Voltar", onBack, size = SolariaSize.compactHeaderControl)
            SolariaAvatar("RD", size = SolariaSize.compactHeaderControl, color = SolariaOrange, online = true)
            Column(modifier = Modifier.weight(1f)) {
                Text("Ricardo Dostoiévski", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text("Disponível", color = SolariaGreen, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
            RoundIconButton(LucideR.drawable.lucide_ic_phone, "Ligar", {}, size = SolariaSize.compactHeaderControl)
            RoundIconButton(LucideR.drawable.lucide_ic_ellipsis, "Mais opções", {}, size = SolariaSize.compactHeaderControl)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SolariaLine))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = SolariaSpace.screen).padding(vertical = SolariaSpace.lg),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        ) {
            Text("Hoje", modifier = Modifier.align(Alignment.CenterHorizontally), color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            state.messages.forEach { message ->
                MessageBubble(message = message.text, mine = message.mine, time = message.time)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.sm, bottom = SolariaSpace.md),
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(LucideR.drawable.lucide_ic_link, "Anexar arquivo", {}, size = SolariaSize.messageAction)
            Surface(shape = RoundedCornerShape(SolariaRadius.pill), color = SolariaInput, modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.padding(horizontal = SolariaSpace.lg, vertical = 14.dp),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = SolariaInk),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.draft.isEmpty()) Text("Insira sua mensagem aqui", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                inner()
                            }
                            if (state.draft.isEmpty()) {
                                Icon(painter = painterResource(LucideR.drawable.lucide_ic_mic), contentDescription = "Mensagem de áudio", tint = SolariaInkMuted, modifier = Modifier.size(SolariaSpace.iconSmall))
                            }
                        }
                    },
                )
            }
            Surface(
                modifier = Modifier.size(SolariaSize.messageAction),
                onClick = onSend,
                shape = CircleShape,
                color = SolariaGreen,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(LucideR.drawable.lucide_ic_send), contentDescription = "Enviar mensagem", tint = SolariaWhite, modifier = Modifier.size(SolariaSpace.icon))
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
            color = if (mine) SolariaMessageOwn else SolariaInput,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp),
        ) {
            Text(message, modifier = Modifier.padding(horizontal = SolariaSpace.md, vertical = SolariaSpace.sm), color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        Text(time, modifier = Modifier.padding(top = 3.dp), color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
    }
}
