package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatMessageActionsTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test fun muteIgnoresRepeatedTapWhileAwaitingServer() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val repository = object : CommunicationRepository by FakeCommunicationRepository() {
            override suspend fun setMuted(conversationId: String, muted: Boolean): ApiResult<Unit> {
                calls++
                gate.await()
                return ApiResult.Success(Unit)
            }
        }
        val viewModel = ChatViewModel(SavedStateHandle(mapOf("id" to "c1")), repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.toggleMute()
        viewModel.toggleMute()
        advanceUntilIdle()
        assertEquals(1, calls)
        assertTrue((viewModel.uiState.value as UiState.Content).data.busy)
        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse((viewModel.uiState.value as UiState.Content).data.busy)
    }
}
