package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ChatAction
import com.project.solaria_mobile.feature.communication.ChatOperation
import com.project.solaria_mobile.feature.communication.ChatUiState
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.testOutbox
import com.project.solaria_mobile.feature.communication.data.toMessage
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.ConversationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatMessageActionsTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    /** Messenger em memória: só a autora edita ou exclui, e o estado pessoal vale para esta conta. */
    private class MessengerRepository(var failure: ErrorKind? = null) : CommunicationRepository by FakeCommunicationRepository() {
        val history = mutableListOf(
            ChatMessage("Olá", mine = false, time = "10:00", id = "m1", sequence = 1),
            ChatMessage("Texto antigo", mine = true, time = "10:01", id = "m2", sequence = 2),
        )
        var state = ConversationState(muted = false, archived = false)
        val patches = mutableListOf<Pair<Boolean?, Boolean?>>()
        val reads = mutableListOf<Int?>()

        override suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>> = ApiResult.Success(history.toList())

        override suspend fun editMessage(id: String, text: String): ApiResult<ChatMessage> = mutate(id) { it.copy(text = text, edited = true) }

        override suspend fun deleteMessage(id: String): ApiResult<ChatMessage> = mutate(id) { it.copy(text = "", deleted = true) }

        override suspend fun conversationState(id: String, muted: Boolean?, archived: Boolean?, lastReadSequence: Int?): ApiResult<ConversationState> {
            patches += muted to archived
            reads += lastReadSequence
            failure?.let { if (muted != null || archived != null) return ApiResult.Failure(it) }
            state = ConversationState(muted ?: state.muted, archived ?: state.archived)
            return ApiResult.Success(state)
        }

        private fun mutate(id: String, change: (ChatMessage) -> ChatMessage): ApiResult<ChatMessage> {
            failure?.let { return ApiResult.Failure(it) }
            val index = history.indexOfFirst { it.id == id }
            if (index < 0) return ApiResult.Failure(ErrorKind.NOT_FOUND)
            if (!history[index].mine) return ApiResult.Failure(ErrorKind.FORBIDDEN)
            history[index] = change(history[index])
            return ApiResult.Success(history[index])
        }
    }

    private fun TestScope.chat(repository: MessengerRepository, handle: SavedStateHandle = SavedStateHandle(mapOf("id" to "c1"))): Pair<ChatViewModel, () -> ChatUiState> {
        val viewModel = ChatViewModel(handle, repository, testOutbox(repository))
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return viewModel to { (viewModel.uiState.value as UiState.Content).data }
    }

    @Test fun editingOwnMessageReplacesItAndRestoresThePreviousDraft() = runTest {
        val repository = MessengerRepository()
        val (viewModel, state) = chat(repository)
        viewModel.onDraftChange("Rascunho em andamento")
        viewModel.startEdit("m2")
        advanceUntilIdle()
        assertEquals("Texto antigo", state().draft)
        assertEquals("m2", state().editingId)

        viewModel.onDraftChange("Texto novo")
        viewModel.send()
        advanceUntilIdle()

        val edited = state().messages.single { it.id == "m2" }
        assertEquals("Texto novo", edited.text)
        assertTrue(edited.edited)
        assertNull(state().editingId)
        assertEquals("Rascunho em andamento", state().draft)
    }

    @Test fun messagesFromOthersCannotBeEditedOrDeleted() = runTest {
        val repository = MessengerRepository()
        val (viewModel, state) = chat(repository)

        viewModel.startEdit("m1")
        viewModel.deleteMessage("m1")
        advanceUntilIdle()

        assertNull(state().editingId)
        assertEquals("Olá", repository.history.first().text)
        assertNull(state().action)
    }

    @Test fun failedDeleteKeepsTheMessageAndExplainsTheResult() = runTest {
        val repository = MessengerRepository(failure = ErrorKind.CONNECTIVITY)
        val (viewModel, state) = chat(repository)

        viewModel.deleteMessage("m2")
        advanceUntilIdle()

        assertEquals("Texto antigo", state().messages.single { it.id == "m2" }.text)
        assertFalse(state().messages.single { it.id == "m2" }.deleted)
        assertEquals(ChatAction.Failed(ChatOperation.DELETE, ErrorKind.CONNECTIVITY), state().action)
    }

    @Test fun confirmedDeleteBecomesAMarker() = runTest {
        val repository = MessengerRepository()
        val (viewModel, state) = chat(repository)

        viewModel.deleteMessage("m2")
        advanceUntilIdle()

        assertTrue(state().messages.single { it.id == "m2" }.deleted)
        assertNull(state().action)
    }

    @Test fun failedEditKeepsTheEditedTextForAnotherTry() = runTest {
        val repository = MessengerRepository()
        val (viewModel, state) = chat(repository)
        viewModel.startEdit("m2")
        viewModel.onDraftChange("Texto novo")
        repository.failure = ErrorKind.SERVER
        viewModel.send()
        advanceUntilIdle()

        assertEquals("Texto novo", state().draft)
        assertEquals("m2", state().editingId)
        assertEquals("Texto antigo", state().messages.single { it.id == "m2" }.text)
    }

    @Test fun muteReadsTheCurrentStateAndArchiveClosesTheChat() = runTest {
        val repository = MessengerRepository()
        val (viewModel, state) = chat(repository)
        assertEquals(false, state().muted)
        assertEquals(null to null, repository.patches.first())
        assertEquals("abrir marca como lido até a última mensagem", 2, repository.reads.first())

        viewModel.setMuted(true)
        advanceUntilIdle()
        assertEquals(true, state().muted)

        viewModel.archive()
        advanceUntilIdle()
        assertTrue(state().archived)
        assertEquals(null to true, repository.patches.last())
    }

    @Test fun deletedMessagesKeepNoTextAndPreviewSaysSo() {
        val dto = MessageDto("m9", "c1", senderId = "eu", content = "segredo", deleted = true)

        val message = dto.toMessage("eu")

        assertTrue(message.deleted)
        assertEquals("", message.text)
        assertEquals("m9", message.id)
    }
}
