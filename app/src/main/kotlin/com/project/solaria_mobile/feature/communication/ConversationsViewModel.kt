package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.data.Conversation
import com.project.solaria_mobile.feature.communication.data.CommunicationRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConversationsUiState(val query: String = "", val conversations: List<Conversation> = emptyList())

@HiltViewModel
class ConversationsViewModel @Inject constructor(repository: CommunicationRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val uiState: StateFlow<ConversationsUiState> = combine(repository.conversations, query) { items, text ->
        ConversationsUiState(text, (items + items).filter {
            it.name.contains(text, ignoreCase = true) || it.preview.contains(text, ignoreCase = true)
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationsUiState(conversations = repository.conversations.value + repository.conversations.value))

    fun onQueryChange(value: String) { query.value = value }
}
