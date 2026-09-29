package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.feature.profile.data.UserProfile
import com.project.solaria_mobile.feature.profile.data.ProfileRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(val self: Boolean = true, val profile: UserProfile, val connected: Boolean = false)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: ProfileRepository) : ViewModel() {
    val ownState: StateFlow<ProfileUiState> = combine(repository.ownProfile, repository.connected) { profile, connected ->
        ProfileUiState(true, profile, connected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(true, repository.ownProfile.value, repository.connected.value))

    val otherState: StateFlow<ProfileUiState> = combine(repository.otherProfile, repository.connected) { profile, connected ->
        ProfileUiState(false, profile, connected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(false, repository.otherProfile.value, repository.connected.value))

    fun toggleConnection() = repository.toggleConnection()
}
