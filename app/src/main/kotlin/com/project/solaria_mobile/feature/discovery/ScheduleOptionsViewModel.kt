package com.project.solaria_mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.discovery.domain.BusySlot
import com.project.solaria_mobile.feature.discovery.domain.CalendarLinkRepository
import com.project.solaria_mobile.feature.discovery.domain.WorkJourney
import com.project.solaria_mobile.feature.discovery.domain.WorkJourneyRepository
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GoogleLink { CHECKING, CONNECTED, DISCONNECTED, UNAVAILABLE }

data class ScheduleOptionsState(
    val technicianId: String? = null,
    /** Só o profissional autônomo define jornada. */
    val independent: Boolean = false,
    /** `null` enquanto carrega ou se o servidor não respondeu; vazia quando ainda não há jornada definida. */
    val journey: WorkJourney? = null,
    val saving: Boolean = false,
    val saveFailed: ErrorKind? = null,
    val google: GoogleLink = GoogleLink.CHECKING,
    val googleBusy: Boolean = false,
    val googleFailed: ErrorKind? = null,
    /** Endereço de autorização que a tela deve abrir uma vez no navegador. */
    val authorizationUrl: String? = null,
    val busy: List<BusySlot> = emptyList(),
) {
    val hasOptions: Boolean get() = technicianId != null
}

@HiltViewModel
class ScheduleOptionsViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val journeys: WorkJourneyRepository,
    private val links: CalendarLinkRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(ScheduleOptionsState())
    val state: StateFlow<ScheduleOptionsState> = mutable.asStateFlow()
    private var month: LocalDate? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            val own = (profiles.ownProfile() as? ApiResult.Success)?.data ?: return@launch
            val technician = own.technicianId ?: return@launch
            mutable.update { it.copy(technicianId = technician, independent = own.independent) }
            if (own.independent) launch { loadJourney(technician) }
            launch { refreshGoogle() }
        }
    }

    private suspend fun loadJourney(technician: String) {
        (journeys.load(technician) as? ApiResult.Success)?.let { loaded -> mutable.update { it.copy(journey = loaded.data) } }
    }

    fun saveJourney(journey: WorkJourney, onSaved: () -> Unit) {
        val technician = mutable.value.technicianId ?: return
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true, saveFailed = null) }
        viewModelScope.launch {
            when (val result = journeys.save(technician, journey)) {
                is ApiResult.Success -> {
                    mutable.update { it.copy(saving = false, journey = result.data) }
                    onSaved()
                }
                is ApiResult.Failure -> mutable.update { it.copy(saving = false, saveFailed = result.kind) }
            }
        }
    }

    fun clearSaveError() = mutable.update { it.copy(saveFailed = null) }

    /** Confere de novo se a autorização foi concluída; chamada ao voltar do navegador. */
    fun refreshGoogle() {
        val technician = mutable.value.technicianId ?: return
        viewModelScope.launch {
            when (val result = links.isConnected(technician)) {
                is ApiResult.Success -> {
                    mutable.update { it.copy(google = if (result.data) GoogleLink.CONNECTED else GoogleLink.DISCONNECTED) }
                    if (result.data) loadBusy()
                }
                is ApiResult.Failure -> mutable.update { it.copy(google = GoogleLink.UNAVAILABLE) }
            }
        }
    }

    fun connectGoogle() {
        val technician = mutable.value.technicianId ?: return
        if (mutable.value.googleBusy) return
        mutable.update { it.copy(googleBusy = true, googleFailed = null) }
        viewModelScope.launch {
            when (val result = links.authorizationUrl(technician)) {
                is ApiResult.Success -> mutable.update { it.copy(googleBusy = false, authorizationUrl = result.data) }
                is ApiResult.Failure -> mutable.update { it.copy(googleBusy = false, googleFailed = result.kind) }
            }
        }
    }

    fun authorizationOpened() = mutable.update { it.copy(authorizationUrl = null) }

    fun disconnectGoogle() {
        val technician = mutable.value.technicianId ?: return
        if (mutable.value.googleBusy) return
        mutable.update { it.copy(googleBusy = true, googleFailed = null) }
        viewModelScope.launch {
            when (val result = links.disconnect(technician)) {
                is ApiResult.Success -> mutable.update { it.copy(googleBusy = false, google = GoogleLink.DISCONNECTED, busy = emptyList()) }
                is ApiResult.Failure -> mutable.update { it.copy(googleBusy = false, googleFailed = result.kind) }
            }
        }
    }

    /** Horários ocupados do mês exibido, vindos do Google; some se a integração não estiver ativa. */
    fun showMonth(first: LocalDate) {
        month = first.withDayOfMonth(1)
        if (mutable.value.google == GoogleLink.CONNECTED) viewModelScope.launch { loadBusy() }
    }

    private suspend fun loadBusy() {
        val technician = mutable.value.technicianId ?: return
        val start = (month ?: LocalDate.now().withDayOfMonth(1))
        val zone = ZoneId.systemDefault()
        val result = links.busy(technician, start.atStartOfDay(zone).toOffsetDateTime(), start.plusMonths(1).atStartOfDay(zone).toOffsetDateTime())
        (result as? ApiResult.Success)?.let { loaded -> mutable.update { it.copy(busy = loaded.data) } }
    }
}
