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

data class ProfileUiState(val self: Boolean = true, val profile: UserProfile, val connected: Boolean = false, val actionError: ErrorKind? = null, val connecting: Boolean = false)

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
            mutableState.value = UiState.Content(ProfileUiState(self, data, data.connected))
        }
    }

    fun toggleConnection() {
        val current = mutableState.value as? UiState.Content ?: return
        if (current.data.connecting) return
        mutableState.value = current.copy(data = current.data.copy(connecting = true, actionError = null))
        viewModelScope.launch {
            val result = repository.setConnection(current.data.profile.id, !current.data.connected)
            mutableState.value = when (result) {
                is ApiResult.Success -> UiState.Content(current.data.copy(connected = result.data, actionError = null, connecting = false))
                is ApiResult.Failure -> UiState.Content(current.data.copy(actionError = result.kind, connecting = false))
            }
        }
    }
}
