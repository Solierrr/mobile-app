package com.project.solaria_mobile.feature.home

import com.project.solaria_mobile.feature.home.data.CompanyLogo
import com.project.solaria_mobile.feature.home.data.HomeRepository
import com.project.solaria_mobile.feature.communication.data.CommunicationRepository
import com.project.solaria_mobile.feature.communication.data.ServiceCall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.feature.project.data.ProjectRepository
import com.project.solaria_mobile.feature.project.data.ProjectSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val recentCompanies: List<CompanyLogo> = emptyList(),
    val recentProjects: List<ProjectSummary> = emptyList(),
    val callsQuery: String = "",
    val recentCalls: List<ServiceCall> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    homeRepository: HomeRepository,
    projectRepository: ProjectRepository,
    communicationRepository: CommunicationRepository,
) : ViewModel() {
    private val callsQuery = MutableStateFlow("")
    val uiState: StateFlow<HomeUiState> = combine(
        homeRepository.recentCompanies, projectRepository.projects, communicationRepository.calls, callsQuery,
    ) { companies, projects, calls, query ->
        HomeUiState(companies, projects.take(4), query, calls.filter {
            it.title.contains(query, ignoreCase = true) || it.detail.contains(query, ignoreCase = true)
        })
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(homeRepository.recentCompanies.value, projectRepository.projects.value.take(4), recentCalls = communicationRepository.calls.value),
    )

    fun onCallsQueryChange(value: String) { callsQuery.value = value }
}
