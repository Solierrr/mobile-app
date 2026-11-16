package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
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
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversation: Conversation,
    val draft: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val sendFailure: ErrorKind? = null,
    val sending: Boolean = false,
)

private data class ChatContent(val conversation: Conversation, val messages: List<ChatMessage>)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CommunicationRepository,
) : ViewModel() {
    private val conversationId = savedStateHandle.routeId()
    private val draft = MutableStateFlow("")
    private val sendFailure = MutableStateFlow<ErrorKind?>(null)
    private val sending = MutableStateFlow(false)
    private val content = MutableStateFlow<UiState<ChatContent>>(UiState.Loading)

    val uiState: StateFlow<UiState<ChatUiState>> = combine(content, draft, sendFailure, sending) { loaded, text, failure, inProgress ->
        when (loaded) {
            is UiState.Content -> UiState.Content(ChatUiState(loaded.data.conversation, text, loaded.data.messages, failure, inProgress))
            UiState.Loading -> UiState.Loading
            UiState.Empty -> UiState.Empty
            is UiState.Error -> loaded
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch {
            content.value = UiState.Loading
            val conversation = repository.conversation(conversationId)
            val messages = repository.messages(conversationId)
            content.value = when {
                conversation is ApiResult.Failure -> UiState.Error(conversation.kind)
                messages is ApiResult.Failure -> UiState.Error(messages.kind)
                conversation is ApiResult.Success && messages is ApiResult.Success ->
                    UiState.Content(ChatContent(conversation.data, messages.data))
                else -> UiState.Error(ErrorKind.UNKNOWN)
            }
        }
    }

    fun onDraftChange(value: String) { draft.value = value }

    fun send() {
        val text = draft.value.trim()
        if (text.isBlank() || sending.value) return
        sending.value = true
        sendFailure.value = null
        viewModelScope.launch {
            try {
                when (val result = repository.sendMessage(conversationId, text)) {
                    is ApiResult.Failure -> sendFailure.value = result.kind
                    is ApiResult.Success -> {
                        val current = content.value as? UiState.Content ?: return@launch
                        content.value = UiState.Content(current.data.copy(messages = current.data.messages + result.data))
                        if (draft.value.trim() == text) draft.value = ""
                    }
                }
            } finally { sending.value = false }
        }
    }
}
