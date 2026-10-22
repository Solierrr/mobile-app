package com.project.solaria_mobile.feature.discovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.collectRead
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import com.project.solaria_mobile.feature.project.domain.hasCoordinates
import com.project.solaria_mobile.presentation.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class MapUiState(val projects: List<ProjectSummary> = emptyList(), val selectedId: String? = null,
    val resolving: Boolean = false, val locationError: ErrorKind? = null)

@HiltViewModel
class MapViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repository: ProjectRepository) : ViewModel() {
    private val initialId = runCatching { savedStateHandle.toRoute<AppDestination.Map>().projectId }.getOrNull()
    private val mutableState = MutableStateFlow<UiState<MapUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<MapUiState>> = mutableState
    private var initialSelectionPending = true
    private var job: Job? = null
    private var resolving: Job? = null
    init { load() }

    fun load() {
        job?.cancel()
        resolving?.cancel()
        job = viewModelScope.launch {
            mutableState.collectRead(repository.observeProjects().let { source ->
                source.map { read -> read.map { projects ->
                    val selected = if (initialSelectionPending) initialId else (mutableState.value as? UiState.Content)?.data?.selectedId
                    initialSelectionPending = false
                    MapUiState(projects, selected)
                } }
            })
        }
    }
    fun select(id: String?) {
        val current = mutableState.value as? UiState.Content ?: return
        mutableState.value = current.copy(data = current.data.copy(selectedId = id, locationError = null))
    }
    fun resolveLocations() {
        if (resolving?.isActive == true) return
        val current = mutableState.value as? UiState.Content ?: return
        val missing = current.data.projects.filter { !it.hasCoordinates() && it.address.isNotBlank() }
        if (missing.isEmpty()) return
        resolving = viewModelScope.launch {
            mutableState.value = current.copy(data = current.data.copy(resolving = true, locationError = null))
            for (project in missing) {
                val result = repository.resolveLocation(project.id)
                val latest = mutableState.value as? UiState.Content ?: break
                mutableState.value = when (result) {
                    is ApiResult.Success -> latest.copy(data = latest.data.copy(projects = latest.data.projects.map {
                        if (it.id == project.id) result.data else it
                    }))
                    is ApiResult.Failure -> latest.copy(data = latest.data.copy(locationError = result.kind))
                }
                if (result is ApiResult.Failure && result.kind in setOf(ErrorKind.UNAUTHORIZED, ErrorKind.CONNECTIVITY, ErrorKind.SERVER, ErrorKind.TIMEOUT)) break
            }
            val latest = mutableState.value as? UiState.Content
            if (latest != null) mutableState.value = latest.copy(data = latest.data.copy(resolving = false))
        }
    }
}
