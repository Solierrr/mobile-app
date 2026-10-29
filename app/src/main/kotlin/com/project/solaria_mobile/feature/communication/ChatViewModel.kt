package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.toUiState
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.ChatPresence
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.canChange
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Aviso curto que a tela mostra depois de uma ação que o servidor recusou ou que falhou.
 *
 * A tela traduz cada valor num texto e chama [ChatViewModel.dismissNotice] quando o usuário o fecha.
 */
enum class ChatNotice {
    /** Sem internet: editar e excluir só funcionam online. */
    OFFLINE,

    /** O servidor recusou a mudança: prazo de 30 minutos vencido ou mensagem já excluída. */
    NOT_ALLOWED,

    /** Alguém mudou a mensagem ao mesmo tempo (409). Tentar de novo costuma resolver. */
    TRY_AGAIN,

    /** A mensagem ou a conversa não está mais acessível (403 ou 404). */
    NO_ACCESS,

    /** Qualquer outra falha. */
    FAILED,
}

/**
 * Estado do chat.
 *
 * @property conversation conversa aberta.
 * @property draft texto do campo de mensagem. Em modo de edição, é o texto novo da mensagem.
 * @property messages mensagens confirmadas e pendentes, já com [ChatMessage.changeable] calculado.
 * @property online `true` se há internet.
 * @property editing mensagem em edição, ou `null`. Enquanto existe, o envio do campo de texto edita em vez de enviar.
 * @property notice aviso a mostrar, ou `null`.
 * @property presence presença do outro participante e quem está digitando.
 */
data class ChatUiState(
    val conversation: Conversation,
    val draft: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val online: Boolean = true,
    val editing: ChatMessage? = null,
    val notice: ChatNotice? = null,
    val presence: ChatPresence = ChatPresence(),
)

private data class Live(
    val conversation: Conversation?,
    val messages: List<ChatMessage>,
    val online: Boolean,
    val presence: ChatPresence,
)

private data class Local(
    val opened: UiState<Unit>,
    val draft: String,
    val editing: ChatMessage?,
    val notice: ChatNotice?,
)

/**
 * ViewModel do chat de uma conversa.
 *
 * Além de ler e enviar, marca a conversa como lida enquanto a tela está aberta, edita e exclui mensagens próprias
 * (só com internet e até 30 minutos depois do envio), e silencia e arquiva a conversa.
 *
 * Depende de:
 * - [CommunicationRepository] -> conversa, mensagens e as ações.
 *
 * Principais operações:
 * - [send] -> envia a mensagem, ou grava a edição quando há uma mensagem em edição.
 * - [startEdit] | [cancelEdit] -> entra e sai do modo de edição.
 * - [delete] -> exclui uma mensagem própria para todos.
 * - [toggleMute] | [toggleArchive] -> silencia ou arquiva a conversa.
 * - [retry] | [discard] -> reenvia ou descarta uma mensagem que não saiu.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CommunicationRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.routeId()
    private val draft = MutableStateFlow("")
    private val opened = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    private val editing = MutableStateFlow<ChatMessage?>(null)
    private val notice = MutableStateFlow<ChatNotice?>(null)

    // Relógio da janela de edição. Só os testes trocam.
    internal var now: () -> Long = System::currentTimeMillis

    private val live = combine(
        repository.conversation(conversationId),
        repository.messages(conversationId),
        repository.online,
        repository.presence(conversationId),
    ) { conversation, messages, online, presence -> Live(conversation, messages, online, presence) }

    private val local = combine(opened, draft, editing, notice) { open, text, edited, aviso -> Local(open, text, edited, aviso) }

    val uiState: StateFlow<UiState<ChatUiState>> = combine(live, local) { data, state ->
        val conversation = data.conversation
        when {
            state.opened is UiState.Error -> state.opened
            conversation != null -> UiState.Content(
                ChatUiState(
                    conversation = conversation,
                    draft = state.draft,
                    messages = data.messages.map { it.copy(changeable = canChange(it, conversation, data.online, now())) },
                    online = data.online,
                    editing = state.editing,
                    notice = state.notice,
                    presence = data.presence,
                ),
            )
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        load()
        viewModelScope.launch { repository.refreshNames(conversationId) }
        // Cada mensagem confirmada nova, vista com o chat aberto, avança a leitura. Funciona offline: o Room guarda e o
        // envio ao servidor sai quando a internet volta.
        viewModelScope.launch {
            repository.messages(conversationId)
                .map { list -> list.maxOfOrNull { it.sequence ?: 0 } ?: 0 }
                .distinctUntilChanged()
                .collect { if (it > 0) repository.markRead(conversationId) }
        }
    }

    override fun onCleared() {
        repository.stopTyping(conversationId)
    }

    fun load() {
        viewModelScope.launch {
            opened.value = UiState.Loading
            opened.value = repository.open(conversationId).toUiState()
        }
    }

    fun onDraftChange(value: String) {
        draft.value = value
        if (value.isNotEmpty() && editing.value == null && canSignalTyping()) {
            repository.typing(conversationId)
        } else {
            repository.stopTyping(conversationId)
        }
    }

    fun send() {
        val text = draft.value
        if (text.isBlank()) return
        repository.stopTyping(conversationId)
        val target = editing.value
        if (target != null) {
            saveEdit(target, text)
            return
        }
        draft.value = ""
        viewModelScope.launch {
            val result = repository.sendMessage(conversationId, text)
            if (result is ApiResult.Failure && draft.value.isEmpty()) draft.value = text
        }
    }

    fun retry(messageId: String) {
        viewModelScope.launch { repository.retryMessage(messageId) }
    }

    fun discard(messageId: String) {
        viewModelScope.launch { repository.discardMessage(messageId) }
    }

    /**
     * Entra no modo de edição com o texto da mensagem no campo. Ignora mensagem que não pode mais ser alterada.
     *
     * @param messageId id da mensagem, o mesmo de [ChatMessage.id].
     */
    fun startEdit(messageId: String) {
        val message = currentMessages().firstOrNull { it.id == messageId && it.changeable } ?: return
        editing.value = message
        draft.value = message.text
        repository.stopTyping(conversationId)
    }

    /** Sai do modo de edição e limpa o campo de texto. */
    fun cancelEdit() {
        editing.value = null
        draft.value = ""
    }

    /**
     * Exclui a mensagem para todos os participantes.
     *
     * A tela deve pedir confirmação antes. Se o servidor recusar, mostra o [ChatNotice] correspondente.
     *
     * @param messageId id da mensagem, o mesmo de [ChatMessage.id].
     */
    fun delete(messageId: String) {
        if (currentMessages().none { it.id == messageId && it.changeable }) return
        viewModelScope.launch {
            val result = repository.deleteMessage(messageId)
            if (result is ApiResult.Failure) notice.value = noticeOf(result.kind)
        }
    }

    /** Silencia a conversa, ou reativa se já está silenciada. Só com internet. */
    fun toggleMute() {
        val current = currentConversation() ?: return
        viewModelScope.launch {
            val result = repository.setMuted(conversationId, !current.muted)
            if (result is ApiResult.Failure) notice.value = noticeOf(result.kind)
        }
    }

    /** Arquiva a conversa, ou tira do arquivo se já está arquivada. Só com internet. */
    fun toggleArchive() {
        val current = currentConversation() ?: return
        viewModelScope.launch {
            val result = repository.setArchived(conversationId, !current.archived)
            if (result is ApiResult.Failure) notice.value = noticeOf(result.kind)
        }
    }

    /** Fecha o aviso atual. */
    fun dismissNotice() {
        notice.value = null
    }

    private fun saveEdit(target: ChatMessage, text: String) {
        viewModelScope.launch {
            when (val result = repository.editMessage(target.id, text)) {
                is ApiResult.Success -> {
                    editing.value = null
                    draft.value = ""
                }
                // Mantém o texto no campo para o usuário tentar de novo ou copiar
                is ApiResult.Failure -> notice.value = noticeOf(result.kind)
            }
        }
    }

    // Conversa com o chatbot ou sem acesso não tem "digitando" para os outros
    private fun canSignalTyping(): Boolean = currentConversation()?.let { !it.chatbot && !it.accessLost } ?: false

    private fun currentConversation(): Conversation? = (uiState.value as? UiState.Content)?.data?.conversation

    private fun currentMessages(): List<ChatMessage> = (uiState.value as? UiState.Content)?.data?.messages.orEmpty()

    private fun noticeOf(kind: ErrorKind): ChatNotice = when (kind) {
        ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT -> ChatNotice.OFFLINE
        ErrorKind.VALIDATION -> ChatNotice.NOT_ALLOWED
        ErrorKind.CONFLICT -> ChatNotice.TRY_AGAIN
        ErrorKind.FORBIDDEN, ErrorKind.NOT_FOUND -> ChatNotice.NO_ACCESS
        else -> ChatNotice.FAILED
    }
}
