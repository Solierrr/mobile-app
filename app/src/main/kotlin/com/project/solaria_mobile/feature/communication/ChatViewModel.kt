package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.AssistantReply
import com.project.solaria_mobile.feature.communication.domain.AssistantRepository
import com.project.solaria_mobile.feature.communication.domain.NEW_SESSION_PREFIX
import com.project.solaria_mobile.feature.communication.domain.MessageAttachment
import com.project.solaria_mobile.feature.communication.data.ChatCapabilities
import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.NoChatRealtime
import com.project.solaria_mobile.feature.communication.data.PAGE_SIZE
import com.project.solaria_mobile.feature.communication.data.PresenceState
import com.project.solaria_mobile.feature.communication.data.conversationId
import com.project.solaria_mobile.feature.communication.data.toMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.project.solaria_mobile.feature.communication.data.MessageOutbox
import com.project.solaria_mobile.feature.communication.data.PendingState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Mensagem guardada na fila do aparelho: aguardando envio ou recusada pelo servidor. */
data class OutgoingMessage(val id: String, val text: String, val failure: ErrorKind?)

/** Ações sobre mensagens e sobre o estado pessoal da conversa. */
enum class ChatOperation { EDIT, DELETE, MUTE, UNMUTE, ARCHIVE }

sealed interface ChatAction {
    data class Working(val operation: ChatOperation) : ChatAction
    data class Failed(val operation: ChatOperation, val kind: ErrorKind) : ChatAction
}

data class ChatUiState(
    val conversation: Conversation,
    val draft: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val sendFailure: ErrorKind? = null,
    val draftLimitExceeded: Boolean = false,
    val outgoing: List<OutgoingMessage> = emptyList(),
    val editingId: String? = null,
    val action: ChatAction? = null,
    /** Nulo enquanto o estado pessoal não foi lido; o menu só oferece silenciar quando ele é conhecido. */
    val muted: Boolean? = null,
    val archived: Boolean = false,
    /** A pessoa deixou de participar da conversa (evento `CONVERSATION_REMOVED`); a tela deve sair dela. */
    val removed: Boolean = false,
    /** Há histórico mais antigo no servidor para carregar ao rolar para cima. */
    val hasMore: Boolean = false,
    val loadingOlder: Boolean = false,
    /** Primeira mensagem não lida ao abrir a conversa, onde aparece o divisor de novas mensagens. */
    val firstUnreadSequence: Int? = null,
    /** Nomes de quem está digitando, quando conhecidos, e quantas pessoas são no total. */
    val typingNames: List<String> = emptyList(),
    val typingCount: Int = 0,
    /** Em conversa direta, se o outro participante está online; nulo quando não se sabe ou é grupo. */
    val online: Boolean? = null,
    val onlineCount: Int = 0,
    /** Anexos (foto, documento, localização) só existem no modo de demonstração, até o messenger ter contrato para isso. */
    val attachmentsEnabled: Boolean = false,
    /** O assistente está elaborando a resposta de uma mensagem enviada nesta sessão. */
    val assistantThinking: Boolean = false,
)

internal const val MAX_MESSAGE_LENGTH = 8_000

private data class ChatContent(
    val conversation: Conversation,
    val messages: List<ChatMessage>,
    val hasMore: Boolean = false,
    val firstUnreadSequence: Int? = null,
)

private data class LivePresence(val typing: Set<String> = emptySet(), val presence: PresenceState = PresenceState(), val loadingOlder: Boolean = false, val thinking: Boolean = false)

private data class ChatControls(
    val editingId: String?,
    val action: ChatAction?,
    val muted: Boolean?,
    val archived: Boolean,
    val removed: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: CommunicationRepository,
    private val outbox: MessageOutbox,
    private val realtime: ChatRealtime,
    private val capabilities: ChatCapabilities,
    private val assistantRepository: AssistantRepository,
) : ViewModel() {
    internal constructor(savedStateHandle: SavedStateHandle, repository: CommunicationRepository, outbox: MessageOutbox) :
        this(savedStateHandle, repository, outbox, NoChatRealtime, ChatCapabilities { false }, NoAssistant)
    internal constructor(savedStateHandle: SavedStateHandle, repository: CommunicationRepository, outbox: MessageOutbox, realtime: ChatRealtime, capabilities: ChatCapabilities) :
        this(savedStateHandle, repository, outbox, realtime, capabilities, NoAssistant)

    private val conversationId = savedStateHandle.routeId()

    /** Sessão do assistente que ainda não tem conversa no messenger: o resto do identificador é o da sessão. */
    private val newSession = conversationId.startsWith(NEW_SESSION_PREFIX)

    /** Conversa no messenger; numa sessão nova só existe depois da primeira resposta. */
    private var remoteId: String? = conversationId.takeUnless { newSession }
    private var threadId: String? = if (newSession) conversationId.removePrefix(NEW_SESSION_PREFIX) else null
    private val assistantPending = MutableStateFlow<List<OutgoingMessage>>(emptyList())

    private fun remote(): String = remoteId ?: conversationId
    private fun isAssistant(): Boolean = (content.value as? UiState.Content)?.data?.conversation?.assistant == true
    private val draft = savedStateHandle.getStateFlow("draft", "")
    private val sendFailure = MutableStateFlow<ErrorKind?>(null)
    private val draftLimitExceeded = MutableStateFlow(false)
    private val content = MutableStateFlow<UiState<ChatContent>>(UiState.Loading)
    private val controls = MutableStateFlow(ChatControls(savedStateHandle["editing"], null, null, false))

    private val outgoing = outbox.observe(conversationId)
        .map { queued -> queued.map { OutgoingMessage(it.id, it.text, it.failure.takeIf { _ -> it.state == PendingState.FAILED }) } }
        .onStart { emit(emptyList()) }

    private val composer = combine(draft, sendFailure, draftLimitExceeded) { text, failure, exceeded -> Triple(text, failure, exceeded) }
    private val live = MutableStateFlow(LivePresence())
    private val myId = repository.currentUserId()

    private val allOutgoing = combine(outgoing, assistantPending) { queued, assistant -> queued + assistant }

    val uiState: StateFlow<UiState<ChatUiState>> = combine(content, composer, allOutgoing, controls, live) { loaded, (text, failure, exceeded), queued, control, presence ->
        when (loaded) {
            is UiState.Content -> {
                // O servidor só manda o `senderId`; o nome vem dos participantes da conversa, e o da própria mensagem (modo de demonstração) tem prioridade.
                val known = loaded.data.conversation.participantNames
                val messages = loaded.data.messages.map { message ->
                    val name = message.senderId?.let(known::get)
                    if (message.mine || !message.senderName.isNullOrBlank() || name == null) message else message.copy(senderName = name)
                }
                val names = known + messages.mapNotNull { message -> message.senderId?.let { id -> message.senderName?.takeIf(String::isNotBlank)?.let { id to it } } }
                // Participantes da conversa menos a própria conta, cruzados com a presença global dos contatos.
                val others = loaded.data.conversation.participants.filterNot { it == myId?.lowercase() }
                val onlineOthers = others.filter { it in presence.presence.online }
                UiState.Content(
                    ChatUiState(
                        loaded.data.conversation, text, messages, failure, exceeded, queued,
                        control.editingId, control.action, control.muted, control.archived, control.removed,
                        hasMore = loaded.data.hasMore, loadingOlder = presence.loadingOlder, firstUnreadSequence = loaded.data.firstUnreadSequence,
                        typingNames = presence.typing.mapNotNull { names[it] },
                        typingCount = presence.typing.size,
                        online = if (!presence.presence.known || loaded.data.conversation.group || others.isEmpty()) null else onlineOthers.isNotEmpty(),
                        onlineCount = onlineOthers.size,
                        attachmentsEnabled = capabilities.attachments() && !loaded.data.conversation.assistant,
                        assistantThinking = presence.thinking,
                    ),
                )
            }
            UiState.Loading -> UiState.Loading
            UiState.Empty -> UiState.Empty
            is UiState.Error -> loaded
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        load()
        viewModelScope.launch {
            outbox.delivered.filter { it.conversationId == conversationId }.collect { delivered ->
                val message = delivered.message
                if (message == null) refreshMessages() else appendMessage(message)
            }
        }
        if (!newSession) {
            // Um canal só traz as conversas todas; aqui ficam os eventos desta, e a presença e o "digitando" vêm do estado global.
            // O que passou com a conexão fora chega como `MessageRecovered`, recuperado pelo próprio tempo real; `Resynced` vale para todas.
            viewModelScope.launch { realtime.events.filter { it.conversationId() == conversationId || it is ChatEvent.Resynced }.collect(::onEvent) }
            viewModelScope.launch { realtime.presence.collect { state -> live.update { it.copy(presence = state) } } }
            viewModelScope.launch { realtime.typingByConversation.collect { all -> live.update { it.copy(typing = all[conversationId].orEmpty()) } } }
        }
    }

    fun load() {
        viewModelScope.launch {
            if (newSession) {
                content.value = UiState.Content(ChatContent(Conversation(conversationId, "", "", "", "", com.project.solaria_mobile.feature.communication.domain.AvatarTone.ORANGE, assistant = true, threadId = threadId), emptyList()))
                return@launch
            }
            content.value = UiState.Loading
            val conversation = repository.conversation(remote())
            val messages = repository.messages(remote())
            content.value = when {
                conversation is ApiResult.Failure -> UiState.Error(conversation.kind)
                messages is ApiResult.Failure -> UiState.Error(messages.kind)
                conversation is ApiResult.Success && messages is ApiResult.Success ->
                    UiState.Content(
                        ChatContent(
                            conversation.data, messages.data,
                            hasMore = messages.data.size >= PAGE_SIZE,
                            firstUnreadSequence = firstUnread(messages.data, conversation.data.unread),
                        ),
                    )
                else -> UiState.Error(ErrorKind.UNKNOWN)
            }
            (conversation as? ApiResult.Success)?.data?.threadId?.let { threadId = it }
            // O mesmo PATCH marca como lido até a última mensagem carregada e devolve o estado pessoal; se falhar, o menu não oferece silenciar.
            val lastRead = (messages as? ApiResult.Success)?.data?.maxOfOrNull { it.sequence }?.takeIf { it > 0 }
            (repository.conversationState(remote(), lastReadSequence = lastRead) as? ApiResult.Success)?.data?.let { state ->
                controls.value = controls.value.copy(muted = state.muted)
            }
        }
    }

    fun onDraftChange(value: String) {
        draftLimitExceeded.value = value.length > MAX_MESSAGE_LENGTH
        if (!draftLimitExceeded.value) savedStateHandle["draft"] = value
        if (value.isNotBlank() && controls.value.editingId == null && !isAssistant()) announceTyping() else stopTyping()
    }

    private var typingStopJob: Job? = null
    private var typingAnnouncedAt = 0L

    /** Avisa que está digitando; o servidor encerra o aviso sozinho em 5 s, então renova a cada 3 s e para depois de 4 s parado. */
    private fun announceTyping() {
        val now = System.currentTimeMillis()
        if (now - typingAnnouncedAt >= TYPING_RENEW_MILLIS) {
            typingAnnouncedAt = now
            viewModelScope.launch { realtime.typing(conversationId, true) }
        }
        typingStopJob?.cancel()
        typingStopJob = viewModelScope.launch { delay(TYPING_IDLE_MILLIS); stopTyping() }
    }

    private fun stopTyping() {
        typingStopJob?.cancel()
        if (typingAnnouncedAt == 0L) return
        typingAnnouncedAt = 0L
        viewModelScope.launch { realtime.typing(conversationId, false) }
    }

    /** Carrega o histórico anterior à mensagem mais antiga que já está na tela. */
    fun loadOlder() {
        val current = content.value as? UiState.Content ?: return
        if (!current.data.hasMore || live.value.loadingOlder) return
        val oldest = current.data.messages.firstOrNull { it.sequence > 0 }?.sequence ?: return
        live.value = live.value.copy(loadingOlder = true)
        viewModelScope.launch {
            val older = repository.olderMessages(remote(), oldest)
            val latest = content.value as? UiState.Content
            if (latest != null && older is ApiResult.Success) {
                val known = latest.data.messages.map { it.id }.toSet()
                val added = older.data.filter { it.id !in known }
                content.value = UiState.Content(latest.data.copy(messages = added + latest.data.messages, hasMore = older.data.size >= PAGE_SIZE))
            }
            live.value = live.value.copy(loadingOlder = false)
        }
    }

    /** Envia uma foto, um documento ou uma localização; só o modo de demonstração aceita, enquanto o messenger não tem contrato para isso. */
    fun sendAttachment(attachment: MessageAttachment) {
        sendFailure.value = null
        viewModelScope.launch {
            when (val result = repository.sendAttachment(remote(), attachment)) {
                is ApiResult.Success -> appendMessage(result.data)
                is ApiResult.Failure -> sendFailure.value = result.kind
            }
        }
    }

    private fun firstUnread(messages: List<ChatMessage>, unread: Int): Int? =
        if (unread <= 0) null else messages.filter { !it.mine && it.sequence > 0 }.takeLast(unread).firstOrNull()?.sequence

    private fun onEvent(event: ChatEvent) {
        when (event) {
            // Numa sessão do assistente a resposta vem no retorno do turno; o evento só duplicaria a mensagem.
            is ChatEvent.MessageCreated, is ChatEvent.MessageChanged -> if (!isAssistant()) onMessageEvent(event)
            is ChatEvent.MessageRecovered -> if (!isAssistant()) onRecovered(event.message)
            // O servidor perdeu a contagem de versões: o que está na tela pode estar velho, então lê o histórico de novo.
            is ChatEvent.Resynced -> if (!isAssistant()) refreshMessages()
            is ChatEvent.ConversationRemoved -> controls.value = controls.value.copy(removed = true)
            // Presença e "digitando" chegam pelo estado global do tempo real, não por este fluxo.
            else -> Unit
        }
    }

    private fun onMessageEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.MessageCreated -> {
                val message = event.message.toMessage(myId)
                val lastKnown = currentMessages().maxOfOrNull { it.sequence } ?: 0
                appendMessage(message)
                // Entre instâncias uma inserção pode ficar para trás: ela aparece como um pulo na sequência, e aí busca o que falta.
                if (hasSequenceGap(lastKnown, message.sequence)) catchUp(lastKnown)
                markRead(message)
            }
            is ChatEvent.MessageChanged -> replaceOrAppend(event.message.toMessage(myId))
            else -> Unit
        }
    }

    private fun onRecovered(dto: MessageDto) {
        val message = dto.toMessage(myId)
        replaceOrAppend(message)
        markRead(message)
    }

    /** Busca o que foi publicado depois de [after], quando uma mensagem ao vivo chegou com a sequência pulada. */
    private fun catchUp(after: Int) {
        viewModelScope.launch {
            val missed = repository.messagesSince(remote(), after) as? ApiResult.Success ?: return@launch
            missed.data.forEach(::replaceOrAppend)
        }
    }

    private fun markRead(message: ChatMessage) {
        if (!message.mine && message.sequence > 0) scheduleRead(message.sequence)
    }

    private var readJob: Job? = null
    private var pendingRead = 0

    /**
     * Junta uma rajada de mensagens numa leitura só: espera um instante sem novidade e marca como lida a maior sequência vista. Se a
     * tela fechar nesse instante, a marcação se perde e a lista mostra a mensagem como não lida até a próxima abertura.
     */
    private fun scheduleRead(sequence: Int) {
        pendingRead = maxOf(pendingRead, sequence)
        readJob?.cancel()
        readJob = viewModelScope.launch {
            delay(READ_DEBOUNCE_MILLIS)
            repository.conversationState(remote(), lastReadSequence = pendingRead)
        }
    }

    /** Guarda a mensagem na fila do aparelho e limpa o campo; a fila envia assim que houver conexão. Em edição, salva a mensagem. */
    fun send() {
        stopTyping()
        val text = draft.value.trim()
        if (text.isBlank() || draftLimitExceeded.value || text.length > MAX_MESSAGE_LENGTH) return
        controls.value.editingId?.let { id -> saveEdit(id, text); return }
        sendFailure.value = null
        savedStateHandle["draft"] = ""
        if (isAssistant()) { askAssistant(text); return }
        viewModelScope.launch {
            if (!outbox.enqueue(conversationId, text)) {
                savedStateHandle["draft"] = text
                sendFailure.value = ErrorKind.UNAUTHORIZED
            }
        }
    }

    /** Leva o texto da própria mensagem para o campo; o rascunho em andamento volta ao cancelar. */
    fun startEdit(id: String) {
        val message = currentMessages().firstOrNull { it.id == id && it.mine && !it.deleted } ?: return
        if (controls.value.editingId == null) savedStateHandle["draftBeforeEdit"] = draft.value
        savedStateHandle["editing"] = id
        savedStateHandle["draft"] = message.text
        draftLimitExceeded.value = false
        controls.value = controls.value.copy(editingId = id, action = null)
    }

    fun cancelEdit() {
        savedStateHandle["draft"] = savedStateHandle.get<String>("draftBeforeEdit").orEmpty()
        finishEdit()
    }

    fun deleteMessage(id: String) {
        if (controls.value.action is ChatAction.Working) return
        if (currentMessages().none { it.id == id && it.mine && !it.deleted }) return
        run(ChatOperation.DELETE) {
            when (val result = repository.deleteMessage(id)) {
                // Só some da conversa o que o servidor confirmou como excluído.
                is ApiResult.Success -> { replaceMessage(result.data); if (controls.value.editingId == id) cancelEdit(); null }
                is ApiResult.Failure -> result.kind
            }
        }
    }

    fun setMuted(muted: Boolean) {
        run(if (muted) ChatOperation.MUTE else ChatOperation.UNMUTE) {
            when (val result = repository.conversationState(remote(), muted = muted)) {
                is ApiResult.Success -> { controls.value = controls.value.copy(muted = result.data.muted); null }
                is ApiResult.Failure -> result.kind
            }
        }
    }

    /** Arquivar vale só para esta conta; os outros participantes continuam com a conversa. */
    fun archive() {
        run(ChatOperation.ARCHIVE) {
            when (val result = repository.conversationState(remote(), archived = true)) {
                is ApiResult.Success -> { controls.value = controls.value.copy(archived = result.data.archived); null }
                is ApiResult.Failure -> result.kind
            }
        }
    }

    fun clearAction() {
        if (controls.value.action is ChatAction.Failed) controls.value = controls.value.copy(action = null)
    }

    fun retry(id: String) {
        assistantPending.value.firstOrNull { it.id == id }?.let { askAssistant(it.text, id); return }
        viewModelScope.launch { outbox.retry(id) }
    }

    fun discard(id: String) {
        if (assistantPending.value.any { it.id == id }) {
            assistantPending.value = assistantPending.value.filterNot { it.id == id }
            return
        }
        viewModelScope.launch { outbox.discard(id) }
    }

    /**
     * Envia a mensagem à sessão pelo ai-assistant (e não pelo messenger, que só guardaria a pergunta sem ninguém para responder).
     * Enquanto o turno roda, a mensagem fica na fila local e o assistente aparece "pensando"; a resposta vem no próprio retorno.
     */
    private fun askAssistant(text: String, id: String = java.util.UUID.randomUUID().toString()) {
        val thread = threadId ?: return
        assistantPending.value = assistantPending.value.filterNot { it.id == id } + OutgoingMessage(id, text, null)
        live.value = live.value.copy(thinking = true)
        viewModelScope.launch {
            when (val result = assistantRepository.send(thread, text)) {
                is ApiResult.Success -> {
                    assistantPending.value = assistantPending.value.filterNot { it.id == id }
                    result.data.messengerConversationId?.let { remoteId = it }
                    showAssistantTurn(text, result.data)
                }
                is ApiResult.Failure -> assistantPending.value = assistantPending.value.map { if (it.id == id) it.copy(failure = result.kind) else it }
            }
            live.value = live.value.copy(thinking = assistantPending.value.any { it.failure == null })
        }
    }

    /** Mostra a pergunta e a resposta na hora e depois troca pelas mensagens gravadas no messenger, com os ids e horários reais. */
    private suspend fun showAssistantTurn(text: String, reply: AssistantReply) {
        val now = java.time.OffsetDateTime.now()
        appendMessage(ChatMessage(text, true, "", id = "local-${java.util.UUID.randomUUID()}", sentAt = now, senderId = myId))
        appendMessage(ChatMessage(reply.text, false, "", id = "local-${java.util.UUID.randomUUID()}", sentAt = now))
        val id = remoteId ?: return
        val messages = (repository.messages(id) as? ApiResult.Success)?.data?.takeIf { it.isNotEmpty() } ?: return
        val conversation = (repository.conversation(id) as? ApiResult.Success)?.data
        val current = content.value as? UiState.Content ?: return
        content.value = UiState.Content(current.data.copy(conversation = conversation ?: current.data.conversation, messages = messages, hasMore = messages.size >= PAGE_SIZE))
    }

    private fun saveEdit(id: String, text: String) {
        if (controls.value.action is ChatAction.Working) return
        val original = currentMessages().firstOrNull { it.id == id }
        if (original == null || original.deleted) { cancelEdit(); return }
        if (original.text == text) { cancelEdit(); return }
        run(ChatOperation.EDIT) {
            when (val result = repository.editMessage(id, text)) {
                is ApiResult.Success -> {
                    replaceMessage(result.data)
                    savedStateHandle["draft"] = savedStateHandle.get<String>("draftBeforeEdit").orEmpty()
                    finishEdit()
                    null
                }
                // O texto editado continua no campo para nova tentativa.
                is ApiResult.Failure -> result.kind
            }
        }
    }

    private fun run(operation: ChatOperation, block: suspend () -> ErrorKind?) {
        if (controls.value.action is ChatAction.Working) return
        controls.value = controls.value.copy(action = ChatAction.Working(operation))
        viewModelScope.launch {
            val failure = block()
            controls.value = controls.value.copy(action = failure?.let { ChatAction.Failed(operation, it) })
        }
    }

    private fun finishEdit() {
        savedStateHandle.remove<String>("editing")
        savedStateHandle.remove<String>("draftBeforeEdit")
        controls.value = controls.value.copy(editingId = null)
    }

    private fun currentMessages(): List<ChatMessage> = (content.value as? UiState.Content)?.data?.messages.orEmpty()

    private fun replaceMessage(updated: ChatMessage) {
        val current = content.value as? UiState.Content ?: return
        content.value = current.copy(data = current.data.copy(messages = current.data.messages.map { if (it.id == updated.id) updated else it }))
    }

    /** A mensagem própria chega pelo envio e pelo evento em tempo real; vale a primeira, pelo identificador do servidor. */
    private fun appendMessage(message: ChatMessage) {
        val current = content.value as? UiState.Content ?: return
        if (message.id.isNotBlank() && current.data.messages.any { it.id == message.id }) return
        content.value = UiState.Content(current.data.copy(messages = current.data.messages + message))
    }

    private fun replaceOrAppend(message: ChatMessage) {
        if (currentMessages().any { it.id == message.id }) replaceMessage(message) else appendMessage(message)
    }

    /** Recarrega o histórico sem voltar ao esqueleto, quando a entrega foi confirmada pelo próprio histórico. */
    private fun refreshMessages() {
        viewModelScope.launch {
            val current = content.value as? UiState.Content ?: return@launch
            val messages = repository.messages(remote()) as? ApiResult.Success ?: return@launch
            content.value = UiState.Content(current.data.copy(messages = messages.data, hasMore = messages.data.size >= PAGE_SIZE))
        }
    }
}

private const val TYPING_RENEW_MILLIS = 3_000L
private const val TYPING_IDLE_MILLIS = 4_000L
private const val READ_DEBOUNCE_MILLIS = 500L

/** Texto curto para o resultado de uma ação da conversa. */
internal fun ChatAction.message(resources: android.content.res.Resources): String = when (this) {
    is ChatAction.Working -> when (operation) {
        ChatOperation.EDIT -> resources.getString(R.string.cm_salvando_edicao)
        ChatOperation.DELETE -> resources.getString(R.string.cm_excluindo_mensagem)
        ChatOperation.MUTE -> resources.getString(R.string.cm_silenciando_conversa)
        ChatOperation.UNMUTE -> resources.getString(R.string.cm_reativando_notificacoes)
        ChatOperation.ARCHIVE -> resources.getString(R.string.cm_arquivando_conversa)
    }
    is ChatAction.Failed -> when (kind) {
        ErrorKind.FORBIDDEN -> resources.getString(R.string.cm_voce_nao_tem_permissao_para)
        ErrorKind.NOT_FOUND -> resources.getString(R.string.cm_a_mensagem_ou_conversa_nao)
        ErrorKind.CONNECTIVITY -> resources.getString(R.string.cm_sem_conexao_nada_foi_alterado)
        else -> when (operation) {
            ChatOperation.EDIT -> resources.getString(R.string.cm_nao_foi_possivel_salvar_a)
            ChatOperation.DELETE -> resources.getString(R.string.cm_nao_foi_possivel_excluir_a)
            ChatOperation.MUTE, ChatOperation.UNMUTE -> resources.getString(R.string.cm_nao_foi_possivel_alterar_as)
            ChatOperation.ARCHIVE -> resources.getString(R.string.cm_nao_foi_possivel_arquivar_a)
        }
    }
}

@androidx.compose.runtime.Composable
internal fun ChatAction.message(): String = message(androidx.compose.ui.platform.LocalContext.current.resources)

/** Sem assistente: usado pelos testes que não tratam sessões. */
private object NoAssistant : AssistantRepository {
    override suspend fun send(threadId: String, text: String): ApiResult<AssistantReply> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
}
