package com.project.solaria_mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.discovery.data.GoogleCalendarRepository
import com.project.solaria_mobile.feature.discovery.data.GoogleConnection
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class GoogleCalendarState(val connection: GoogleConnection? = null, val events: List<ScheduleEvent> = emptyList(), val busy: Boolean = false, val message: String? = null)

@HiltViewModel
class GoogleCalendarViewModel @Inject constructor(private val repository: GoogleCalendarRepository) : ViewModel() {
    private val mutable = MutableStateFlow(GoogleCalendarState())
    val state = mutable.asStateFlow()
    private val authorizations = Channel<String>(Channel.BUFFERED)
    val authorization = authorizations.receiveAsFlow()
    private var job: Job? = null
    private var month = LocalDate.now()
    private var actionJob: Job? = null

    init {
        viewModelScope.launch {
            repository.sessions.collect {
                job?.cancel()
                actionJob?.cancel()
                mutable.value = GoogleCalendarState()
                load(month)
            }
        }
    }
    fun load(date: LocalDate) {
        month = date
        job?.cancel()
        job = viewModelScope.launch { refresh(date) }
    }

    private suspend fun refresh(date: LocalDate) {
            mutable.value = mutable.value.copy(busy = true)
            when (val result = repository.connection()) {
                is ApiResult.Success -> {
                    val connection = result.data
                    mutable.value = mutable.value.copy(connection = connection, events = if (connection.connected) mutable.value.events else emptyList(), message = null)
                    if (connection.connected) {
                        when (val events = repository.events(date)) {
                            is ApiResult.Success -> mutable.value = mutable.value.copy(events = events.data)
                            is ApiResult.Failure -> unavailable(events.kind)

                        }
                    }
                }
                is ApiResult.Failure -> unavailable(result.kind)
            }
            mutable.value = mutable.value.copy(busy = false)
    }
    private fun unavailable(kind: ErrorKind) {
        val lostAccess = kind in setOf(ErrorKind.UNAUTHORIZED, ErrorKind.FORBIDDEN, ErrorKind.NOT_FOUND, ErrorKind.CONFLICT)
        mutable.value = mutable.value.copy(
            events = if (lostAccess) emptyList() else mutable.value.events,
            connection = if (lostAccess) mutable.value.connection?.copy(connected = false, reconnectRequired = true) else mutable.value.connection,
            message = if (lostAccess) "Reconecte sua conta Google para atualizar os eventos." else "Não foi possível atualizar o Google Agenda. Suas atividades do Solaria continuam disponíveis.",
        )
    }
    fun connect() = action {
        when (val result = repository.connect()) {
            is ApiResult.Success -> { if (result.data != null) authorizations.send(result.data); refresh(month) }
            is ApiResult.Failure -> mutable.value = mutable.value.copy(message = "Não foi possível conectar o Google Agenda. Confira sua conexão e tente novamente.")
        }
    }
    fun disconnect() = action {
        when (repository.disconnect()) {
            is ApiResult.Success -> { mutable.value = GoogleCalendarState(message = "Google Agenda desconectado."); refresh(month) }
            is ApiResult.Failure -> mutable.value = mutable.value.copy(message = "Não foi possível desconectar. Tente novamente com internet.")
        }
    }
    fun export(event: ScheduleEvent, end: OffsetDateTime) = action {
        when (repository.export(event, end)) {
            is ApiResult.Success -> { refresh(month); mutable.value = mutable.value.copy(message = "Atividade exportada para o Google Agenda.") }
            is ApiResult.Failure -> mutable.value = mutable.value.copy(message = "Não foi possível exportar. Verifique o horário de término, sua conexão e a conta Google.")
        }
    }
    private fun action(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        actionJob = viewModelScope.launch { mutable.value = mutable.value.copy(busy = true); try { block() } finally { mutable.value = mutable.value.copy(busy = false) } }
    }
}
