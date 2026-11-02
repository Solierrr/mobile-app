package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.communication.domain.ServiceDecision
import com.project.solaria_mobile.feature.communication.domain.ServiceCallRepository
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

data class ServiceActionState(val busy: Boolean = false, val error: ErrorKind? = null, @androidx.annotation.StringRes val message: Int? = null)

@HiltViewModel
class CallDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ServiceCallRepository,
) : ViewModel() {
    private val callId = savedStateHandle.routeId()
    private val mutableState = MutableStateFlow<UiState<ServiceCall>>(UiState.Loading)
    val uiState: StateFlow<UiState<ServiceCall>> = mutableState
    private val mutableAction = MutableStateFlow(ServiceActionState())
    val action: StateFlow<ServiceActionState> = mutableAction
    private var job: kotlinx.coroutines.Job? = null

    init { load() }

    fun clearMessage(@androidx.annotation.StringRes expected: Int) {
        if (mutableAction.value.message == expected) mutableAction.value = mutableAction.value.copy(message = null)
    }

    fun load() {
        if (mutableAction.value.busy) return
        job?.cancel()
        job = viewModelScope.launch { mutableState.collectRead(repository.observeCall(callId)) }
    }

    fun change(decision: ServiceDecision, scheduledAt: OffsetDateTime? = null) {
        val loaded = mutableState.value as? UiState.Content ?: return
        if (mutableAction.value.busy || loaded.refreshing) return
        val current = loaded.data
        if (!current.canManage || current.managedByAgenda) {
            mutableAction.value = ServiceActionState(error = ErrorKind.FORBIDDEN)
            return
        }
        val valid = when (decision) {
            ServiceDecision.ACCEPT -> current.status == "OPEN" && current.hasExecutors
            ServiceDecision.COMPLETE -> current.status == "IN_PROGRESS"
            ServiceDecision.CANCEL, ServiceDecision.SCHEDULE -> current.status in setOf("OPEN", "IN_PROGRESS")
        }
        if (!valid || (decision == ServiceDecision.SCHEDULE && (scheduledAt == null || !scheduledAt.isAfter(OffsetDateTime.now())))) {
            mutableAction.value = ServiceActionState(error = ErrorKind.VALIDATION)
            return
        }
        job?.cancel()
        mutableAction.value = ServiceActionState(busy = true)
        viewModelScope.launch {
            when (val result = repository.change(callId, decision, scheduledAt)) {
                is ApiResult.Success -> {
                    mutableState.value = UiState.Content(result.data)
                    mutableAction.value = ServiceActionState(message = when (decision) {
                        ServiceDecision.ACCEPT -> R.string.cd_servico_aceito
                        ServiceDecision.COMPLETE -> R.string.cd_servico_concluido
                        ServiceDecision.CANCEL -> R.string.cd_servico_cancelado
                        ServiceDecision.SCHEDULE -> R.string.cd_agendamento_atualizado
                    })
                    load()
                }
                is ApiResult.Failure -> {
                    mutableAction.value = ServiceActionState(error = result.kind)
                    if (result.kind in setOf(ErrorKind.FORBIDDEN, ErrorKind.UNAUTHORIZED, ErrorKind.NOT_FOUND)) mutableState.value = UiState.Error(result.kind)
                    else load()
                }
            }
        }
    }
}
