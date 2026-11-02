package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.canRetainContent
import com.project.solaria_mobile.feature.communication.data.NotificationUpdates
import com.project.solaria_mobile.feature.communication.domain.AccountNotification
import com.project.solaria_mobile.feature.communication.domain.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class NotificationsUiState(
    val items: List<AccountNotification> = emptyList(),
    val unreadCount: Long? = null,
    val unreadOnly: Boolean = false,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val pendingId: String? = null,
    val markingAll: Boolean = false,
    val actionError: ErrorKind? = null,
    val retryOpenId: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    updates: NotificationUpdates,
) : ViewModel() {
    private val mutable = MutableStateFlow<UiState<NotificationsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<NotificationsUiState>> = mutable
    private val opened = Channel<AccountNotification>(Channel.BUFFERED)
    val openings = opened.receiveAsFlow()
    private var unreadOnly = false
    private var loadJob: Job? = null
    private var generation = 0

    init {
        load()
        viewModelScope.launch { updates.events.collect { load() } }
    }

    fun filter(unread: Boolean) {
        val current = (mutable.value as? UiState.Content)?.data
        if (current?.pendingId != null || current?.markingAll == true) return
        if (unreadOnly == unread) return
        unreadOnly = unread
        load(reset = true)
    }

    fun load(reset: Boolean = false) {
        val active = (mutable.value as? UiState.Content)?.data
        if (active?.pendingId != null || active?.markingAll == true) return
        loadJob?.cancel()
        val request = ++generation
        val previous = if (reset) null else mutable.value as? UiState.Content
        mutable.value = previous?.copy(refreshing = true, refreshError = null) ?: UiState.Loading
        val filter = unreadOnly
        loadJob = viewModelScope.launch {
            val read = repository.page(0, filter)
            if (request != generation) return@launch
            when (val result = read.result) {
                is ApiResult.Failure -> mutable.value = if (previous != null && result.kind.canRetainContent())
                    previous.copy(refreshing = false, refreshError = result.kind) else UiState.Error(result.kind)
                is ApiResult.Success -> {
                    val count = repository.unreadCount()
                    if (request != generation) return@launch
                    mutable.value = UiState.Content(
                        NotificationsUiState(result.data.items, (count as? ApiResult.Success)?.data, filter,
                            result.data.page, result.data.page + 1 < result.data.totalPages),
                        syncedAt = read.syncedAt, cached = read.cached, refreshError = read.refreshError,
                    )
                }
            }
        }
    }

    fun loadMore() {
        val loaded = mutable.value as? UiState.Content ?: return
        val current = loaded.data
        if (!current.hasMore || current.loadingMore || loaded.refreshing || current.markingAll || current.pendingId != null) return
        val request = generation
        change { it.copy(loadingMore = true, actionError = null) }
        viewModelScope.launch {
            val read = repository.page(current.page + 1, current.unreadOnly)
            if (request != generation) return@launch
            when (val result = read.result) {
                is ApiResult.Failure -> change { it.copy(loadingMore = false, actionError = result.kind) }
                is ApiResult.Success -> change { it.copy(
                    items = (it.items + result.data.items).distinctBy(AccountNotification::id),
                    page = result.data.page, hasMore = result.data.page + 1 < result.data.totalPages, loadingMore = false,
                ) }
            }
        }
    }

    fun open(item: AccountNotification) {
        val loaded = mutable.value as? UiState.Content ?: return
        if (loaded.refreshing) return
        val current = loaded.data
        if (current.pendingId != null || current.markingAll) return
        val request = generation
        change { it.copy(pendingId = item.id, actionError = null, retryOpenId = null) }
        viewModelScope.launch {
            val result = if (item.readAt != null) ApiResult.Success(item) else repository.markRead(item.id)
            if (request != generation) return@launch
            when (result) {
                is ApiResult.Failure -> change { it.copy(pendingId = null, actionError = result.kind, retryOpenId = item.id) }
                is ApiResult.Success -> {
                    change { state -> state.copy(items = state.items.map { if (it.id == item.id) result.data else it }, pendingId = null) }
                    opened.send(result.data)
                    load()
                }
            }
        }
    }

    fun openById(id: String) {
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,128}"))) return
        val loaded = mutable.value as? UiState.Content ?: return
        if (loaded.refreshing || loaded.data.pendingId != null || loaded.data.markingAll) return
        val item = loaded.data.items.firstOrNull { it.id == id }
        if (item != null) { open(item); return }
        val request = generation
        change { it.copy(pendingId = id, actionError = null, retryOpenId = null) }
        viewModelScope.launch {
            val result = repository.markRead(id)
            if (request != generation) return@launch
            when (result) {
                is ApiResult.Failure -> change { it.copy(pendingId = null, actionError = result.kind, retryOpenId = id) }
                is ApiResult.Success -> {
                    change { it.copy(pendingId = null) }
                    opened.send(result.data)
                    load()
                }
            }
        }
    }

    fun markAllRead() {
        val loaded = mutable.value as? UiState.Content ?: return
        if (loaded.refreshing) return
        val current = loaded.data
        if (current.markingAll || current.pendingId != null) return
        val request = generation
        change { it.copy(markingAll = true, actionError = null) }
        viewModelScope.launch {
            val result = repository.markAllRead()
            if (request != generation) return@launch
            when (result) {
                is ApiResult.Failure -> change { it.copy(markingAll = false, actionError = result.kind) }
                is ApiResult.Success -> {
                    change { state -> state.copy(markingAll = false, items = state.items.map { it.copy(readAt = it.readAt ?: Instant.now().toString()) }) }
                    load()
                }
            }
        }
    }

    private fun change(transform: (NotificationsUiState) -> NotificationsUiState) {
        val current = mutable.value as? UiState.Content ?: return
        mutable.value = current.copy(data = transform(current.data))
    }
}
