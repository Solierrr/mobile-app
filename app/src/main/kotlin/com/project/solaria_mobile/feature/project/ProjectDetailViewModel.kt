package com.project.solaria_mobile.feature.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import kotlinx.coroutines.Job
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.project.domain.ProjectStatusOption
import com.project.solaria_mobile.feature.project.data.ProjectAccessLog

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val accessLog: ProjectAccessLog? = null,
) : ViewModel() {
    private val projectId = savedStateHandle.routeId()
    private val mutableState = MutableStateFlow<UiState<ProjectSummary>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProjectSummary>> = mutableState

    init {
        load()
        viewModelScope.launch { accessLog?.record(projectId) }
    }

    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { mutableState.collectRead(repository.observeProject(projectId)) }
    }

    private val mutableAction = MutableStateFlow<ProjectAction?>(null)
    val action: StateFlow<ProjectAction?> = mutableAction

    /** Muda a situação no servidor; a tela só mostra a nova situação depois da confirmação. */
    fun changeStatus(status: ProjectStatusOption) {
        if (mutableAction.value == ProjectAction.Working) return
        mutableAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutableAction.value = when (val result = repository.changeStatus(projectId, status)) {
                is ApiResult.Success -> {
                    mutableState.value = UiState.Content(result.data)
                    null
                }
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, ProjectOperation.STATUS, status)
            }
        }
    }

    fun archive() {
        if (mutableAction.value == ProjectAction.Working) return
        mutableAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutableAction.value = when (val result = repository.archive(projectId)) {
                is ApiResult.Success -> ProjectAction.Archived
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, ProjectOperation.ARCHIVE)
            }
        }
    }

    fun clearAction() {
        if (mutableAction.value != ProjectAction.Working) mutableAction.value = null
    }
}

enum class ProjectOperation { STATUS, ARCHIVE, UPLOAD_PHOTO, REMOVE_PHOTO, SET_COVER, ACTIVITY_STATUS, INVITE, REVOKE_INVITE, RESEND_INVITE, REMOVE_MEMBER, RESPOND_INVITE, SAVE_ACTIVITY, CREATE_LINK, REVOKE_LINK, CREATE_REPORT }

sealed interface ProjectAction {
    data object Working : ProjectAction
    data object Archived : ProjectAction
    /** Operação confirmada pelo servidor, para a tela fechar o formulário ou o segundo nível que a originou. */
    data class Done(val operation: ProjectOperation) : ProjectAction
    data class Failed(val kind: ErrorKind, val operation: ProjectOperation, val status: ProjectStatusOption? = null) : ProjectAction
}
