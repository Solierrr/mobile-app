@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.domain.canModify

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.AppButton
import com.project.solaria_mobile.core.designsystem.components.AppDialog
import com.project.solaria_mobile.core.designsystem.components.AppMenu
import com.project.solaria_mobile.core.designsystem.components.AppMenuDivider
import com.project.solaria_mobile.core.designsystem.components.AppMenuItem
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.ButtonSize
import com.project.solaria_mobile.core.designsystem.components.ButtonVariant
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import com.project.solaria_mobile.core.designsystem.components.SkeletonGroup
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.offline.LocalOffline
import com.project.solaria_mobile.feature.communication.domain.MessageAttachment
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(onBack: () -> Unit, onOpenDetails: (String) -> Unit = {}, viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var npsOpen by remember { mutableStateOf(false) }
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.ch_conversa), onBack) }, loading = { ChatSkeleton() }) { content ->
        // Sair de uma sessão do assistente que já respondeu pede a nota antes; a folha fecha e só então a tela sai.
        val leave = { if (content.npsPending) npsOpen = true else onBack() }
        androidx.activity.compose.BackHandler(enabled = content.npsPending && !npsOpen) { leave() }
        if (npsOpen) NpsSheet(onSubmit = viewModel::submitNps, onDismiss = { npsOpen = false; onBack() })
        // Conversa arquivada sai da lista desta conta; volta para a lista em vez de manter a tela aberta.
        // Quem perdeu a participação também não tem mais o que fazer aqui.
        LaunchedEffect(content.archived || content.removed) { if (content.archived || content.removed) onBack() }
        ChatScreen(
            content, viewModel::onDraftChange, viewModel::send, leave, viewModel::retry, viewModel::discard,
            ChatActions(viewModel::startEdit, viewModel::cancelEdit, viewModel::deleteMessage, viewModel::setMuted, viewModel::archive, viewModel::clearAction),
            onLoadOlder = viewModel::loadOlder,
            onAttachment = viewModel::sendAttachment,
            onOpenDetails = { onOpenDetails(content.conversation.id) },
            onDownload = viewModel::download,
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
    onLoadOlder: () -> Unit = {},
    onAttachment: (MessageAttachment) -> Unit = {},
    onOpenDetails: (() -> Unit)? = null,
    onDownload: (String, String?, java.time.Instant?) -> Unit = { _, _, _ -> },
) {
    val messagesState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var confirmArchive by remember { mutableStateOf(false) }
    var attachOpen by remember { mutableStateOf(false) }
    var locationOpen by remember { mutableStateOf(false) }
    var invalidAttachment by remember { mutableStateOf(false) }
    val working = state.action is ChatAction.Working
    val items = remember(state.messages, state.firstUnreadSequence, state.conversation.group) {
        buildChatItems(state.messages, state.firstUnreadSequence, state.conversation.group)
    }
    val typing = state.typingCount > 0 || state.assistantThinking
    val total = items.size + state.outgoing.size + if (typing) 1 else 0

    // A lista só acompanha as mensagens novas quando a pessoa estava no fim (ou a mensagem é dela); ler o histórico antigo não é interrompido.
    val nearBottom by remember {
        derivedStateOf {
            val info = messagesState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 2
        }
    }
    var anchored by remember { mutableStateOf(true) }
    var ready by remember(state.conversation.id) { mutableStateOf(false) }
    LaunchedEffect(messagesState) {
        snapshotFlow { messagesState.isScrollInProgress }.collect { scrolling -> if (!scrolling) anchored = nearBottom }
    }
    val lastKey = items.lastOrNull()?.key
    LaunchedEffect(lastKey, state.outgoing.size, typing) {
        if (total == 0) return@LaunchedEffect
        val lastIsMine = (items.lastOrNull() as? ChatListItem.Entry)?.message?.mine == true
        if (!ready) {
            messagesState.scrollToItem(total - 1)
            ready = true
        } else if (anchored || state.outgoing.isNotEmpty() || lastIsMine) {
            messagesState.animateScrollToItem(total - 1)
        }
    }
    val canLoadOlder by rememberUpdatedState(ready && state.hasMore && !state.loadingOlder)
    val loadOlder by rememberUpdatedState(onLoadOlder)
    LaunchedEffect(messagesState) {
        snapshotFlow { messagesState.firstVisibleItemIndex }.filter { it <= 3 }.collect { if (canLoadOlder) loadOlder() }
    }
    val scope = rememberCoroutineScope()

    val sources = rememberChatAttachmentSources(onAttachment = onAttachment, onInvalid = { invalidAttachment = true })
    val tooLong = state.draftLimitExceeded || state.draft.trim().length > MAX_MESSAGE_LENGTH
    val presenceDescription = when (state.online) {
        true -> stringResource(R.string.ch_online)
        false -> stringResource(R.string.ch_offline)
        null -> null
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalMediaUi provides MediaUi(state.mediaLoading, state.mediaUnavailable, onDownload)) {
    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.md, bottom = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            RoundIconButton(AppIcons.chevron_left, stringResource(R.string.common_back), onBack, size = Sizing.compactHeaderControl)
            Avatar(state.conversation.initials, Modifier.semantics { presenceDescription?.let { stateDescription = it } }, size = Sizing.compactHeaderControl, online = state.online == true, color = state.conversation.tone.toColor(), textColor = state.conversation.tone.toContentColor(), placeholderIcon = if (state.conversation.assistant) AppIcons.sparkles_filled else if (state.conversation.group) AppIcons.group_silhouette else AppIcons.user_silhouette)
            // Em grupo, tocar no cabeçalho abre os participantes e a gestão do grupo.
            Column(modifier = Modifier.weight(1f).then(if (onOpenDetails != null && state.conversation.group) Modifier.clickable(onClick = onOpenDetails) else Modifier)) {
                Text(state.conversation.name.ifBlank { stringResource(R.string.as_sessao) }, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
                ChatStatusLine(state)
            }
            Box {
                RoundIconButton(AppIcons.ellipsis, stringResource(R.string.ch_opcoes_da_conversa), { actions.onClearAction(); menuOpen = true }, size = Sizing.compactHeaderControl, enabled = !working)
                AppMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    state.muted?.let { muted ->
                        AppMenuItem(if (muted) stringResource(R.string.ch_reativar_notificacoes) else stringResource(R.string.ch_silenciar_conversa), { menuOpen = false; actions.onMute(!muted) }, icon = AppIcons.bell)
                    }
                    if (onOpenDetails != null && state.conversation.group) AppMenuItem(stringResource(R.string.gp_detalhes), { menuOpen = false; onOpenDetails() }, icon = AppIcons.users)
                    AppMenuItem(stringResource(R.string.ch_arquivar_conversa), { menuOpen = false; confirmArchive = true }, icon = AppIcons.folder)
                }
            }
        }
        if (state.messages.isEmpty() && state.outgoing.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                if (state.conversation.assistant) StateNotice(stringResource(R.string.as_comece_titulo), stringResource(R.string.as_comece_desc), icon = AppIcons.sparkles)
                else StateNotice(stringResource(R.string.ch_comece_a_conversa), stringResource(R.string.ch_envie_a_primeira_mensagem_para), icon = AppIcons.message_circle)
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = messagesState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
                    // Entre mensagens seguidas da mesma pessoa; entre pessoas ou sequências, o balão ganha mais espaço no topo.
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.loadingOlder) item(key = "loading-older") { OlderMessagesSkeleton() }
                    items(items, key = { it.key }) { item ->
                        when (item) {
                            is ChatListItem.Day -> Box(Modifier.padding(vertical = Spacing.sm)) { DaySeparator(item.date) }
                            ChatListItem.NewMessages -> Box(Modifier.padding(vertical = Spacing.sm)) { NewMessagesDivider() }
                            is ChatListItem.Entry -> Box(Modifier.padding(top = if (item.firstInGroup) Spacing.sm else 0.dp)) {
                                ChatMessageItem(item, state.conversation.group, state.conversation.assistant, editing = item.message.id == state.editingId, enabled = !working, onEdit = actions.onEdit, onDelete = { confirmDelete = it }, readOnly = state.conversation.readOnly || state.needsVerification)
                            }
                        }
                    }
                    items(state.outgoing, key = { it.id }) { message -> Box(Modifier.padding(top = Spacing.sm)) { OutgoingBubble(message, onRetry, onDiscard) } }
                    if (typing) item(key = "typing") { Box(Modifier.padding(top = Spacing.sm)) { TypingBubble() } }
                }
                ScrollToEndButton(!nearBottom, Modifier.align(Alignment.BottomEnd).padding(Spacing.md)) { scope.launch { messagesState.animateScrollToItem(total - 1) } }
            }
        }
        state.action?.let { action ->
            Text(
                action.message(),
                Modifier.padding(horizontal = Spacing.screen),
                color = if (action is ChatAction.Failed) AppTheme.colors.danger else AppTheme.colors.onSurfaceMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.editingId != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.ch_editando_mensagem), Modifier.weight(1f), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelLarge)
                AppButton(stringResource(R.string.ch_cancelar), actions.onCancelEdit, variant = ButtonVariant.Text, size = ButtonSize.Medium, enabled = !working)
            }
        }
        if (tooLong) Text(stringResource(R.string.ch_max_length, MAX_MESSAGE_LENGTH), Modifier.padding(horizontal = Spacing.screen), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
        state.sendFailure?.let { failure ->
            Text(com.project.solaria_mobile.core.common.ApiResult.Failure(failure, errorCode = state.sendErrorCode).userMessage(), Modifier.padding(horizontal = Spacing.screen), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
        }
        if (invalidAttachment) Text(stringResource(R.string.ch_arquivo_invalido), Modifier.padding(horizontal = Spacing.screen), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
        if (state.uploading) Text(stringResource(R.string.ch_enviando_anexo), Modifier.padding(horizontal = Spacing.screen), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        if (state.needsVerification) com.project.solaria_mobile.core.designsystem.components.PrimaryButton(stringResource(R.string.ch_conferir_estado), actions.onClearAction, secondary = true)
        else if (state.conversation.readOnly) Text(stringResource(if (state.conversation.communityStatus == "ARCHIVED") R.string.ch_comunidade_arquivada else R.string.ch_comunidade_estado_indisponivel), Modifier.fillMaxWidth().padding(Spacing.screen), color = AppTheme.colors.onSurfaceMuted)
        else Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.attachmentsEnabled && state.editingId == null) {
                RoundIconButton(AppIcons.folder_plus, stringResource(R.string.ch_anexar), { invalidAttachment = false; attachOpen = true }, size = Sizing.compactHeaderControl, enabled = !working)
            }
            Surface(shape = RoundedCornerShape(Radius.large), color = AppTheme.colors.inputFill, modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppTheme.colors.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.onSurface),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.draft.isEmpty()) Text(stringResource(R.string.ch_mensagem), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                                inner()
                            }
                        }
                    },
                )
            }
            val canSend = !tooLong && state.draft.isNotBlank() && !working
            RoundIconButton(if (state.editingId != null) AppIcons.check else AppIcons.send, if (state.editingId != null) stringResource(R.string.ch_salvar_edicao) else stringResource(R.string.ch_enviar_mensagem), onSend,
                enabled = canSend,
                background = if (canSend) AppTheme.colors.action else AppTheme.colors.inputFill,
                tint = if (canSend) AppTheme.colors.onAccent else AppTheme.colors.onSurfaceMuted)
        }
    }

    }
    if (attachOpen) AttachmentSheet(sources, onLocation = { locationOpen = true }, onDismiss = { attachOpen = false })
    if (locationOpen) LocationShareSheet(
        onDismiss = { locationOpen = false },
        onSend = { latitude, longitude, label -> onAttachment(MessageAttachment.Location(latitude, longitude, label)) },
    )
    confirmDelete?.let { id ->
        AppDialog(
            onDismissRequest = { confirmDelete = null },
            title = stringResource(R.string.ch_excluir_mensagem),
            message = stringResource(R.string.ch_a_mensagem_sera_excluida_para),
            confirmLabel = stringResource(R.string.ch_excluir),
            onConfirm = { confirmDelete = null; actions.onDelete(id) },
            dismissLabel = stringResource(R.string.ch_manter_mensagem),
            destructive = true,
        )
    }
    if (confirmArchive) {
        AppDialog(
            onDismissRequest = { confirmArchive = false },
            title = stringResource(R.string.ch_arquivar_conversa_2),
            message = stringResource(R.string.ch_a_conversa_sai_da_sua),
            confirmLabel = stringResource(R.string.pd_arquivar),
            onConfirm = { confirmArchive = false; actions.onArchive() },
        )
    }
}

/** Sob o nome: quem está digitando, depois quantos estão online (grupo), e se está silenciada. Em conversa direta, quem está online aparece na bolinha do avatar. */
@Composable
private fun ChatStatusLine(state: ChatUiState) {
    if (!state.conversation.assistant) RealtimeNotice(state.connection)
    val muted = state.muted == true
    val typingText = when {
        state.assistantThinking -> stringResource(R.string.as_pensando)
        state.typingCount == 0 -> null
        !state.conversation.group -> stringResource(R.string.ch_digitando)
        state.typingCount == 1 && state.typingNames.size == 1 -> stringResource(R.string.ch_x_digitando, state.typingNames.first())
        state.typingCount == 1 -> stringResource(R.string.ch_alguem_digitando)
        else -> stringResource(R.string.ch_varios_digitando, state.typingCount)
    }
    val presenceText = if (state.conversation.group && state.onlineCount > 0) stringResource(R.string.ch_n_online, state.onlineCount) else null
    val text = typingText ?: presenceText
    if (text != null) {
        Text(text, color = if (typingText != null) AppTheme.colors.brandStrong else AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
    }
    if (muted) Text(stringResource(R.string.ch_silenciada), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
}

/** Mensagem ainda na fila do aparelho: aguardando envio, ou recusada pelo servidor com as ações de tentar de novo e descartar. */
@Composable
private fun OutgoingBubble(message: OutgoingMessage, onRetry: (String) -> Unit, onDiscard: (String) -> Unit) {
    val failure = message.failure
    val status = when {
        failure != null -> stringResource(R.string.ch_nao_enviada, failure.userMessage())
        LocalOffline.current -> stringResource(R.string.ch_aguardando_conexao)
        else -> stringResource(R.string.ch_enviando)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            color = AppTheme.colors.action,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
            modifier = Modifier.padding(start = Spacing.xl),
        ) {
            MessageText(message.text, Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm), mine = true)
        }
        Text(status, modifier = Modifier.padding(top = 3.dp), color = if (failure != null) AppTheme.colors.danger else AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
        if (failure != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppButton(stringResource(R.string.ch_descartar), { onDiscard(message.id) }, variant = ButtonVariant.Secondary, size = ButtonSize.Medium)
                AppButton(stringResource(R.string.pr_tentar_de_novo), { onRetry(message.id) }, variant = ButtonVariant.Text, size = ButtonSize.Medium)
            }
        }
    }
}

/** Marcador do histórico anterior que está a caminho, no topo da lista. */
@Composable
private fun OlderMessagesSkeleton() {
    SkeletonGroup(Modifier.padding(vertical = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Skeleton(Modifier.fillMaxWidth(0.55f).height(Sizing.actionHeight), RoundedCornerShape(Radius.large))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { Skeleton(Modifier.fillMaxWidth(0.4f).height(Sizing.actionHeight), RoundedCornerShape(Radius.large)) }
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
private fun ChatMessageItem(entry: ChatListItem.Entry, group: Boolean, assistant: Boolean, editing: Boolean, enabled: Boolean, onEdit: (String) -> Unit, onDelete: (String) -> Unit, readOnly: Boolean = false) {
    val message = entry.message
    val clock = message.clockTime(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
    if (message.deleted) {
        DeletedBubble(message.mine, stringResource(R.string.ch_mensagem_excluida), clock)
        return
    }
    var open by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    var now by remember { mutableStateOf(java.time.Instant.now()) }
    LaunchedEffect(open) {
        while (open) { now = java.time.Instant.now(); kotlinx.coroutines.delay(1_000) }
    }
    val scope = rememberCoroutineScope()
    val time = listOfNotNull(clock.ifBlank { null }, stringResource(R.string.ch_editada).takeIf { message.edited }, stringResource(R.string.ch_editando).takeIf { editing }).joinToString(" · ")
    MessageRow(
        entry, group, time,
        bubbleModifier = Modifier.combinedClickable(enabled = enabled, onLongClickLabel = stringResource(R.string.ch_opcoes_da_mensagem), onLongClick = { open = true }, onClick = {}),
        menu = {
            AppMenu(expanded = open, onDismissRequest = { open = false }) {
                if (message.text.isNotBlank()) AppMenuItem(stringResource(R.string.ps_copiar), {
                    open = false
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("mensagem", message.text))) }
                }, icon = AppIcons.copy)
                if (message.canModify(now) && !assistant && !readOnly) {
                    if (message.attachment == null) AppMenuItem(stringResource(R.string.common_edit), { open = false; onEdit(message.id) }, icon = AppIcons.pencil)
                    AppMenuDivider()
                    AppMenuItem(stringResource(R.string.ch_excluir), { open = false; onDelete(message.id) }, destructive = true, icon = AppIcons.trash)
                } else if (message.mine && !assistant && !readOnly) {
                    Text(stringResource(R.string.ch_prazo_edicao), Modifier.padding(Spacing.sm), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    )
}

/** Botão para voltar às mensagens mais recentes, visível só quando a lista está longe do fim. */
@Composable
private fun ScrollToEndButton(visible: Boolean, modifier: Modifier, onClick: () -> Unit) {
    AnimatedVisibility(visible, modifier, enter = fadeIn(), exit = fadeOut()) {
        RoundIconButton(AppIcons.chevron_down, stringResource(R.string.ch_ir_para_o_fim), onClick, size = Sizing.compactHeaderControl)
    }
}
