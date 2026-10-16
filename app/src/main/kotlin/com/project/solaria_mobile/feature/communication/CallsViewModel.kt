package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CallsUiState(val query: String = "", val calls: List<ServiceCall> = emptyList())

@HiltViewModel
class CallsViewModel @Inject constructor(private val repository: CommunicationRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val source = MutableStateFlow<UiState<List<ServiceCall>>>(UiState.Loading)

    val uiState: StateFlow<UiState<CallsUiState>> = combine(source, query) { loaded, text ->
        loaded.map { items ->
            CallsUiState(text, items.filter {
                it.title.contains(text, ignoreCase = true) || it.detail.contains(text, ignoreCase = true)
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch { source.load(isEmpty = { it.isEmpty() }) { repository.calls() } }
    }

    fun onQueryChange(value: String) { query.value = value }
}
