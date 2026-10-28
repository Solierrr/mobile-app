package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.realtime.websocketUrlOf
import com.project.solaria_mobile.feature.communication.ChatUiState
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.MessageOutbox
import com.project.solaria_mobile.feature.communication.data.RemoteCommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.MessageStatus
import com.project.solaria_mobile.feature.communication.domain.NotificationGroup
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.timelineOf
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Testes de sincronização do chat: ordem da timeline, fila offline, reconexão e eventos do tempo real.
 *
 * Roda o [ChatViewModel] e o [RemoteCommunicationRepository] sobre dublês em memória, sem rede nem banco real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatSyncTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private fun sent(sequence: Int, clientId: String? = null) =
        ChatMessage("m$sequence", "mensagem $sequence", false, "10:0$sequence", sequence, clientId)

    private fun queued(clientId: String, status: MessageStatus = MessageStatus.PENDING) =
        ChatMessage(clientId, "na fila $clientId", true, "10:09", clientMessageId = clientId, status = status)

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
     * Dublê de [CommunicationRepository] controlado pelo teste: conexão, mensagens ao vivo, pendentes e reconexões.
     *
     * Registra em [catchUpCursors] os cursores pedidos nas buscas de recuperação.
     */
    private class ControllableRepository : CommunicationRepository {
        val connection = MutableStateFlow(true)
        val live = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 8)
        val pending = MutableStateFlow<List<ChatMessage>>(emptyList())
        val reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
        val history = mutableListOf<ChatMessage>()
        val catchUpCursors = mutableListOf<Int?>()
        var sendResult: ApiResult<Unit> = ApiResult.Success(Unit)

        override val online: Flow<Boolean> = connection
        override suspend fun conversations(): ApiResult<List<Conversation>> = ApiResult.Success(emptyList())
        override suspend fun conversation(id: String): ApiResult<Conversation> =
            ApiResult.Success(Conversation(id, "AB", "Ana Bia", "", "", AvatarTone.BLUE))

        override suspend fun messages(conversationId: String, afterSequence: Int?): ApiResult<List<ChatMessage>> {
            if (afterSequence != null) catchUpCursors += afterSequence
            return ApiResult.Success(history.filter { (it.sequence ?: 0) > (afterSequence ?: 0) })
        }

        override fun liveMessages(conversationId: String): Flow<ChatMessage> = live
        override fun pendingMessages(conversationId: String): Flow<List<ChatMessage>> = pending
        override fun reconnections(): Flow<Unit> = reconnected
        override suspend fun sendMessage(conversationId: String, text: String): ApiResult<Unit> = sendResult
        override suspend fun retryMessage(messageId: String) = Unit
        override suspend fun discardMessage(messageId: String) = Unit
        override suspend fun calls(): ApiResult<List<ServiceCall>> = ApiResult.Success(emptyList())
        override suspend fun call(id: String): ApiResult<ServiceCall> = ApiResult.Failure(ErrorKind.NOT_FOUND)
        override suspend fun notifications(): ApiResult<List<NotificationGroup>> = ApiResult.Success(emptyList())
    }

    private fun TestScope.chatOf(repository: CommunicationRepository): Pair<ChatViewModel, () -> ChatUiState> {
        val viewModel = ChatViewModel(SavedStateHandle(mapOf("id" to "c1")), repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel to { (viewModel.uiState.value as UiState.Content).data }
    }

    @Test fun queuedMessagesShowAfterTheDeliveredOnesAndDisappearOnConfirmation() = runTest {
        val repository = ControllableRepository().apply { history += listOf(sent(1), sent(2)) }
        val (_, state) = chatOf(repository)

        repository.pending.value = listOf(queued("x"))
        advanceUntilIdle()
        assertEquals(listOf("m1", "m2", "x"), state().messages.map { it.id })
        assertEquals(MessageStatus.PENDING, state().messages.last().status)

        repository.live.emit(sent(3, "x"))
        advanceUntilIdle()
        assertEquals(listOf("m1", "m2", "m3"), state().messages.map { it.id })
    }

    @Test fun sendingClearsTheDraftEvenWhileOffline() = runTest {
        val repository = ControllableRepository().apply { connection.value = false }
        val (viewModel, state) = chatOf(repository)

        viewModel.onDraftChange("vai quando voltar")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("", state().draft)
        assertEquals(false, state().online)
    }

    @Test fun draftComesBackWhenTheMessageCannotEvenBeQueued() = runTest {
        val repository = ControllableRepository().apply { sendResult = ApiResult.Failure(ErrorKind.CONNECTIVITY) }
        val (viewModel, state) = chatOf(repository)

        viewModel.onDraftChange("sem tipo da conversa")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("sem tipo da conversa", state().draft)
    }

    @Test fun reconnectionFetchesOnlyWhatArrivedAfterTheLastKnownSequence() = runTest {
        val repository = ControllableRepository().apply { history += listOf(sent(1), sent(2)) }
        val (_, state) = chatOf(repository)

        repository.history += sent(3)
        repository.reconnected.emit(Unit)
        advanceUntilIdle()

        assertEquals(listOf(2), repository.catchUpCursors)
        assertEquals(listOf("m1", "m2", "m3"), state().messages.map { it.id })
    }

    private fun TestScope.remoteRepository(
        api: ScriptedMessengerApi,
        realtime: FakeRealtimeClient,
        online: Boolean,
        dao: InMemoryPendingMessageDao = InMemoryPendingMessageDao(),
    ): RemoteCommunicationRepository {
        val executor = ApiCallExecutor(ApiErrorParser(ApiJson.instance))
        val credentials = object : CredentialProvider { override fun accessToken() = tokenFor("user-1") }
        val network = object : NetworkMonitor { override val isOnline = MutableStateFlow(online) }
        val outbox = MessageOutbox(dao, api, executor, network, credentials, backgroundScope)
        return RemoteCommunicationRepository(api, executor, credentials, outbox, realtime, network, ApiJson.instance, FakeCommunicationRepository())
    }

    @Test fun catchUpFollowsThePagesUntilTheLastOne() = runTest {
        val api = ScriptedMessengerApi().apply { history += (1..250).map { MessageDto("m$it", "c1", sequence = it) } }
        val repository = remoteRepository(api, FakeRealtimeClient(), online = true)

        val result = repository.messages("c1", afterSequence = 0)

        assertEquals((1..250).map { "m$it" }, (result as ApiResult.Success).data.map { it.id })
        assertEquals(3, api.historyCalls)
    }

    @Test fun realtimeMessageEventsBecomeChatMessagesAndOtherEventsAreIgnored() = runTest {
        val realtime = FakeRealtimeClient()
        val repository = remoteRepository(ScriptedMessengerApi(), realtime, online = true)
        val received = mutableListOf<ChatMessage>()
        backgroundScope.launch { repository.liveMessages("c1").toList(received) }
        runCurrent()

        val topic = "/topic/conversations.c1"
        realtime.frames.emit(topic to """{"type":"USER_ONLINE","conversationId":"c1","payload":{"userId":"u2"}}""")
        realtime.frames.emit(topic to "não é json")
        realtime.frames.emit(topic to """{"type":"MESSAGE_CREATED","conversationId":"c1","eventId":"m9","payload":{"id":"m9","conversationId":"c1","senderId":"USER-1","content":"olá","sequence":9,"timestamp":"2026-10-28T10:00:00Z"}}""")
        realtime.frames.emit(topic to """{"type":"MESSAGE_DELETED","conversationId":"c1","payload":{"id":"m8","conversationId":"c1","senderId":"u2","content":null,"deleted":true,"sequence":8}}""")
        runCurrent()

        assertEquals(listOf("m9", "m8"), received.map { it.id })
        assertTrue(received[0].mine)
        assertEquals("Mensagem apagada", received[1].text)
    }

    @Test fun conversationTypeLearnedOnlineIsReusedToQueueWhileOffline() = runTest {
        val api = ScriptedMessengerApi().apply { conversationTypes["g1"] = "GROUP" }
        val dao = InMemoryPendingMessageDao()
        val repository = remoteRepository(api, FakeRealtimeClient(), online = false, dao = dao)

        repository.conversation("g1")
        api.conversationFailure = IOException("sem rede")
        val result = repository.sendMessage("g1", "oi grupo")
        val pending = mutableListOf<List<ChatMessage>>()
        backgroundScope.launch { repository.pendingMessages("g1").toList(pending) }
        runCurrent()

        assertEquals(ApiResult.Success(Unit), result)
        assertEquals("USER_TO_GROUP", dao.rows.single().messageType)
        assertTrue(api.sendRequests.isEmpty())
        assertEquals(listOf("oi grupo"), pending.last().map { it.text })
        assertEquals(MessageStatus.PENDING, pending.last().single().status)
    }
}
