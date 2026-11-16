package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.MAX_MESSAGE_LENGTH
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDraftTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test fun restoresDraftAndRejectsOversizedPaste() = runTest {
        val handle = SavedStateHandle(mapOf("id" to "c1", "draft" to "Ainda escrevendo"))
        val viewModel = ChatViewModel(handle, FakeCommunicationRepository())
        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals("Ainda escrevendo", (expectMostRecentItem() as UiState.Content).data.draft)
            viewModel.onDraftChange("a".repeat(MAX_MESSAGE_LENGTH + 1))
            advanceUntilIdle()
            assertEquals("Ainda escrevendo", handle.get<String>("draft"))
            viewModel.onDraftChange("Novo texto")
            advanceUntilIdle()
            assertEquals("Novo texto", handle.get<String>("draft"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun draftAtLimitCanBeSentAndClearsSavedState() = runTest {
        val handle = SavedStateHandle(mapOf("id" to "c1"))
        val viewModel = ChatViewModel(handle, FakeCommunicationRepository())
        viewModel.uiState.test {
            advanceUntilIdle()
            val text = "a".repeat(MAX_MESSAGE_LENGTH)
            viewModel.onDraftChange(text)
            viewModel.send()
            advanceUntilIdle()
            val state = (expectMostRecentItem() as UiState.Content).data
            assertEquals(text, state.messages.last().text)
            assertEquals("", handle.get<String>("draft"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
