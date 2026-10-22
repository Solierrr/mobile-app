package com.project.solaria_mobile.feature.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import kotlinx.coroutines.Job
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProjectSectionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProjectDetailRepository,
) : ViewModel() {
    val projectId = savedStateHandle.routeId()

    private val mutableLocation = MutableStateFlow<UiState<ProjectLocationInfo>>(UiState.Loading)
    private val mutableMembers = MutableStateFlow<UiState<List<ProjectMember>>>(UiState.Loading)
    private val mutableActivities = MutableStateFlow<UiState<List<ProjectActivity>>>(UiState.Loading)
    private val mutableHistory = MutableStateFlow<UiState<List<ProjectHistoryEntry>>>(UiState.Loading)
    private val mutablePhotos = MutableStateFlow<UiState<List<ProjectPhoto>>>(UiState.Loading)

    val location: StateFlow<UiState<ProjectLocationInfo>> = mutableLocation
    val members: StateFlow<UiState<List<ProjectMember>>> = mutableMembers
    val activities: StateFlow<UiState<List<ProjectActivity>>> = mutableActivities
    val history: StateFlow<UiState<List<ProjectHistoryEntry>>> = mutableHistory
    val photos: StateFlow<UiState<List<ProjectPhoto>>> = mutablePhotos

    private val loaded = mutableSetOf<ProjectSheet>()
    private val jobs = mutableMapOf<ProjectSheet, Job>()

    init { loadPhotos() }

    fun open(sheet: ProjectSheet) {
        if (sheet in loaded) return
        load(sheet)
    }

    fun retry(sheet: ProjectSheet) = load(sheet)

    suspend fun photoContent(photoId: String): ByteArray? =
        (repository.photoContent(projectId, photoId) as? com.project.solaria_mobile.core.common.ApiResult.Success)?.data

    private fun loadPhotos() {
        loaded += ProjectSheet.GALLERY
        jobs[ProjectSheet.GALLERY] = viewModelScope.launch { mutablePhotos.collectRead(repository.observePhotos(projectId), isEmpty = { it.isEmpty() }) }
    }

    private fun load(sheet: ProjectSheet) {
        loaded += sheet
        jobs.remove(sheet)?.cancel()
        jobs[sheet] = viewModelScope.launch {
            when (sheet) {
                ProjectSheet.LOCATION -> mutableLocation.collectRead(repository.observeLocation(projectId))
                ProjectSheet.GALLERY -> mutablePhotos.collectRead(repository.observePhotos(projectId), isEmpty = { it.isEmpty() })
                ProjectSheet.AGENDA -> mutableActivities.collectRead(repository.observeActivities(projectId), isEmpty = { it.isEmpty() })
                ProjectSheet.HISTORY -> mutableHistory.collectRead(repository.observeHistory(projectId), isEmpty = { it.isEmpty() })
                ProjectSheet.TEAM -> mutableMembers.collectRead(repository.observeMembers(projectId), isEmpty = { it.isEmpty() })
                ProjectSheet.CONVERSATIONS -> Unit
            }
        }
    }
}
