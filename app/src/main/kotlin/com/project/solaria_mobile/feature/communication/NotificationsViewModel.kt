package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class NotificationsViewModel @Inject constructor(repository: CommunicationRepository) : ViewModel() {
    val groups: StateFlow<List<Pair<String, List<ServiceCall>>>> = repository.notifications.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.notifications.value
    )
}
