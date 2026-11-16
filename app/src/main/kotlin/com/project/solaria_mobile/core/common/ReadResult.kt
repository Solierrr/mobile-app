package com.project.solaria_mobile.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

data class ReadResult<out T>(
    val result: ApiResult<T>,
    val syncedAt: Long? = null,
    val cached: Boolean = false,
    val refreshing: Boolean = false,
    val refreshError: ErrorKind? = null,
)

inline fun <T, R> ReadResult<T>.map(transform: (T) -> R): ReadResult<R> =
    ReadResult(result.map(transform), syncedAt, cached, refreshing, refreshError)

fun <T> readOnce(block: suspend () -> ApiResult<T>): Flow<ReadResult<T>> = flow { emit(ReadResult(block())) }

suspend fun <T> MutableStateFlow<UiState<T>>.collectRead(
    source: Flow<ReadResult<T>>,
    isEmpty: (T) -> Boolean = { false },
) {
    val previous = value as? UiState.Content
    value = previous?.copy(refreshing = true, refreshError = null) ?: UiState.Loading
    source.collect { read ->
        val result = read.result
        value = when {
            result is ApiResult.Success && isEmpty(result.data) && !read.refreshing -> UiState.Empty
            result is ApiResult.Success -> UiState.Content(result.data, read.refreshing, read.syncedAt, read.cached, read.refreshError)
            result is ApiResult.Failure && value is UiState.Content && result.kind.canRetainContent() ->
                (value as UiState.Content<T>).copy(refreshing = false, refreshError = result.kind)
            else -> result.toUiState(isEmpty)
        }
    }
}

internal fun ErrorKind.canRetainContent(): Boolean = this in setOf(
    ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT, ErrorKind.SERVER, ErrorKind.RATE_LIMITED,
)
