package com.project.solaria_mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConnectionsUiState(val query: String = "", val connections: List<Connection> = emptyList())

@HiltViewModel
class ConnectionsViewModel @Inject constructor(private val repository: ProfileRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val source = MutableStateFlow<UiState<List<Connection>>>(UiState.Loading)

    val uiState: StateFlow<UiState<ConnectionsUiState>> = combine(source, query) { loaded, text ->
        loaded.map { items ->
            ConnectionsUiState(text, items.filter {
                it.name.contains(text, ignoreCase = true) || it.handle.contains(text, ignoreCase = true)
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch { source.load(isEmpty = { it.isEmpty() }) { repository.connections() } }
    }

    fun onQueryChange(value: String) { query.value = value }
}
