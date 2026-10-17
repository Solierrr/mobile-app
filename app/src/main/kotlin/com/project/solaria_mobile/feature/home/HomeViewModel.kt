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
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
)

private const val RECENT_LIMIT = 4

private data class HomeContent(
    val companies: List<CompanyLogo>,
    val projects: List<ProjectSummary>,
    val calls: List<ServiceCall>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val projectRepository: ProjectRepository,
    private val communicationRepository: CommunicationRepository,
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
            val companies = homeRepository.recentCompanies()
            val projects = projectRepository.projects()
            val calls = communicationRepository.calls()
            content.value = when {
                companies is ApiResult.Failure -> UiState.Error(companies.kind)
                projects is ApiResult.Failure -> UiState.Error(projects.kind)
                calls is ApiResult.Failure -> UiState.Error(calls.kind)
                companies is ApiResult.Success && projects is ApiResult.Success && calls is ApiResult.Success ->
                    ApiResult.Success(HomeContent(companies.data, projects.data, calls.data)).toUiState()
                else -> UiState.Error(ErrorKind.UNKNOWN)
            }
        }
    }

    fun onCallsQueryChange(value: String) { callsQuery.value = value }
}
