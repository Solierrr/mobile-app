@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import com.project.solaria_mobile.core.designsystem.components.SkeletonGroup
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import com.project.solaria_mobile.core.offline.LocalOffline
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.launch
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.MessageStatus

@Composable
fun ChatRoute(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Conversa", onBack) }, loading = { ChatSkeleton() }) { content ->
        // Conversa arquivada sai da lista desta conta; volta para a lista em vez de manter a tela aberta.

        ChatScreen(
            content, viewModel::onDraftChange, viewModel::send, onBack, viewModel::retry, viewModel::discard,
            ChatActions(viewModel::startEdit, viewModel::cancelEdit, viewModel::delete, { viewModel.toggleMute() }, viewModel::toggleArchive, viewModel::dismissNotice),
        )
    }
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onRetry: (String) -> Unit = {},
    onDiscard: (String) -> Unit = {},
    actions: ChatActions = ChatActions(),
) {
    val messagesState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var confirmArchive by remember { mutableStateOf(false) }
    val working = state.busy
    var previousCount by remember(state.conversation.id) { mutableIntStateOf(0) }
    LaunchedEffect(state.messages.size) {
        val count = state.messages.size
        val lastVisible = messagesState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (count > 0 && (previousCount == 0 || lastVisible >= previousCount - 2 || state.messages.any { it.status == MessageStatus.PENDING } || state.messages.lastOrNull()?.mine == true)) {
            messagesState.animateScrollToItem(count - 1)
        }
        previousCount = count
    }
    val tooLong = state.draftLimitExceeded || state.draft.trim().length > MAX_MESSAGE_LENGTH
    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.md, bottom = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            RoundIconButton(AppIcons.chevron_left, "Voltar", onBack, size = Sizing.compactHeaderControl)
            Avatar(state.conversation.initials, size = Sizing.compactHeaderControl, color = state.conversation.tone.toColor())
            Column(modifier = Modifier.weight(1f)) {
                Text(state.conversation.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                val presence = state.presence
                val presenceText = presence.label()
                presenceText?.let { Text(it, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium) }
                if (state.conversation.muted == true) Text("Silenciada", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
            Box {
                RoundIconButton(AppIcons.ellipsis, "Opções da conversa", { actions.onClearAction(); menuOpen = true }, size = Sizing.compactHeaderControl, enabled = !working)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = AppTheme.colors.surface) {
                    state.conversation.muted?.let { muted ->
                        DropdownMenuItem(
                            text = { Text(if (muted) "Reativar notificações" else "Silenciar conversa") },
                            onClick = { menuOpen = false; actions.onMute(!muted) },
                        )
                    }
                    DropdownMenuItem(text = { Text(if (state.conversation.archived) "Desarquivar conversa" else "Arquivar conversa") }, onClick = { menuOpen = false; confirmArchive = true })
                }
            }
        }
        if (state.messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                StateNotice("Comece a conversa", "Envie a primeira mensagem para conversar por aqui.", icon = AppIcons.message_circle)
            }
        } else {
            LazyColumn(
                state = messagesState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    if (message.status != MessageStatus.SENT) OutgoingBubble(message, onRetry, onDiscard) else ChatMessageItem(message, editing = message.id == state.editing?.id, enabled = !working, onEdit = actions.onEdit, onDelete = { confirmDelete = it })
                }
            }
        }
        state.notice?.let { notice ->
            Text(notice.message(), Modifier.padding(horizontal = Spacing.screen), color = AppTheme.colors.danger,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        if (state.editing?.id != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Editando mensagem", Modifier.weight(1f), color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                TextButton(actions.onCancelEdit, Modifier.heightIn(min = Sizing.touchTarget), enabled = !working) {
                    Text("Cancelar", color = AppTheme.colors.action, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (tooLong) Text("A mensagem pode ter até $MAX_MESSAGE_LENGTH caracteres.", Modifier.padding(horizontal = Spacing.screen), color = AppTheme.colors.danger, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        if (state.conversation.accessLost) {
            StateNotice("Conversa em somente leitura", "Você não participa mais desta conversa.")
        } else Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(Radius.large), color = AppTheme.colors.inputFill, modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = state.draft,
                    readOnly = working,
                    onValueChange = onDraftChange,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    maxLines = 6,
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
            val canSend = !tooLong && state.draft.isNotBlank() && !working
            RoundIconButton(if (state.editing?.id != null) AppIcons.check else AppIcons.send, if (state.editing?.id != null) "Salvar edição" else "Enviar mensagem", onSend,
                enabled = canSend,
                background = if (canSend) AppTheme.colors.action else AppTheme.colors.inputFill,
                tint = if (canSend) AppTheme.colors.onAccent else AppTheme.colors.onSurfaceMuted)
        }
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Excluir mensagem?", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall) },
            text = { Text("A mensagem será excluída para todos na conversa e ficará marcada como excluída.", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = { confirmDelete = null; actions.onDelete(id) }) { Text("Excluir", color = AppTheme.colors.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Manter mensagem") } },
            containerColor = AppTheme.colors.surface,
        )
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(if (state.conversation.archived) "Desarquivar conversa?" else "Arquivar conversa?", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall) },
            text = { Text(if (state.conversation.archived) "A conversa volta para sua lista de ativas." else "A conversa sai da sua lista. As outras pessoas continuam com acesso a ela.", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = { confirmArchive = false; actions.onArchive() }) { Text(if (state.conversation.archived) "Desarquivar" else "Arquivar") } },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } },
            containerColor = AppTheme.colors.surface,
        )
    }
}

@Composable
private fun MessageBubble(
    message: String,
    mine: Boolean,
    time: String,
    statusColor: Color = AppTheme.colors.onSurfaceMuted,
    bubbleModifier: Modifier = Modifier,
    placeholder: Boolean = false,
    menu: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box {
        Surface(
            modifier = bubbleModifier,
            color = if (placeholder) Color.Transparent else if (mine) AppTheme.colors.ownMessage else AppTheme.colors.inputFill,
            border = if (placeholder) androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.inputFill) else null,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp),
        ) {
            Text(
                message,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                color = if (placeholder) AppTheme.colors.onSurfaceMuted else AppTheme.colors.onSurface,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontStyle = if (placeholder) FontStyle.Italic else FontStyle.Normal),
            )
        }
            menu()
        }
        Text(time, modifier = Modifier.padding(top = 3.dp), color = statusColor, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
    }
}

/** Mensagem ainda na fila do aparelho: aguardando envio, ou recusada pelo servidor com as ações de tentar de novo e descartar. */
@Composable
private fun OutgoingBubble(message: ChatMessage, onRetry: (String) -> Unit, onDiscard: (String) -> Unit) {
    val failed = message.status == MessageStatus.FAILED
    val status = when {
        failed -> "Não enviada. Tente novamente."
        LocalOffline.current -> "Aguardando conexão"
        else -> "Enviando…"
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        MessageBubble(message.text, mine = true, time = status, statusColor = if (failed) AppTheme.colors.danger else AppTheme.colors.onSurfaceMuted)
        if (failed) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TextButton({ onDiscard(message.id) }, Modifier.heightIn(min = Sizing.touchTarget)) {
                    Text("Descartar", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                }
                TextButton({ onRetry(message.id) }, Modifier.heightIn(min = Sizing.touchTarget)) {
                    Text("Tentar de novo", color = AppTheme.colors.action, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Balões alternados nas larguras típicas, sem texto falso. */
@Composable
private fun ChatSkeleton() {
    SkeletonGroup(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(0.7f to false, 0.5f to true, 0.8f to false, 0.4f to true, 0.6f to true, 0.75f to false).forEach { (width, own) ->
            Box(Modifier.fillMaxWidth(), contentAlignment = if (own) Alignment.CenterEnd else Alignment.CenterStart) {
                Skeleton(Modifier.fillMaxWidth(width).height(Sizing.actionHeight), RoundedCornerShape(Radius.large))
            }
        }
    }
}

/** Ações da conversa ligadas ao ViewModel; os padrões vazios servem para prévias e testes de tela. */
data class ChatActions(
    val onEdit: (String) -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onMute: (Boolean) -> Unit = {},
    val onArchive: () -> Unit = {},
    val onClearAction: () -> Unit = {},
)

/**
 * Mensagem do histórico. Toque longo abre copiar e, nas próprias, editar e excluir. Mensagem excluída aparece como marcador,
 * sem menu, e a editada mostra a marca ao lado do horário.
 */
@Composable
private fun ChatMessageItem(message: ChatMessage, editing: Boolean, enabled: Boolean, onEdit: (String) -> Unit, onDelete: (String) -> Unit) {
    if (message.deleted) {
        MessageBubble("Mensagem excluída", message.mine, message.time, placeholder = true)
        return
    }
    var open by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val time = listOfNotNull(message.time.ifBlank { null }, "editada".takeIf { message.edited }, "editando".takeIf { editing }).joinToString(" · ")
    MessageBubble(
        message.text, message.mine, time,
        bubbleModifier = Modifier.combinedClickable(enabled = enabled, onLongClickLabel = "Opções da mensagem", onLongClick = { open = true }, onClick = {}),
        menu = {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = AppTheme.colors.surface) {
                DropdownMenuItem(text = { Text("Copiar") }, onClick = {
                    open = false
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("mensagem", message.text))) }
                })
                if (message.changeable && message.id.isNotBlank()) {
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { open = false; onEdit(message.id) })
                    DropdownMenuItem(text = { Text("Excluir", color = AppTheme.colors.danger) }, onClick = { open = false; onDelete(message.id) })
                }
            }
        },
    )
}
