package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.common.onSuccess
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    private val conversation = MutableStateFlow<UiState<Conversation>>(UiState.Loading)

    private val delivered = MutableStateFlow<Map<String, ChatMessage>>(emptyMap())

    private val timeline = combine(delivered, repository.pendingMessages(conversationId)) { sent, pending ->
        timelineOf(sent.values, pending)
    }

    val uiState: StateFlow<UiState<ChatUiState>> =
        combine(conversation, timeline, draft, repository.online) { loaded, messages, text, online ->
            loaded.map { ChatUiState(it, text, messages, online) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        load()
        viewModelScope.launch { repository.liveMessages(conversationId).collect { merge(listOf(it)) } }
        viewModelScope.launch { repository.reconnections().collect { catchUp() } }
    }

    fun load() {
        viewModelScope.launch {
            conversation.value = UiState.Loading
            val loaded = repository.conversation(conversationId)
            val messages = repository.messages(conversationId)
            conversation.value = when {
                loaded is ApiResult.Failure -> UiState.Error(loaded.kind)
                messages is ApiResult.Failure -> UiState.Error(messages.kind)
                loaded is ApiResult.Success && messages is ApiResult.Success -> {
                    merge(messages.data)
                    UiState.Content(loaded.data)
                }
                else -> UiState.Error(ErrorKind.UNKNOWN)
            }
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

    private suspend fun catchUp() {
        if (conversation.value !is UiState.Content) return
        val lastSequence = delivered.value.values.maxOfOrNull { it.sequence ?: 0 } ?: 0
        repository.messages(conversationId, afterSequence = lastSequence).onSuccess(::merge)
    }

    private fun merge(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        delivered.update { current -> current + messages.associateBy { it.id } }
    }
}

internal fun timelineOf(delivered: Collection<ChatMessage>, pending: List<ChatMessage>): List<ChatMessage> {
    val sent = delivered.sortedBy { it.sequence ?: Int.MAX_VALUE }
    val confirmed = sent.mapNotNullTo(HashSet()) { it.clientMessageId }
    return sent + pending.filterNot { it.clientMessageId in confirmed }
}
