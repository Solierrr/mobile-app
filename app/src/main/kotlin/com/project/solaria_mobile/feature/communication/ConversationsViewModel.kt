package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationsUiState(val query: String = "", val conversations: List<Conversation> = emptyList())

/** Lista de conversas; [assistant] escolhe entre as conversas com pessoas e as sessões do assistente. */
open class ConversationListViewModel(private val repository: CommunicationRepository, private val assistant: Boolean, geoSessions: kotlinx.coroutines.flow.Flow<List<Conversation>> = kotlinx.coroutines.flow.flowOf(emptyList())) : ViewModel() {
    private val query = MutableStateFlow("")
    private val source = MutableStateFlow<UiState<List<Conversation>>>(UiState.Loading)

    val uiState: StateFlow<UiState<ConversationsUiState>> = combine(source, query, geoSessions) { loaded, text, geo ->
        loaded.map { items ->
            ConversationsUiState(text, (items.filter { it.assistant == assistant } + if (assistant) geo else emptyList()).filter {
                it.name.contains(text, ignoreCase = true) || it.preview.contains(text, ignoreCase = true)
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch { source.load() { repository.conversations() } }
    }

    fun onQueryChange(value: String) { query.value = value }
}

@HiltViewModel
class ConversationsViewModel @Inject constructor(repository: CommunicationRepository) : ConversationListViewModel(repository, assistant = false)

@HiltViewModel
class AssistantSessionsViewModel @Inject constructor(repository: CommunicationRepository, geo: com.project.solaria_mobile.feature.discovery.GeoSessionStore) :
    ConversationListViewModel(repository, assistant = true, geoSessions = geo.sessions.flowMap { list -> list.map { geoConversation(it) } })

/** A sessão do assistente geográfico como uma linha da lista de sessões, com o projeto no lugar do título. */
private fun geoConversation(session: com.project.solaria_mobile.feature.discovery.GeoSession): Conversation = Conversation(
    id = "geo:${session.projectId}:${session.id}",
    initials = "", name = session.projectName,
    preview = session.messages.lastOrNull()?.text?.replace(Regex("[*_`#>~]"), "")?.replace(Regex("\\s+"), " ")?.trim().orEmpty(),
    time = com.project.solaria_mobile.core.format.formatTimestamp(session.updatedAt.toInstant().toString()),
    tone = com.project.solaria_mobile.feature.communication.domain.AvatarTone.ORANGE, assistant = true, geo = true,
)
