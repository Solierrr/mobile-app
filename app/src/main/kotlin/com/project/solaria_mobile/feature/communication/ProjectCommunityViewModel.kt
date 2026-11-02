package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.canRetainContent
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunity
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunityRepository
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Aviso de grupo criado; [rejected] convites não foram admitidos pelo servidor. */
data class GroupNotice(val rejected: Int)

data class CommunityContent(val community: ProjectCommunity, val busy: Boolean = false, val error: ErrorKind? = null, val notice: GroupNotice? = null)

@HiltViewModel
class ProjectCommunityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProjectCommunityRepository,
) : ViewModel() {
    private val projectId = savedStateHandle.routeId()
    private val mutable = MutableStateFlow<UiState<CommunityContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<CommunityContent>> = mutable
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        val previous = mutable.value as? UiState.Content
        if (previous?.data?.busy == true) return
        loadJob?.cancel()
        mutable.value = previous?.copy(refreshing = true) ?: UiState.Loading
        loadJob = viewModelScope.launch {
            val read = repository.read(projectId)
            mutable.value = when (val result = read.result) {
                is ApiResult.Success -> UiState.Content(CommunityContent(result.data, notice = previous?.data?.notice),
                    syncedAt = read.syncedAt, cached = read.cached, refreshError = read.refreshError)
                is ApiResult.Failure -> if (previous != null && result.kind.canRetainContent())
                    previous.copy(refreshing = false, refreshError = result.kind) else UiState.Error(result.kind)
            }
        }
    }

    fun create() = operation { community ->
        if (community.id != null || !community.canCreate) return@operation ApiResult.Failure(ErrorKind.FORBIDDEN)
        repository.create(projectId)
    }

    fun clearNotice() {
        val current = mutable.value as? UiState.Content ?: return
        if (!current.data.busy) mutable.value = current.copy(data = current.data.copy(notice = null))
    }

    fun addMember(authId: String) = operation { community ->
        val id = community.id ?: return@operation ApiResult.Failure(ErrorKind.NOT_FOUND)
        if (!community.active || !community.canManage || community.people.none { it.authId == authId && !it.joined })
            return@operation ApiResult.Failure(ErrorKind.FORBIDDEN)
        repository.addMember(id, authId)
    }

    fun createGroup(title: String, participants: Set<String>? = null) = operation { community ->
        val id = community.id ?: return@operation ApiResult.Failure(ErrorKind.NOT_FOUND)
        if (!community.active || community.members < 2) return@operation ApiResult.Failure(ErrorKind.FORBIDDEN)
        if (title.trim().isEmpty() || title.trim().length > 120) return@operation ApiResult.Failure(ErrorKind.VALIDATION)
        if (participants != null && (participants.isEmpty() || participants.any { candidate -> community.people.none { it.authId == candidate && it.joined && !it.self } }))
            return@operation ApiResult.Failure(ErrorKind.VALIDATION)
        when (val result = repository.createGroup(id, title.trim(), participants.orEmpty())) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(GroupNotice(result.data.rejectedParticipants))
        }
    }

    private fun operation(call: suspend (ProjectCommunity) -> ApiResult<*>) {
        val current = mutable.value as? UiState.Content ?: return
        if (current.refreshing || current.data.busy) return
        mutable.value = current.copy(data = current.data.copy(busy = true, error = null, notice = null))
        viewModelScope.launch {
            val result = call(current.data.community)
            val latest = mutable.value as? UiState.Content ?: return@launch
            mutable.value = latest.copy(data = latest.data.copy(busy = false,
                error = (result as? ApiResult.Failure)?.kind,
                notice = (result as? ApiResult.Success)?.data as? GroupNotice))
            if (result is ApiResult.Success || (result is ApiResult.Failure && result.kind == ErrorKind.CONFLICT)) load()
        }
    }
}
