package com.project.solaria_mobile.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfileRepository
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Decide se o profissional precisa completar o perfil antes de usar o aplicativo. Só é `true` quando a conta tem perfil
 * profissional e ele ainda não foi concluído; conta sem perfil, falha ao consultar ou perfil concluído liberam o acesso,
 * para um problema de rede nunca prender ninguém no onboarding.
 */
@HiltViewModel
class OnboardingGateViewModel @Inject constructor(
    private val profiles: ProfessionalProfileRepository,
    private val people: ProfileRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow<Boolean?>(null)
    val needsOnboarding: StateFlow<Boolean?> = mutable

    fun refresh() {
        viewModelScope.launch {
            val own = (people.ownProfile() as? ApiResult.Success)?.data
            mutable.value = when {
                own?.technicianId == null -> false
                else -> (profiles.load() as? ApiResult.Success)?.data?.let { !it.completed } ?: false
            }
        }
    }

    /** Chamado quando o perfil acaba de ser concluído, sem esperar nova consulta. */
    fun completed() {
        mutable.value = false
    }
}
