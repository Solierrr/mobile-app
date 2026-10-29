package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.toUiState
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
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

data class ChatUiState(
    val conversation: Conversation,
    val draft: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val online: Boolean = true,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CommunicationRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.routeId()
    private val draft = MutableStateFlow("")
    private val opened = MutableStateFlow<UiState<Unit>>(UiState.Loading)

    val uiState: StateFlow<UiState<ChatUiState>> = combine(
        opened,
        repository.conversation(conversationId),
        repository.messages(conversationId),
        draft,
        repository.online,
    ) { open, conversation, messages, text, online ->
        when {
            open is UiState.Error -> open
            conversation != null -> UiState.Content(ChatUiState(conversation, text, messages, online))
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

    fun load() {
        viewModelScope.launch {
            opened.value = UiState.Loading
            opened.value = repository.open(conversationId).toUiState()
        }
    }

    fun onDraftChange(value: String) { draft.value = value }

    fun send() {
        val text = draft.value
        if (text.isBlank()) return
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

    /** Silencia a conversa, ou reativa se já está silenciada. Só com internet. */
    fun toggleMute() {
        val current = (uiState.value as? UiState.Content)?.data?.conversation ?: return
        viewModelScope.launch { repository.setMuted(conversationId, !current.muted) }
    }

    /** Arquiva a conversa, ou tira do arquivo se já está arquivada. Só com internet. */
    fun toggleArchive() {
        val current = (uiState.value as? UiState.Content)?.data?.conversation ?: return
        viewModelScope.launch { repository.setArchived(conversationId, !current.archived) }
    }
}
