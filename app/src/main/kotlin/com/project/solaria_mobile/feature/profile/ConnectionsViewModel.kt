package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConnectionsUiState(val query: String = "", val connections: List<Connection> = emptyList())

@HiltViewModel
class ConnectionsViewModel @Inject constructor(repository: ProfileRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val uiState: StateFlow<ConnectionsUiState> = combine(repository.connections, query) { items, text ->
        ConnectionsUiState(text, (items + items).filter { it.name.contains(text, ignoreCase = true) || it.handle.contains(text, ignoreCase = true) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionsUiState(connections = repository.connections.value + repository.connections.value))

    fun onQueryChange(value: String) { query.value = value }
}
