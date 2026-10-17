package com.project.solaria_mobile.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProjectListViewModel @Inject constructor(private val repository: ProjectRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<List<ProjectSummary>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ProjectSummary>>> = mutableState

    init { load() }

    fun load() {
        viewModelScope.launch { mutableState.load(isEmpty = { it.isEmpty() }) { repository.projects() } }
    }
}
