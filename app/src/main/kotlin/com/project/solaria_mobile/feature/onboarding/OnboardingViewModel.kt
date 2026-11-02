package com.project.solaria_mobile.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.discovery.JourneyDraft
import com.project.solaria_mobile.feature.discovery.domain.WorkJourneyRepository
import com.project.solaria_mobile.feature.discovery.draftOf
import com.project.solaria_mobile.feature.discovery.pendingDays
import com.project.solaria_mobile.feature.discovery.journeyOf
import com.project.solaria_mobile.feature.discovery.unspecifiedWeekdays
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfile
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfileRepository
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class OnboardingUiState(
    val loading: Boolean = true,
    val loadFailure: ErrorKind? = null,
    val profile: ProfessionalProfile = ProfessionalProfile(),
    /** Profissão do cadastro, só para mostrar: ela vem do registro profissional e não muda por aqui. */
    val occupation: String = "",
    /** Só o autônomo define jornada. */
    val independent: Boolean = false,
    val journey: JourneyDraft = emptyMap(),
    val saving: Boolean = false,
    val failure: ErrorKind? = null,
) {
    /** Pronto para concluir: tudo o que o servidor exige e, para o autônomo, uma jornada válida. */
    val canComplete: Boolean get() = profile.gaps().isEmpty() && !profile.bioTooLong && (!independent || journey.values.any { it != null }) && pendingDays(journey).isEmpty()
}

sealed interface OnboardingEffect {
    /** Perfil concluído. */
    data object Completed : OnboardingEffect
}

/**
 * Complete seu perfil: carrega o rascunho do servidor, guarda cada passo e conclui quando há o necessário. O onboarding
 * é obrigatório, então não há como adiar: o rascunho é guardado a cada passo e ao fechar o aplicativo, mesmo sem
 * endereço; só concluir exige o endereço completo e uma especialidade.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profiles: ProfessionalProfileRepository,
    private val people: ProfileRepository,
    private val journeys: WorkJourneyRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutable

    private val effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    /** Salvamentos acontecem um de cada vez, na ordem, para o último estado sempre prevalecer. */
    private val persistence = Mutex()
    private var technicianId: String? = null

    init { load() }

    fun load() {
        mutable.update { it.copy(loading = true, loadFailure = null) }
        viewModelScope.launch {
            val own = when (val result = people.ownProfile()) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> return@launch fail(result.kind)
            }
            technicianId = own.technicianId ?: return@launch fail(ErrorKind.NOT_FOUND)
            val profile = when (val result = profiles.load()) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> return@launch fail(result.kind)
            }
            val journey = if (own.independent) {
                val saved = (journeys.load(technicianId!!) as? ApiResult.Success)?.data?.takeIf { it.days.isNotEmpty() }
                saved?.let(::draftOf) ?: unspecifiedWeekdays()
            } else emptyMap()
            mutable.update {
                it.copy(loading = false, profile = profile, occupation = own.occupation, independent = own.independent, journey = journey)
            }
        }
    }

    private fun fail(kind: ErrorKind) = mutable.update { it.copy(loading = false, loadFailure = kind) }

    fun update(transform: (ProfessionalProfile) -> ProfessionalProfile) =
        mutable.update { it.copy(profile = transform(it.profile), failure = null) }

    fun updateJourney(journey: JourneyDraft) = mutable.update { it.copy(journey = journey, failure = null) }

    /** Guarda o rascunho sem esperar: o fluxo segue enquanto o salvamento termina em segundo plano. */
    fun saveDraft() {
        viewModelScope.launch {
            persistence.withLock {
                // Depois de concluído não há mais rascunho: salvar ao sair da tela desfaria a conclusão.
                if (!mutable.value.profile.completed) persist()
            }
        }
    }

    fun complete() {
        viewModelScope.launch {
            persistence.withLock {
                if (!persist()) return@withLock
                mutable.update { it.copy(saving = true) }
                when (val result = profiles.complete()) {
                    is ApiResult.Success -> {
                        mutable.update { it.copy(saving = false, profile = result.data) }
                        effects.send(OnboardingEffect.Completed)
                    }
                    is ApiResult.Failure -> mutable.update { it.copy(saving = false, failure = result.kind) }
                }
            }
        }
    }

    /** Envia o rascunho (e a jornada do autônomo) e devolve se deu certo. Chamado sempre dentro do [persistence]. */
    private suspend fun persist(): Boolean {
        val current = mutable.value
        mutable.update { it.copy(saving = true, failure = null) }
        val saved = profiles.save(current.profile)
        if (saved is ApiResult.Failure) {
            mutable.update { it.copy(saving = false, failure = saved.kind) }
            return false
        }
        if (current.independent && pendingDays(current.journey).isEmpty()) {
            val result = journeys.save(technicianId ?: return false, journeyOf(current.journey))
            if (result is ApiResult.Failure) {
                mutable.update { it.copy(saving = false, failure = result.kind) }
                return false
            }
        }
        mutable.update { it.copy(saving = false, profile = it.profile.copy(progress = (saved as ApiResult.Success).data.progress)) }
        return true
    }
}
