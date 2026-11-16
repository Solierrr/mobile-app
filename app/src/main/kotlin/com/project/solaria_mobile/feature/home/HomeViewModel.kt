package com.project.solaria_mobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.home.domain.RecentCompany
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
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
    val recentCompanies: List<RecentCompany> = emptyList(),
    val recentProjects: List<ProjectSummary> = emptyList(),
    val callsQuery: String = "",
    val recentCalls: List<ServiceCall> = emptyList(),
    val profile: UserProfile? = null,
    val callsFailure: ErrorKind? = null,
)

private const val RECENT_LIMIT = 4
private const val RECENT_PROJECT_LIMIT = 3
private const val RECENT_COMPANY_LIMIT = 8

private data class HomeContent(
    val projects: List<ProjectSummary>,
    val calls: List<ServiceCall>,
    val profile: UserProfile?,
    val callsFailure: ErrorKind?,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
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
                    recentCompanies = recentCompaniesOf(loaded.data.projects),
                    recentProjects = loaded.data.projects.sortedByDescending { it.createdAtMillis }.take(RECENT_PROJECT_LIMIT),
                    callsQuery = query,
                    profile = loaded.data.profile,
                    callsFailure = loaded.data.callsFailure,
                    recentCalls = loaded.data.calls.take(RECENT_LIMIT).filter {
                        it.title.contains(query, ignoreCase = true) || it.detail.contains(query, ignoreCase = true)
                    },
                ), loaded.refreshing, loaded.syncedAt, loaded.cached, loaded.refreshError,
            )
            UiState.Loading -> UiState.Loading
            UiState.Empty -> UiState.Empty
            is UiState.Error -> loaded
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { load() }

    private var loadJob: kotlinx.coroutines.Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val previous = content.value as? UiState.Content
            content.value = previous?.copy(refreshing = true) ?: UiState.Loading
            val callsResult = communicationRepository.calls()
            val calls = (callsResult as? ApiResult.Success)?.data.orEmpty()
            combine(projectRepository.observeProjects(), profileRepository.observeOwnProfile()) { projects, profile ->
                val result = projects.result
                when (result) {
                    is ApiResult.Success -> UiState.Content(
                        HomeContent(result.data, calls, (profile.result as? ApiResult.Success)?.data, (callsResult as? ApiResult.Failure)?.kind),
                        refreshing = projects.refreshing || profile.refreshing,
                        syncedAt = projects.syncedAt,
                        cached = projects.cached,
                        refreshError = projects.refreshError ?: profile.refreshError,
                    )
                    is ApiResult.Failure -> UiState.Error(result.kind)
                }
            }.collect { content.value = it }
        }
    }

    fun onCallsQueryChange(value: String) { callsQuery.value = value }
}

internal fun recentCompaniesOf(projects: List<ProjectSummary>): List<RecentCompany> =
    projects
        .filter { it.companyId != null && it.companyName.isNotBlank() }
        .sortedByDescending { it.createdAtMillis }
        .distinctBy { it.companyId }
        .take(RECENT_COMPANY_LIMIT)
        .map { RecentCompany(id = it.companyId.orEmpty(), name = it.companyName, logoUrl = it.companyLogoUrl) }
