package com.project.solaria_mobile.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import com.project.solaria_mobile.feature.profile.domain.CompanyProfile
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.companyFromProjects
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.presentation.navigation.routeIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class CompanyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projects: ProjectRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val companyId = savedStateHandle.routeIdOrNull()
    private val state = MutableStateFlow<UiState<CompanyProfile>>(UiState.Loading)
    val uiState: StateFlow<UiState<CompanyProfile>> = state
    private var job: Job? = null

    init { load() }

    fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            val id = companyId ?: when (val profile = profiles.observeOwnProfile().first {
                !it.refreshing || (it.result as? ApiResult.Success)?.data?.companyId != null
            }.result) {
                is ApiResult.Failure -> {
                    state.collectRead(flowOf(ReadResult(profile)))
                    return@launch
                }
                is ApiResult.Success -> profile.data.companyId
            }
            if (id == null) {
                state.value = UiState.Empty
                return@launch
            }
            state.collectRead(projects.observeProjects().map { read ->
                val result = when (val source = read.result) {
                    is ApiResult.Failure -> source
                    is ApiResult.Success -> companyFromProjects(id, source.data)?.let { ApiResult.Success(it) }
                        ?: ApiResult.Success(CompanyProfile(id, "Empresa vinculada", null, emptyList(), detailsAvailable = false))
                }
                ReadResult(result, read.syncedAt, read.cached, read.refreshing, read.refreshError)
            })
        }
    }
}
