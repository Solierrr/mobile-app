package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.NoChatRealtime
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val query: String = "",
    val conversations: List<Conversation> = emptyList(),
    /** Conversas diretas cuja outra pessoa está online; vazio enquanto a presença não é conhecida. */
    val online: Set<String> = emptySet(),
    /** Conversas em que alguém está digitando agora. */
    val typing: Set<String> = emptySet(),
)

/** Lista de conversas; [assistant] escolhe entre as conversas com pessoas e as sessões do assistente. */
open class ConversationListViewModel(
    private val repository: CommunicationRepository,
    private val assistant: Boolean,
    geoSessions: kotlinx.coroutines.flow.Flow<List<Conversation>> = kotlinx.coroutines.flow.flowOf(emptyList()),
    private val realtime: ChatRealtime = NoChatRealtime,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val source = MutableStateFlow<UiState<List<Conversation>>>(UiState.Loading)
    private val myId = repository.currentUserId()?.lowercase()
    private var connected = false

    val uiState: StateFlow<UiState<ConversationsUiState>> = combine(source, query, geoSessions, realtime.presence, realtime.typingByConversation) { loaded, text, geo, presence, typing ->
        loaded.map { items ->
            val conversations = (items.filter { it.assistant == assistant } + if (assistant) geo else emptyList()).filter {
                it.name.contains(text, ignoreCase = true) || it.preview.contains(text, ignoreCase = true)
            }
            ConversationsUiState(
                text, conversations,
                // Conversa direta: a outra pessoa é o participante que não é a própria conta.
                online = if (!presence.known) emptySet() else conversations.filter { c -> !c.group && !c.assistant && c.participants.any { it != myId && it in presence.online } }.map { it.id }.toSet(),
                typing = conversations.filter { !typing[it.id].isNullOrEmpty() }.map { it.id }.toSet(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        load()
        // Um canal só traz as conversas todas: a lista muda sem recarregar. Um evento que chega durante uma carga pode ser
        // desfeito por ela; o próximo evento ou a próxima carga corrige.
        viewModelScope.launch {
            realtime.events.collect { event ->
                // A contagem de versões do servidor recomeçou: o que a lista mostra pode estar velho, então lê tudo de novo.
                if (event is ChatEvent.Resynced) load()
                else source.update { state -> if (state is UiState.Content) state.copy(data = state.data.applyLive(event, myId)) else state }
            }
        }
        viewModelScope.launch {
            // O que passou enquanto a conexão estava fora não chega depois; a lista é lida de novo quando ela volta.
            var wasDown = false
            realtime.connected.collect { up ->
                connected = up
                if (!up) wasDown = true else if (wasDown) { wasDown = false; load() }
            }
        }
    }

    fun load() {
        viewModelScope.launch { source.load() { repository.conversations() } }
    }

    fun onQueryChange(value: String) { query.value = value }

    /** Ao voltar para a lista só lê de novo se o tempo real estava fora; com ele ligado a lista já está em dia. */
    fun onResume() { if (!connected) load() }
}

@HiltViewModel
class ConversationsViewModel @Inject constructor(repository: CommunicationRepository, realtime: ChatRealtime) :
    ConversationListViewModel(repository, assistant = false, realtime = realtime) {
    internal constructor(repository: CommunicationRepository) : this(repository, NoChatRealtime)
}

@HiltViewModel
class AssistantSessionsViewModel @Inject constructor(repository: CommunicationRepository, geo: com.project.solaria_mobile.feature.discovery.GeoSessionStore, realtime: ChatRealtime) :
    ConversationListViewModel(repository, assistant = true, geoSessions = geo.sessions.flowMap { list -> list.map { geoConversation(it) } }, realtime = realtime)

/** A sessão do assistente geográfico como uma linha da lista de sessões, com o projeto no lugar do título. */
private fun geoConversation(session: com.project.solaria_mobile.feature.discovery.GeoSession): Conversation = Conversation(
    id = "geo:${session.projectId}:${session.id}",
    initials = "", name = session.projectName,
    preview = session.messages.lastOrNull()?.text?.replace(Regex("[*_`#>~]"), "")?.replace(Regex("\\s+"), " ")?.trim().orEmpty(),
    time = com.project.solaria_mobile.core.format.formatTimestamp(session.updatedAt.toInstant().toString()),
    tone = com.project.solaria_mobile.feature.communication.domain.AvatarTone.ORANGE, assistant = true, geo = true,
)
