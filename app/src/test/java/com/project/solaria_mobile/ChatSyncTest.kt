package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.network.realtime.websocketUrlOf
import com.project.solaria_mobile.feature.communication.ChatUiState
import com.project.solaria_mobile.feature.communication.ChatNotice
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.ConversationsUiState
import com.project.solaria_mobile.feature.communication.ConversationsViewModel
import com.project.solaria_mobile.feature.communication.data.timelineOf
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.MessageStatus
import com.project.solaria_mobile.feature.communication.domain.NotificationGroup
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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

/**
 * Testes das telas de conversa sobre um repositório controlado pelo teste: ordem da timeline, fila offline, estados
 * de abertura do chat e estados da lista (carregando, vazia, com erro e com conteúdo).
 *
 * A sincronização com o servidor é testada em [MessageSyncTest] e a leitura do banco em
 * [RemoteCommunicationRepositoryTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatSyncTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private fun sent(sequence: Int, clientId: String? = null) =
        ChatMessage("m$sequence", "mensagem $sequence", false, "10:0$sequence", sequence, clientId)

    private fun queued(clientId: String, status: MessageStatus = MessageStatus.PENDING) =
        ChatMessage(clientId, "na fila $clientId", true, "10:09", clientMessageId = clientId, status = status)

    private fun conversation(id: String = "c1", name: String = "Ana Bia", accessLost: Boolean = false) =
        Conversation(id, "AB", name, "oi", "10:00", AvatarTone.BLUE, accessLost)

    @Test fun timelineOrdersBySequenceAndHidesQueuedMessagesAlreadyConfirmed() {
        val timeline = timelineOf(listOf(sent(2, "a"), sent(1)), listOf(queued("a"), queued("b")))

        assertEquals(listOf("m1", "m2", "b"), timeline.map { it.id })
    }

    @Test fun websocketUrlComesFromTheMessengerBaseUrl() {
        assertEquals("wss://api.example.com/messaging/ws", websocketUrlOf("https://api.example.com/"))
        assertEquals("ws://10.0.2.2:8080/messaging/ws", websocketUrlOf("http://10.0.2.2:8080"))
        assertNull(websocketUrlOf(""))
    }

    /**
     * Dublê de [CommunicationRepository] controlado pelo teste: conversa guardada, mensagens guardadas, fila de
     * envio, conexão, resultado da sincronização e o resultado de abrir o chat.
     *
     * Registra em [refreshes] e [namesRequests] o que os ViewModels pediram.
     */
    private class ControllableRepository : CommunicationRepository {
        val connection = MutableStateFlow(true)
        val stored = MutableStateFlow<Conversation?>(null)
        val list = MutableStateFlow<List<Conversation>>(emptyList())
        val history = MutableStateFlow<List<ChatMessage>>(emptyList())
        val pending = MutableStateFlow<List<ChatMessage>>(emptyList())
        val sync = MutableStateFlow<ApiResult<Unit>?>(null)
        var openResult: CompletableDeferred<ApiResult<Unit>> = CompletableDeferred(ApiResult.Success(Unit))
        var sendResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var refreshes = 0
        var opens = 0
        val namesRequests = mutableListOf<String?>()
        val edits = mutableListOf<Pair<String, String>>()
        val deletes = mutableListOf<String>()
        var editResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var deleteResult: ApiResult<Unit> = ApiResult.Success(Unit)

        override val online: Flow<Boolean> = connection
        override val syncResult: Flow<ApiResult<Unit>?> = sync
        override fun conversations(archived: Boolean): Flow<List<Conversation>> = list
        override fun conversation(id: String): Flow<Conversation?> = stored
        override fun messages(conversationId: String): Flow<List<ChatMessage>> =
            combine(history, pending) { delivered, queue -> timelineOf(delivered, queue) }

        override suspend fun open(conversationId: String): ApiResult<Unit> {
            opens++
            return openResult.await()
        }

        override fun refresh() {
            refreshes++
        }

        override suspend fun refreshNames(conversationId: String?) {
            namesRequests += conversationId
        }

        override suspend fun sendMessage(conversationId: String, text: String): ApiResult<Unit> = sendResult
        override suspend fun retryMessage(messageId: String) = Unit
        override suspend fun discardMessage(messageId: String) = Unit
        override suspend fun markRead(conversationId: String) = Unit
        override suspend fun setMuted(conversationId: String, muted: Boolean): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun setArchived(conversationId: String, archived: Boolean): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun editMessage(messageId: String, text: String): ApiResult<Unit> {
            edits += messageId to text
            return editResult
        }
        override suspend fun deleteMessage(messageId: String): ApiResult<Unit> {
            deletes += messageId
            return deleteResult
        }
        override suspend fun calls(): ApiResult<List<ServiceCall>> = ApiResult.Success(emptyList())
        override suspend fun call(id: String): ApiResult<ServiceCall> = ApiResult.Failure(ErrorKind.NOT_FOUND)
        override suspend fun notifications(): ApiResult<List<NotificationGroup>> = ApiResult.Success(emptyList())
    }

    // ---- ações do chat: edição e exclusão de mensagem

    private val now = 1_793_181_600_000L

    private fun mine(sequence: Int, sentAt: Long, text: String = "meu texto") =
        ChatMessage("m$sequence", text, true, "10:00", sequence, sentAt = sentAt)

    private fun TestScope.chatAt(repository: CommunicationRepository): Pair<ChatViewModel, () -> UiState<ChatUiState>> {
        val viewModel = ChatViewModel(SavedStateHandle(mapOf("id" to "c1")), repository).also { it.now = { now } }
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel to { viewModel.uiState.value }
    }

    @Test fun recentOwnMessageIsChangeableAndEditingSendsTheNewTextAndLeavesEditMode() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            history.value = listOf(mine(1, now - 60_000))
        }
        val (viewModel, state) = chatAt(repository)
        assertTrue(contentOf(state).messages.single().changeable)

        viewModel.startEdit("m1")
        advanceUntilIdle()
        assertEquals("meu texto", contentOf(state).draft)
        assertEquals("m1", contentOf(state).editing?.id)

        viewModel.onDraftChange("texto corrigido")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(listOf("m1" to "texto corrigido"), repository.edits)
        assertNull(contentOf(state).editing)
        assertEquals("", contentOf(state).draft)
        assertEquals(1, contentOf(state).messages.size)
    }

    @Test fun refusedEditKeepsTheTextAndShowsANoticeUntilDismissed() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            history.value = listOf(mine(1, now - 60_000))
            editResult = ApiResult.Failure(ErrorKind.VALIDATION)
        }
        val (viewModel, state) = chatAt(repository)

        viewModel.startEdit("m1")
        viewModel.onDraftChange("tarde demais")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(ChatNotice.NOT_ALLOWED, contentOf(state).notice)
        assertEquals("tarde demais", contentOf(state).draft)
        assertEquals("m1", contentOf(state).editing?.id)

        viewModel.dismissNotice()
        viewModel.cancelEdit()
        advanceUntilIdle()

        assertNull(contentOf(state).notice)
        assertNull(contentOf(state).editing)
        assertEquals("", contentOf(state).draft)
    }

    @Test fun messageOlderThanThirtyMinutesIsNotChangeableAndEditingItIsIgnored() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            history.value = listOf(mine(1, now - 31 * 60_000))
        }
        val (viewModel, state) = chatAt(repository)

        viewModel.startEdit("m1")
        viewModel.delete("m1")
        advanceUntilIdle()

        assertFalse(contentOf(state).messages.single().changeable)
        assertNull(contentOf(state).editing)
        assertTrue(repository.deletes.isEmpty())
    }

    @Test fun deleteGoesToTheRepositoryAndFailuresBecomeNotices() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            history.value = listOf(mine(1, now - 60_000))
        }
        val (viewModel, state) = chatAt(repository)

        viewModel.delete("m1")
        advanceUntilIdle()
        assertEquals(listOf("m1"), repository.deletes)
        assertNull(contentOf(state).notice)

        repository.deleteResult = ApiResult.Failure(ErrorKind.CONNECTIVITY)
        viewModel.delete("m1")
        advanceUntilIdle()
        assertEquals(ChatNotice.OFFLINE, contentOf(state).notice)

        repository.deleteResult = ApiResult.Failure(ErrorKind.CONFLICT)
        viewModel.delete("m1")
        advanceUntilIdle()
        assertEquals(ChatNotice.TRY_AGAIN, contentOf(state).notice)

        repository.deleteResult = ApiResult.Failure(ErrorKind.NOT_FOUND)
        viewModel.delete("m1")
        advanceUntilIdle()
        assertEquals(ChatNotice.NO_ACCESS, contentOf(state).notice)
    }

    @Test fun offlineMakesEveryMessageUnchangeable() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            history.value = listOf(mine(1, now - 60_000))
            connection.value = false
        }
        val (_, state) = chatAt(repository)

        assertFalse(contentOf(state).messages.single().changeable)
    }

    private fun TestScope.chatOf(repository: CommunicationRepository): Pair<ChatViewModel, () -> UiState<ChatUiState>> {
        val viewModel = ChatViewModel(SavedStateHandle(mapOf("id" to "c1")), repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel to { viewModel.uiState.value }
    }

    private fun TestScope.contentOf(state: () -> UiState<ChatUiState>): ChatUiState = (state() as UiState.Content).data

    @Test fun queuedMessagesShowAfterTheDeliveredOnesAndDisappearOnConfirmation() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            history.value = listOf(sent(1), sent(2))
        }
        val (_, state) = chatOf(repository)

        repository.pending.value = listOf(queued("x"))
        advanceUntilIdle()
        assertEquals(listOf("m1", "m2", "x"), contentOf(state).messages.map { it.id })
        assertEquals(MessageStatus.PENDING, contentOf(state).messages.last().status)

        repository.history.value = listOf(sent(1), sent(2), sent(3, "x"))
        advanceUntilIdle()
        assertEquals(listOf("m1", "m2", "m3"), contentOf(state).messages.map { it.id })
    }

    @Test fun chatOpensFromTheStoredConversationWhileTheNetworkStepIsStillRunning() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            history.value = listOf(sent(1))
            openResult = CompletableDeferred()
        }

        val (_, state) = chatOf(repository)

        assertEquals(listOf("m1"), contentOf(state).messages.map { it.id })
        assertEquals(1, repository.opens)
    }

    @Test fun chatShowsLoadingUntilTheConversationExists() = runTest {
        val repository = ControllableRepository().apply { openResult = CompletableDeferred() }
        val (_, state) = chatOf(repository)
        assertEquals(UiState.Loading, state())

        repository.stored.value = conversation()
        advanceUntilIdle()

        assertTrue(state() is UiState.Content)
    }

    @Test fun chatShowsTheErrorWhenTheConversationCannotBeLoaded() = runTest {
        val repository = ControllableRepository().apply { openResult = CompletableDeferred(ApiResult.Failure(ErrorKind.NOT_FOUND)) }

        val (_, state) = chatOf(repository)

        assertEquals(UiState.Error(ErrorKind.NOT_FOUND), state())
    }

    @Test fun chatbotThatCannotBeOpenedShowsTheErrorEvenWithAStoredConversation() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            openResult = CompletableDeferred(ApiResult.Failure(ErrorKind.CONNECTIVITY))
        }

        val (_, state) = chatOf(repository)

        assertEquals(UiState.Error(ErrorKind.CONNECTIVITY), state())
    }

    @Test fun retryOpensTheChatAgain() = runTest {
        val repository = ControllableRepository().apply { openResult = CompletableDeferred(ApiResult.Failure(ErrorKind.CONNECTIVITY)) }
        val (viewModel, state) = chatOf(repository)
        assertEquals(UiState.Error(ErrorKind.CONNECTIVITY), state())

        repository.stored.value = conversation()
        repository.openResult = CompletableDeferred(ApiResult.Success(Unit))
        viewModel.load()
        advanceUntilIdle()

        assertTrue(state() is UiState.Content)
        assertEquals(2, repository.opens)
    }

    @Test fun chatAsksForTheNamesOfTheOpenConversationAndShowsTheOfflineState() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation(accessLost = true)
            connection.value = false
        }

        val (_, state) = chatOf(repository)

        assertEquals(listOf<String?>("c1"), repository.namesRequests)
        assertFalse(contentOf(state).online)
        assertTrue(contentOf(state).conversation.accessLost)
    }

    @Test fun sendingClearsTheDraftEvenWhileOffline() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            connection.value = false
        }
        val (viewModel, state) = chatOf(repository)

        viewModel.onDraftChange("vai quando voltar")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("", contentOf(state).draft)
        assertEquals(false, contentOf(state).online)
    }

    @Test fun draftComesBackWhenTheMessageCannotEvenBeQueued() = runTest {
        val repository = ControllableRepository().apply {
            stored.value = conversation()
            sendResult = ApiResult.Failure(ErrorKind.FORBIDDEN)
        }
        val (viewModel, state) = chatOf(repository)

        viewModel.onDraftChange("sem acesso")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("sem acesso", contentOf(state).draft)
    }

    private fun TestScope.conversationsOf(repository: CommunicationRepository): Pair<ConversationsViewModel, () -> UiState<ConversationsUiState>> {
        val viewModel = ConversationsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel to { viewModel.uiState.value }
    }

    @Test fun emptyListShowsLoadingUntilTheFirstSyncEnds() = runTest {
        val repository = ControllableRepository()
        val (_, state) = conversationsOf(repository)
        assertEquals(UiState.Loading, state())

        repository.sync.value = ApiResult.Success(Unit)
        advanceUntilIdle()

        assertEquals(UiState.Empty, state())
    }

    @Test fun emptyListAfterAFailedSyncShowsTheError() = runTest {
        val repository = ControllableRepository().apply { sync.value = ApiResult.Failure(ErrorKind.CONNECTIVITY) }

        val (_, state) = conversationsOf(repository)

        assertEquals(UiState.Error(ErrorKind.CONNECTIVITY), state())
    }

    @Test fun storedConversationsShowEvenWhenTheLastSyncFailed() = runTest {
        val repository = ControllableRepository().apply {
            list.value = listOf(conversation("c1", "Ana Bia"), conversation("c2", "Beto", accessLost = true))
            sync.value = ApiResult.Failure(ErrorKind.CONNECTIVITY)
        }

        val (_, state) = conversationsOf(repository)

        val content = (state() as UiState.Content).data
        assertEquals(listOf("Ana Bia", "Beto"), content.conversations.map { it.name })
        assertTrue(content.conversations[1].accessLost)
    }

    @Test fun listFiltersByNameAndPreviewAndRetryAsksForANewSync() = runTest {
        val repository = ControllableRepository().apply {
            list.value = listOf(conversation("c1", "Ana Bia"), conversation("c2", "Beto"))
        }
        val (viewModel, state) = conversationsOf(repository)

        viewModel.onQueryChange("beto")
        advanceUntilIdle()
        assertEquals(listOf("Beto"), (state() as UiState.Content).data.conversations.map { it.name })

        viewModel.load()
        assertEquals(1, repository.refreshes)
        assertEquals(listOf<String?>(null), repository.namesRequests)
    }
}
