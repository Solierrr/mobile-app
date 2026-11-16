package com.project.solaria_mobile.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import com.project.solaria_mobile.core.common.map
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import com.project.solaria_mobile.presentation.navigation.routeIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(val self: Boolean = true, val profile: UserProfile, val connected: Boolean = false, val actionError: ErrorKind? = null)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
) : ViewModel() {
    private val profileId: String? = savedStateHandle.routeIdOrNull()
    private val mutableState = MutableStateFlow<UiState<ProfileUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProfileUiState>> = mutableState

    init { load() }

    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (profileId == null) {
                mutableState.collectRead(repository.observeOwnProfile().map { read -> read.map { ProfileUiState(true, it) } })
                return@launch
            }
            mutableState.value = UiState.Loading
            val self = false
            val profile = repository.profile(profileId)
            if (profile is ApiResult.Failure) {
                mutableState.value = UiState.Error(profile.kind)
                return@launch
            }
            val data = (profile as? ApiResult.Success)?.data
            if (data == null) {
                mutableState.value = UiState.Error(ErrorKind.UNKNOWN)
                return@launch
            }
            val connected = repository.isConnected(data.id)
            mutableState.value = when (connected) {
                is ApiResult.Failure -> UiState.Error(connected.kind)
                is ApiResult.Success -> UiState.Content(ProfileUiState(self, data, connected.data))
            }
        }
    }

    fun toggleConnection() {
        val current = mutableState.value as? UiState.Content ?: return
        viewModelScope.launch {
            val result = repository.toggleConnection(current.data.profile.id)
            mutableState.value = when (result) {
                is ApiResult.Success -> UiState.Content(current.data.copy(connected = result.data, actionError = null))
                is ApiResult.Failure -> UiState.Content(current.data.copy(actionError = result.kind))
            }
        }
    }
}
