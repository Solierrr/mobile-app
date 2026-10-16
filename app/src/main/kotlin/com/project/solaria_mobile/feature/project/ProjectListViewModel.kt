package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import com.project.solaria_mobile.feature.project.domain.ProjectRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProjectListViewModel @Inject constructor(repository: ProjectRepository) : ViewModel() {
    val projects: StateFlow<List<ProjectSummary>> = repository.projects.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.projects.value
    )
}
