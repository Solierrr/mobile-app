package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.NotificationGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repository: CommunicationRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<List<NotificationGroup>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<NotificationGroup>>> = mutableState

    init { load() }

    fun load() {
        viewModelScope.launch { mutableState.load(isEmpty = { it.isEmpty() }) { repository.notifications() } }
    }
}
