package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface NewGroup {
    data object Idle : NewGroup
    data object Working : NewGroup
    data class Failed(val failure: ApiResult.Failure) : NewGroup
    /** [rejected] convidados não foram admitidos pelo servidor. */
    data class Created(val conversationId: String, val rejected: Int) : NewGroup
}

/** Cria um grupo avulso com as conexões escolhidas; quem cria vira o gestor. */
@HiltViewModel
class NewGroupViewModel @Inject constructor(private val repository: CommunicationRepository) : ViewModel() {
    private val mutable = MutableStateFlow<NewGroup>(NewGroup.Idle)
    val state: StateFlow<NewGroup> = mutable

    fun create(title: String, participants: Set<String>) {
        if (mutable.value is NewGroup.Working) return
        val name = title.trim()
        if (name.isEmpty() || name.length > MAX_GROUP_TITLE || participants.isEmpty()) {
            mutable.value = NewGroup.Failed(ApiResult.Failure(ErrorKind.VALIDATION))
            return
        }
        mutable.value = NewGroup.Working
        viewModelScope.launch {
            mutable.value = when (val result = repository.createGroup(name, participants)) {
                is ApiResult.Success -> NewGroup.Created(result.data.conversationId, result.data.rejectedParticipants)
                is ApiResult.Failure -> NewGroup.Failed(result)
            }
        }
    }

    /** Depois de abrir o grupo criado, volta ao início para um retorno à tela não reabri-lo. */
    fun consumed() {
        mutable.value = NewGroup.Idle
    }
}
