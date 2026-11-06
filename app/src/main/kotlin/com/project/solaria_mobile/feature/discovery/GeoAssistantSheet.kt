package com.project.solaria_mobile.feature.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.SheetSize
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.communication.MessageText
import com.project.solaria_mobile.feature.communication.TypingBubble
import com.project.solaria_mobile.feature.project.domain.ProjectSummary

/**
 * Folha do assistente geográfico (ai-geographic) de um projeto no mapa. As respostas são simuladas, sem chamada de rede:
 * o contrato do ai-geographic ainda está em refinamento. A folha cresce quando a conversa começa, e a sessão é a mesma até a
 * pessoa sair da tela do mapa, mesmo que a feche sem querer.
 */
@Composable
fun GeoAssistantSheet(project: ProjectSummary, messages: List<GeoMessage>, thinking: Boolean, onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    val started = messages.isNotEmpty()
    BottomSheet(onDismiss = onDismiss, size = if (started) SheetSize.Expanded else SheetSize.Content, scrollable = !started) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(stringResource(R.string.geo_assistente), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.geo_sobre, project.name), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (started) Conversation(messages, thinking, Modifier.weight(1f)) else Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            listOf(R.string.geo_exemplo_1, R.string.geo_exemplo_2, R.string.geo_exemplo_3).forEach { example ->
                val text = stringResource(example)
                ActionRow(text, AppIcons.sparkles, { draft = text }, compact = true)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.screenBottom),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(Radius.large), color = AppTheme.colors.inputFill, modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = draft, onValueChange = { draft = it }, maxLines = 4,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppTheme.colors.onSurface),
                    cursorBrush = SolidColor(AppTheme.colors.onSurface),
                    decorationBox = { inner ->
                        Box { if (draft.isEmpty()) Text(stringResource(R.string.geo_pergunte), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium); inner() }
                    },
                )
            }
            val canSend = draft.isNotBlank() && !thinking
            RoundIconButton(
                AppIcons.send, stringResource(R.string.ch_enviar_mensagem), { onSend(draft); draft = "" },
                size = Sizing.compactHeaderControl, enabled = canSend,
                background = if (canSend) AppTheme.colors.action else AppTheme.colors.inputFill,
                tint = if (canSend) AppTheme.colors.onAccent else AppTheme.colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun Conversation(messages: List<GeoMessage>, thinking: Boolean, modifier: Modifier) {
    val state = rememberLazyListState()
    LaunchedEffect(messages.size, thinking) {
        val total = messages.size + if (thinking) 1 else 0
        if (total > 0) state.animateScrollToItem(total - 1)
    }
    LazyColumn(
        modifier.fillMaxWidth(), state = state,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(messages) { message -> Bubble(message) }
        if (thinking) item { TypingBubble() }
    }
}

@Composable
private fun Bubble(message: GeoMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (message.mine) AppTheme.colors.action else AppTheme.colors.inputFill,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (message.mine) 18.dp else 4.dp, bottomEnd = if (message.mine) 4.dp else 18.dp),
            modifier = Modifier.padding(start = if (message.mine) Spacing.xl else 0.dp, end = if (message.mine) 0.dp else Spacing.xl),
        ) {
            MessageText(message.text, Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm), message.mine)
        }
    }
}
