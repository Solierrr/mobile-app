package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChatUiState(val draft: String = "", val messages: List<ChatMessage> = emptyList())

@HiltViewModel
class ChatViewModel @Inject constructor(private val repository: CommunicationRepository) : ViewModel() {
    private val draft = MutableStateFlow("")
    val uiState: StateFlow<ChatUiState> = combine(repository.messages, draft) { messages, text ->
        ChatUiState(text, messages)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState(messages = repository.messages.value))

    fun onDraftChange(value: String) { draft.value = value }
    fun send() {
        repository.sendMessage(draft.value)
        if (draft.value.isNotBlank()) draft.value = ""
    }
}
