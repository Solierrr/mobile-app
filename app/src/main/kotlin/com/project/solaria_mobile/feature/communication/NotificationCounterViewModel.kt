package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.communication.data.NotificationUpdates
import com.project.solaria_mobile.feature.communication.domain.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationCounterViewModel @Inject constructor(
    private val repository: NotificationRepository,
    updates: NotificationUpdates,
) : ViewModel() {
    private val mutable = MutableStateFlow<Long?>(null)
    val unread = mutable.asStateFlow()
    private var job: Job? = null

    init { viewModelScope.launch { updates.events.collect { refresh() } } }

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            mutable.value = (repository.unreadCount() as? ApiResult.Success)?.data
        }
    }
}
