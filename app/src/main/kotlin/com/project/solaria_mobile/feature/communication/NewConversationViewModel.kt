package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.profile.domain.Connection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface NewConversation {
    data object Idle : NewConversation
    data class Working(val name: String) : NewConversation
    data class Failed(val kind: ErrorKind) : NewConversation
    data class Ready(val conversationId: String) : NewConversation
}

/** Abre a conversa direta com uma conexão; o messenger reaproveita a que já existir com ela. */
@HiltViewModel
class NewConversationViewModel @Inject constructor(private val repository: CommunicationRepository) : ViewModel() {
    private val mutable = MutableStateFlow<NewConversation>(NewConversation.Idle)
    val state: StateFlow<NewConversation> = mutable

    fun start(connection: Connection) {
        if (mutable.value is NewConversation.Working) return
        val authId = connection.authId
        if (authId.isNullOrBlank()) { mutable.value = NewConversation.Failed(ErrorKind.NOT_FOUND); return }
        mutable.value = NewConversation.Working(connection.name)
        viewModelScope.launch {
            mutable.value = when (val result = repository.startDirectConversation(authId)) {
                is ApiResult.Success -> NewConversation.Ready(result.data)
                is ApiResult.Failure -> NewConversation.Failed(result.kind)
            }
        }
    }

    /** Depois de abrir a conversa, a escolha volta ao início, para um retorno à tela não reabrir a mesma conversa. */
    fun consumed() {
        mutable.value = NewConversation.Idle
    }
}
