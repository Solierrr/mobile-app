package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationsUiState(val query: String = "", val conversations: List<Conversation> = emptyList())

@HiltViewModel
class ConversationsViewModel @Inject constructor(private val repository: CommunicationRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<UiState<ConversationsUiState>> = combine(
        repository.conversations(),
        repository.syncResult,
        query,
    ) { items, sync, text ->
        when {
            items.isNotEmpty() -> UiState.Content(
                ConversationsUiState(text, items.filter {
                    it.name.contains(text, ignoreCase = true) || it.preview.contains(text, ignoreCase = true)
                })
            )
            sync is ApiResult.Failure -> UiState.Error(sync.kind)
            sync is ApiResult.Success -> UiState.Empty
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        viewModelScope.launch { repository.refreshNames() }
    }

    fun load() = repository.refresh()

    fun onQueryChange(value: String) { query.value = value }
}
