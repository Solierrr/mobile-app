package com.project.solaria_mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

data class ConnectionsUiState(val query: String = "", val connections: List<Connection> = emptyList(),
    val discover: Boolean = false, val hasMore: Boolean = false, val loadingMore: Boolean = false)

@HiltViewModel
class ConnectionsViewModel @Inject constructor(private val repository: ProfileRepository,
    network: com.project.solaria_mobile.core.network.NetworkMonitor) : ViewModel() {
    private val source = MutableStateFlow<UiState<ConnectionsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ConnectionsUiState>> = source
    var query = MutableStateFlow(""); private set
    val discover = MutableStateFlow(false)
    private var page = 0
    private var loadedQuery = ""
    private var loadedDiscover = false
    private var request: Job? = null
    init {
        load()
        viewModelScope.launch { network.isOnline.distinctUntilChanged().drop(1).collect { online -> if (online) load() } }
    }

    fun load() { fetch(false) }
    fun more() { if ((source.value as? UiState.Content)?.data?.hasMore == true) fetch(true) }
    fun selectDiscover(value: Boolean) {
        if (discover.value == value) return
        discover.value = value
        query.value = ""
        source.value = UiState.Loading
        load()
    }
    fun onQueryChange(value: String) {
        query.value = value.take(120)
        val current = source.value as? UiState.Content
        if (current != null) source.value = current.copy(data = current.data.copy(query = query.value, hasMore = false), refreshing = true)
        fetch(false, debounce = true)
    }
    private fun fetch(append: Boolean, debounce: Boolean = false) {
        if (append && (source.value as? UiState.Content)?.data?.loadingMore == true) return
        request?.cancel()
        request = viewModelScope.launch {
            if (debounce) delay(300)
            val previous = source.value as? UiState.Content
            source.value = previous?.copy(data = previous.data.copy(loadingMore = append), refreshing = !append,
                refreshError = null) ?: UiState.Loading
            val sameSource = query.value == loadedQuery && discover.value == loadedDiscover
            val next = if (append && sameSource) page + 1 else 0
            val read = repository.readPeople(query.value, next, !discover.value)
            when (val result = read.result) {
                is ApiResult.Failure -> {
                    source.value = if (previous != null && sameSource && result.kind in setOf(ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT, ErrorKind.SERVER))
                        previous.copy(data = previous.data.copy(loadingMore = false), refreshing = false, refreshError = result.kind)
                    else UiState.Error(result.kind)
                }
                is ApiResult.Success -> {
                    page = next
                    loadedQuery = query.value
                    loadedDiscover = discover.value
                    val items = if (append && sameSource) previous?.data?.connections.orEmpty() + result.data.items else result.data.items
                    source.value = UiState.Content(ConnectionsUiState(query.value, items.distinctBy { it.id }, discover.value, result.data.hasMore),
                        syncedAt = read.syncedAt, cached = read.cached, refreshError = read.refreshError)
                }
            }
        }
    }
}
