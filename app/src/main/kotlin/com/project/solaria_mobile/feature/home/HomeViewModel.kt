package com.project.solaria_mobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.toUiState
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.home.domain.CompanyLogo
import com.project.solaria_mobile.feature.home.domain.HomeRepository
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentCompanies: List<CompanyLogo> = emptyList(),
    val recentProjects: List<ProjectSummary> = emptyList(),
    val callsQuery: String = "",
    val recentCalls: List<ServiceCall> = emptyList(),
    val profile: UserProfile? = null,
)

private const val RECENT_LIMIT = 4

private data class HomeContent(
    val companies: List<CompanyLogo>,
    val projects: List<ProjectSummary>,
    val calls: List<ServiceCall>,
    val profile: UserProfile?,
)

private data class HomeLoad(
    val companies: ApiResult<List<CompanyLogo>>,
    val projects: ApiResult<List<ProjectSummary>>,
    val calls: ApiResult<List<ServiceCall>>,
    val profile: ApiResult<UserProfile>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val projectRepository: ProjectRepository,
    private val communicationRepository: CommunicationRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val callsQuery = MutableStateFlow("")
    private val content = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)

    val uiState: StateFlow<UiState<HomeUiState>> = combine(content, callsQuery) { loaded, query ->
        when (loaded) {
            is UiState.Content -> UiState.Content(
                HomeUiState(
                    recentCompanies = loaded.data.companies,
                    recentProjects = loaded.data.projects.take(RECENT_LIMIT),
                    callsQuery = query,
                    profile = loaded.data.profile,
                    recentCalls = loaded.data.calls.take(RECENT_LIMIT).filter {
                        it.title.contains(query, ignoreCase = true) || it.detail.contains(query, ignoreCase = true)
                    },
                )
            )
            UiState.Loading -> UiState.Loading
            UiState.Empty -> UiState.Empty
            is UiState.Error -> loaded
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch {
            content.value = UiState.Loading
            val (companies, projects, calls, profile) = coroutineScope {
                val companiesCall = async { homeRepository.recentCompanies() }
                val projectsCall = async { projectRepository.projects() }
                val callsCall = async { communicationRepository.calls() }
                val profileCall = async { profileRepository.ownProfile() }
                HomeLoad(companiesCall.await(), projectsCall.await(), callsCall.await(), profileCall.await())
            }
            content.value = when {
                companies is ApiResult.Failure -> UiState.Error(companies.kind)
                projects is ApiResult.Failure -> UiState.Error(projects.kind)
                calls is ApiResult.Failure -> UiState.Error(calls.kind)
                companies is ApiResult.Success && projects is ApiResult.Success && calls is ApiResult.Success ->
                    ApiResult.Success(HomeContent(companies.data, projects.data, calls.data, (profile as? ApiResult.Success)?.data)).toUiState()
                else -> UiState.Error(ErrorKind.UNKNOWN)
            }
        }
    }

    fun onCallsQueryChange(value: String) { callsQuery.value = value }
}
