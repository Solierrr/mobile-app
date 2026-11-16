package com.project.solaria_mobile.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import kotlinx.coroutines.Job
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import com.project.solaria_mobile.feature.project.data.ProjectDisplayPreferences
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.project.domain.ProjectInvitation

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val preferences: ProjectDisplayPreferences? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<List<ProjectSummary>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ProjectSummary>>> = mutableState
    val gridMode = (preferences?.observeGrid() ?: flowOf(false))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val mutableLayoutFailure = MutableStateFlow(false)
    val layoutFailure: StateFlow<Boolean> = mutableLayoutFailure

    fun toggleLayout() {
        viewModelScope.launch {
            mutableLayoutFailure.value = false
            try { preferences?.setGrid(!gridMode.value) } catch (_: java.io.IOException) { mutableLayoutFailure.value = true }
        }
    }

    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { mutableState.collectRead(repository.observeProjects()) }
        loadInvitations()
    }

    /** Convites recebidos e ainda pendentes; sem rede ou sem suporte, a lista fica vazia e não bloqueia os projetos. */
    private val mutableInvitations = MutableStateFlow<List<ProjectInvitation>>(emptyList())
    val invitations: StateFlow<List<ProjectInvitation>> = mutableInvitations
    private val mutableInvitationAction = MutableStateFlow<ProjectAction?>(null)
    val invitationAction: StateFlow<ProjectAction?> = mutableInvitationAction

    init { load() }

    private fun loadInvitations() {
        viewModelScope.launch {
            mutableInvitations.value = (repository.myInvitations() as? ApiResult.Success)?.data.orEmpty()
        }
    }

    /** Aceitar ou recusar; ao aceitar, a lista de projetos é recarregada para mostrar o novo projeto. */
    fun respond(id: String, accept: Boolean) {
        if (mutableInvitationAction.value == ProjectAction.Working) return
        mutableInvitationAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutableInvitationAction.value = when (val result = repository.respondInvitation(id, accept)) {
                is ApiResult.Success -> {
                    mutableInvitations.value = mutableInvitations.value.filterNot { it.id == id }
                    if (accept) load()
                    null
                }
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, ProjectOperation.RESPOND_INVITE)
            }
        }
    }
}
