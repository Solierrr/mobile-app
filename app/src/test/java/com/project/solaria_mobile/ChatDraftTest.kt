package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.MAX_MESSAGE_LENGTH
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.project.solaria_mobile.feature.communication.data.testOutbox

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDraftTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test fun restoresDraftFromSavedState() = runTest {
        val handle = SavedStateHandle(mapOf("id" to "c1", "draft" to "Ainda estou escrevendo"))
        val repository = FakeCommunicationRepository()
        val viewModel = ChatViewModel(handle, repository, testOutbox(repository))
        viewModel.uiState.test {
            advanceUntilIdle()
            val state = (expectMostRecentItem() as UiState.Content).data
            assertEquals("Ainda estou escrevendo", state.draft)
            viewModel.onDraftChange("Rascunho alterado")
            advanceUntilIdle()
            assertEquals("Rascunho alterado", handle.get<String>("draft"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun oversizedPasteKeepsPreviousDraftAndShowsTheLimit() = runTest {
        var sends = 0
        val repository = object : CommunicationRepository by FakeCommunicationRepository() {
            override suspend fun sendMessage(conversationId: String, text: String, clientMessageId: String?): ApiResult<ChatMessage> {
                sends++
                return ApiResult.Success(ChatMessage(text, true, "Agora"))
            }
        }
        val outbox = testOutbox(repository)
        val viewModel = ChatViewModel(SavedStateHandle(mapOf("id" to "c1")), repository, outbox)
        viewModel.uiState.test {
            advanceUntilIdle()
            val initial = (expectMostRecentItem() as UiState.Content).data.messages
            viewModel.onDraftChange("Texto que já estava aqui")
            viewModel.onDraftChange("a".repeat(MAX_MESSAGE_LENGTH + 1))
            viewModel.send()
            advanceUntilIdle()
            outbox.drain()
            advanceUntilIdle()
            val state = (expectMostRecentItem() as UiState.Content).data
            assertEquals(0, sends)
            assertEquals("Texto que já estava aqui", state.draft)
            assertEquals(true, state.draftLimitExceeded)
            assertEquals(initial, state.messages)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun draftAtServerLimitCanBeSentAndClearsSavedState() = runTest {
        val handle = SavedStateHandle(mapOf("id" to "c1"))
        val repository = FakeCommunicationRepository()
        val outbox = testOutbox(repository)
        val viewModel = ChatViewModel(handle, repository, outbox)
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            val draft = "a".repeat(MAX_MESSAGE_LENGTH)
            viewModel.onDraftChange(draft)
            viewModel.send()
            advanceUntilIdle()
            outbox.drain()
            advanceUntilIdle()
            val state = (expectMostRecentItem() as UiState.Content).data
            assertEquals(draft, state.messages.last().text)
            assertEquals("", state.draft)
            assertEquals("", handle.get<String>("draft"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
