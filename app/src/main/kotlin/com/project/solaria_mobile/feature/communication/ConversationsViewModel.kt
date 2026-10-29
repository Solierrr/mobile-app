package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da lista de conversas.
 *
 * @property query texto da busca.
 * @property conversations conversas que passam pela busca, com [Conversation.unreadCount] e [Conversation.muted].
 * @property showArchived `true` quando a lista mostra as arquivadas em vez das ativas.
 */
data class ConversationsUiState(
    val query: String = "",
    val conversations: List<Conversation> = emptyList(),
    val showArchived: Boolean = false,
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(private val repository: CommunicationRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val showArchived = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val source = showArchived.flatMapLatest { archived -> repository.conversations(archived) }

    val uiState: StateFlow<UiState<ConversationsUiState>> = combine(
        source,
        repository.syncResult,
        query,
        showArchived,
    ) { items, sync, text, archived ->
        when {
            items.isNotEmpty() || archived -> UiState.Content(
                ConversationsUiState(
                    query = text,
                    conversations = items.filter {
                        it.name.contains(text, ignoreCase = true) || it.preview.contains(text, ignoreCase = true)
                    },
                    showArchived = archived,
                ),
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

    /** Alterna entre a lista de conversas ativas e a de arquivadas. */
    fun toggleArchived() { showArchived.value = !showArchived.value }
}
