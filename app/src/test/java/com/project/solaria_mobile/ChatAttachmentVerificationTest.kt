package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.project.solaria_mobile.core.common.*
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.data.*
import com.project.solaria_mobile.feature.communication.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatAttachmentVerificationTest {
    @get:Rule val main = MainDispatcherRule()
    private class Messenger : CommunicationRepository by FakeCommunicationRepository() {
        val history = mutableListOf<ChatMessage>()
        var readable = true
        var failReadAfterSend = false
        var sends = 0
        override suspend fun conversation(id: String) = ApiResult.Success(Conversation(id, "E", "Equipe", "", "", AvatarTone.BLUE, group = true))
        override suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>> =
            if (readable) ApiResult.Success(history.toList()) else ApiResult.Failure(ErrorKind.CONNECTIVITY)
        override suspend fun sendAttachment(conversationId: String, attachment: MessageAttachment, caption: String, clientMessageId: String?): ApiResult<ChatMessage> {
            sends++
            history += ChatMessage("", true, "", clientMessageId, id = "sent", sequence = 1, attachment = attachment)
            if (failReadAfterSend) readable = false
            return ApiResult.Failure(ErrorKind.SERVER, errorCode = "MESSAGE_SEND_UNCERTAIN")
        }
    }

    @Test fun anUncertainAttachmentIsRecognizedByItsClientIdWithoutSendingAgain() = runTest {
        val repository = Messenger()
        val model = ChatViewModel(SavedStateHandle(mapOf("id" to "c1")), repository, testOutbox(repository))
        model.uiState.test {
            advanceUntilIdle()
            model.sendAttachment(MessageAttachment.Location(-23.5, -46.6))
            advanceUntilIdle()
            val ui = (expectMostRecentItem() as UiState.Content).data
            assertEquals("sent", ui.messages.single().id)
            assertNull(ui.sendFailure)
            assertFalse(ui.needsVerification)
            assertEquals(1, repository.sends)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun aFailedVerificationBlocksOtherAttachmentsUntilTheDeliveryCanBeRead() = runTest {
        val repository = Messenger().apply { failReadAfterSend = true }
        val model = ChatViewModel(SavedStateHandle(mapOf("id" to "c1")), repository, testOutbox(repository))
        model.uiState.test {
            advanceUntilIdle()
            model.sendAttachment(MessageAttachment.Location(-23.5, -46.6))
            advanceUntilIdle()
            assertTrue((expectMostRecentItem() as UiState.Content).data.needsVerification)
            model.sendAttachment(MessageAttachment.Location(-23.5, -46.6))
            advanceUntilIdle()
            assertEquals(1, repository.sends)
            repository.readable = true
            model.clearAction()
            advanceUntilIdle()
            assertFalse((expectMostRecentItem() as UiState.Content).data.needsVerification)
            assertEquals(1, repository.sends)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
