package com.project.solaria_mobile.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Empresa da própria conta, para a lista de projetos oferecer o atalho só a quem tem uma. */
@HiltViewModel
class OwnCompanyViewModel @Inject constructor(private val profiles: ProfileRepository) : ViewModel() {
    private val mutable = MutableStateFlow<String?>(null)
    val companyId: StateFlow<String?> = mutable

    init {
        viewModelScope.launch { mutable.value = (profiles.ownProfile() as? ApiResult.Success)?.data?.companyId }
    }
}
