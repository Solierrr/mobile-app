package com.project.solaria_mobile.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import com.project.solaria_mobile.presentation.navigation.routeIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(val self: Boolean = true, val profile: UserProfile, val connected: Boolean = false)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
) : ViewModel() {
    private val profileId: String? = savedStateHandle.routeIdOrNull()
    private val mutableState = MutableStateFlow<UiState<ProfileUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProfileUiState>> = mutableState

    init { load() }

    fun load() {
        viewModelScope.launch {
            mutableState.value = UiState.Loading
            val self = profileId == null
            val profile = if (profileId == null) repository.ownProfile() else repository.profile(profileId)
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
            if (result is ApiResult.Success) {
                mutableState.value = UiState.Content(current.data.copy(connected = result.data))
            }
        }
    }
}
