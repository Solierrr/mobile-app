package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CallsUiState(val query: String = "", val calls: List<ServiceCall> = emptyList())

@HiltViewModel
class CallsViewModel @Inject constructor(repository: CommunicationRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val uiState: StateFlow<CallsUiState> = combine(repository.calls, query) { items, text ->
        CallsUiState(text, (items + items).filter {
            it.title.contains(text, ignoreCase = true) || it.detail.contains(text, ignoreCase = true)
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CallsUiState(calls = repository.calls.value + repository.calls.value))

    fun onQueryChange(value: String) { query.value = value }
}
