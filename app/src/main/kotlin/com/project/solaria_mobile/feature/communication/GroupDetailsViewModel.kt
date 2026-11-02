package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** [name] é nulo enquanto o api-core não devolveu o nome daquela pessoa. */
data class GroupMember(val authId: String, val name: String?, val manager: Boolean, val self: Boolean)

data class GroupDetailsUi(
    val conversation: Conversation,
    val members: List<GroupMember>,
    /** A conta é o gestor: só ela adiciona, remove e passa a gestão. */
    val canManage: Boolean,
    val busy: Boolean = false,
    val failure: ApiResult.Failure? = null,
    /** Convidados que o servidor recusou na última adição. */
    val rejected: Int = 0,
    /** A conta saiu do grupo: a tela fecha. */
    val left: Boolean = false,
) {
    /** Quem pode assumir a gestão: qualquer participante que não seja a própria conta. */
    val successors: List<GroupMember> get() = members.filterNot { it.self }
}

/** Participantes de um grupo e as ações de gestão: adicionar, remover, passar a gestão e sair. */
@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CommunicationRepository,
) : ViewModel() {
    private val id = savedStateHandle.routeId()
    private val me = repository.currentUserId()?.lowercase()
    private val mutable = MutableStateFlow<UiState<GroupDetailsUi>>(UiState.Loading)
    val uiState: StateFlow<UiState<GroupDetailsUi>> = mutable

    init { load() }

    fun load() {
        viewModelScope.launch {
            // Uma operação em andamento e o aviso da última ficam por cima do que a leitura trouxe.
            val previous = (mutable.value as? UiState.Content)?.data
            mutable.load { repository.conversation(id).map(::details) }
            if (previous != null) update { it.copy(failure = previous.failure, rejected = previous.rejected) }
        }
    }

    fun add(userIds: Set<String>) = operation { repository.addParticipants(id, userIds) }

    fun remove(userId: String, successorId: String? = null) = operation { repository.removeParticipant(id, userId, successorId) }

    /** Sair é remover a própria conta; o gestor com outros no grupo indica quem assume. */
    fun leave(successorId: String? = null) {
        val self = me ?: return
        operation(leaving = true) { repository.removeParticipant(id, self, successorId) }
    }

    fun transfer(successorId: String) = operation { repository.transferManager(id, successorId) }

    fun clearFailure() = update { it.copy(failure = null, rejected = 0) }

    private fun operation(leaving: Boolean = false, call: suspend () -> ApiResult<*>) {
        val current = (mutable.value as? UiState.Content)?.data ?: return
        if (current.busy) return
        update { it.copy(busy = true, failure = null, rejected = 0) }
        viewModelScope.launch {
            val result = call()
            update { it.copy(busy = false, failure = result as? ApiResult.Failure, rejected = (result as? ApiResult.Success)?.data as? Int ?: 0, left = leaving && result is ApiResult.Success) }
            if (result is ApiResult.Success && !leaving) load()
        }
    }

    private fun update(change: (GroupDetailsUi) -> GroupDetailsUi) {
        val state = mutable.value as? UiState.Content ?: return
        mutable.value = state.copy(data = change(state.data))
    }

    private fun details(conversation: Conversation): GroupDetailsUi {
        val names = conversation.participantNames.mapKeys { it.key.lowercase() }
        val members = conversation.participants.map { GroupMember(it, names[it], it == conversation.managerId, it == me) }
            .sortedWith(compareByDescending<GroupMember> { it.manager }.thenByDescending { it.self }.thenBy { it.name.orEmpty().lowercase() })
        return GroupDetailsUi(conversation, members, canManage = me != null && conversation.managerId == me)
    }
}
