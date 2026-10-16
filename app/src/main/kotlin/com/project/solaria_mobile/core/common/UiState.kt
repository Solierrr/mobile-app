package com.project.solaria_mobile.core.common

import kotlinx.coroutines.flow.MutableStateFlow

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<out T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val kind: ErrorKind) : UiState<Nothing>
}

inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Content -> UiState.Content(transform(data))
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
    value = UiState.Loading
    value = block().toUiState(isEmpty)
}
