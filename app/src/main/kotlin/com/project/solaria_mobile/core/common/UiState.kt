package com.project.solaria_mobile.core.common

import kotlinx.coroutines.flow.MutableStateFlow

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<out T>(
        val data: T,
        val refreshing: Boolean = false,
        val syncedAt: Long? = null,
        val cached: Boolean = false,
        val refreshError: ErrorKind? = null,
    ) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val kind: ErrorKind) : UiState<Nothing>
}

inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Content -> UiState.Content(transform(data), refreshing, syncedAt, cached, refreshError)
    UiState.Loading -> UiState.Loading
    UiState.Empty -> UiState.Empty
    is UiState.Error -> this
}

fun <T> ApiResult<T>.toUiState(isEmpty: (T) -> Boolean = { false }): UiState<T> = when (this) {
    is ApiResult.Success -> if (isEmpty(data)) UiState.Empty else UiState.Content(data)
    is ApiResult.Failure -> UiState.Error(kind)
}

suspend fun <T> MutableStateFlow<UiState<T>>.load(
    isEmpty: (T) -> Boolean = { false },
    block: suspend () -> ApiResult<T>,
) {
    val previous = value as? UiState.Content
    value = previous?.copy(refreshing = true, refreshError = null) ?: UiState.Loading
    val result = block()
    value = if (result is ApiResult.Failure && previous != null && result.kind.canRetainContent()) {
        previous.copy(refreshing = false, refreshError = result.kind)
    } else result.toUiState(isEmpty)
}
