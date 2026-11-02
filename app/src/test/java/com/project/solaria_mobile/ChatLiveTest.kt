package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ChatListItem
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.buildChatItems
import com.project.solaria_mobile.feature.communication.clockTime
import com.project.solaria_mobile.feature.communication.data.ChatCapabilities
import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.PresenceState
import com.project.solaria_mobile.feature.communication.data.parseChatEvent
import com.project.solaria_mobile.feature.communication.data.toConversation
import com.project.solaria_mobile.feature.communication.data.testOutbox
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun at(day: Int, hour: Int, minute: Int): OffsetDateTime = OffsetDateTime.of(2026, 10, day, hour, minute, 0, 0, ZoneOffset.UTC)

private fun message(id: String, sender: String?, mine: Boolean, sentAt: OffsetDateTime?, sequence: Int = 0) =
    ChatMessage("texto $id", mine, "", id = id, sequence = sequence, sentAt = sentAt, senderId = sender)

class ChatListItemsTest {
    @Test fun addsOneSeparatorPerDayAndGroupsBySenderWithinFiveMinutes() {
        val items = buildChatItems(
            listOf(
                message("a", "ana", false, at(20, 9, 0)),
                message("b", "ana", false, at(20, 9, 2)),
                message("c", "ana", false, at(20, 9, 30)),
                message("d", "me", true, at(20, 9, 31)),
                message("e", "me", true, at(21, 8, 0)),
            ),
            firstUnreadSequence = null, group = true, zone = ZoneOffset.UTC,
        )
        assertEquals(listOf("day-2026-10-20", "a", "b", "c", "d", "day-2026-10-21", "e"), items.map { it.key })
        val entries = items.filterIsInstance<ChatListItem.Entry>().associateBy { it.message.id }
        assertTrue(entries.getValue("a").firstInGroup && !entries.getValue("a").lastInGroup)
        assertTrue(!entries.getValue("b").firstInGroup && entries.getValue("b").lastInGroup)
        // 28 minutos depois, a mesma pessoa abre uma nova sequência.
        assertTrue(entries.getValue("c").firstInGroup && entries.getValue("c").lastInGroup)
        // O nome só aparece em grupo, na primeira mensagem de outra pessoa.
        assertTrue(entries.getValue("a").showSender)
        assertFalse(entries.getValue("b").showSender)
        assertFalse(entries.getValue("d").showSender)
    }

    @Test fun directConversationNeverShowsTheSender() {
        val items = buildChatItems(listOf(message("a", "ana", false, at(20, 9, 0))), null, group = false, zone = ZoneOffset.UTC)
        assertFalse(items.filterIsInstance<ChatListItem.Entry>().single().showSender)
    }

    @Test fun unreadDividerSitsBeforeTheFirstUnreadAndBreaksTheSequence() {
        val items = buildChatItems(
            listOf(
                message("a", "ana", false, at(20, 9, 0), sequence = 1),
                message("b", "ana", false, at(20, 9, 1), sequence = 2),
            ),
            firstUnreadSequence = 2, group = false, zone = ZoneOffset.UTC,
        )
        assertEquals(listOf("day-2026-10-20", "a", "new-messages", "b"), items.map { it.key })
        assertTrue((items.last() as ChatListItem.Entry).firstInGroup)
    }

    @Test fun messagesWithoutTimestampGetNoSeparator() {
        val items = buildChatItems(listOf(message("a", null, true, null), message("b", null, true, null)), null, group = false)
        assertEquals(listOf("a", "b"), items.map { it.key })
    }
}

class ChatClockTimeTest {
    @Test fun bubbleAlwaysShowsTheTimeOfDayEvenForOlderDays() {
        val old = message("a", "ana", false, at(1, 9, 5))
        assertEquals("09:05", old.clockTime(java.util.Locale.forLanguageTag("pt-BR"), ZoneOffset.UTC))
        assertEquals("fallback", ChatMessage("x", true, "fallback").clockTime(java.util.Locale.forLanguageTag("pt-BR"), ZoneOffset.UTC))
    }
}

class ChatEventParsingTest {
    private fun envelope(type: String, payload: String) = """{"type":"$type","conversationId":"c1","eventId":"e1","serverTimestamp":"2026-10-20T09:00:00Z","payload":$payload}"""

    @Test fun parsesMessageEvents() {
        val body = """{"id":"m1","conversationId":"c1","senderId":"u1","content":"oi","sequence":3,"timestamp":"2026-10-20T09:00:00Z"}"""
        assertEquals("oi", (parseChatEvent(envelope("MESSAGE_CREATED", body)) as ChatEvent.MessageCreated).message.content)
        assertTrue(parseChatEvent(envelope("MESSAGE_EDITED", body)) is ChatEvent.MessageChanged)
        assertTrue(parseChatEvent(envelope("MESSAGE_DELETED", body)) is ChatEvent.MessageChanged)
    }

    // Presença e retrato não pertencem a uma conversa: o servidor manda `conversationId` nulo.
    private fun bare(type: String, payload: String) = """{"type":"$type","conversationId":null,"eventId":"e1","serverTimestamp":"2026-10-20T09:00:00Z","payload":$payload}"""

    @Test fun parsesTypingWithItsConversation() {
        assertEquals(ChatEvent.Typing("c1", "u1", true), parseChatEvent(envelope("TYPING_STARTED", """{"userId":"u1"}""")))
        assertEquals(ChatEvent.Typing("c1", "u1", false), parseChatEvent(envelope("TYPING_STOPPED", """{"userId":"u1"}""")))
        // Sem a conversa não há onde mostrar o aviso.
        assertNull(parseChatEvent(bare("TYPING_STARTED", """{"userId":"u1"}""")))
    }

    @Test fun parsesPresenceWithItsEpochAndVersion() {
        assertEquals(ChatEvent.Presence("u2", true, "e1", 4), parseChatEvent(bare("USER_ONLINE", """{"userId":"u2","presenceEpoch":"e1","presenceVersion":4}""")))
        assertEquals(ChatEvent.Presence("u2", false), parseChatEvent(bare("USER_OFFLINE", """{"userId":"u2"}""")))
        assertEquals(
            ChatEvent.PresenceSnapshot(mapOf("u1" to true, "u2" to false), "e1", 2),
            parseChatEvent(bare("PRESENCE_SNAPSHOT", """{"subscriptionId":null,"presenceEpoch":"e1","presenceVersion":2,"users":{"u1":true,"u2":false}}""")),
        )
    }

    @Test fun parsesConversationUpdatesAsTheListProjection() {
        val event = parseChatEvent(envelope("CONVERSATION_UPDATED", """{"id":"c1","syncEpoch":"x","syncVersion":9,"conversationType":"GROUP","participantIds":["u1","u2"],"lastInteractionAt":"2026-11-02T10:00:00Z","unreadCount":3,"muted":false,"archived":false,"lastMessage":{"id":"m1","senderId":"u1","content":"oi","deleted":false}}"""))
        val conversation = (event as ChatEvent.ConversationUpdated).conversation
        assertEquals("c1", conversation.id)
        assertEquals(3L, conversation.unreadCount)
        assertEquals(listOf("u1", "u2"), conversation.participantIds)
        assertEquals("oi", conversation.lastMessage?.content)
    }

    @Test fun removedConversationComesFromThePayloadId() {
        assertEquals(ChatEvent.ConversationRemoved("c9"), parseChatEvent("""{"type":"CONVERSATION_REMOVED","eventId":"e","payload":{"id":"c9","syncEpoch":"x","syncVersion":2}}"""))
    }

    @Test fun ignoresUnknownTypesAndMalformedBodies() {
        assertNull(parseChatEvent(envelope("CONVERSATION_RENAMED", """{"id":"c1"}""")))
        assertNull(parseChatEvent("não é json"))
        assertNull(parseChatEvent(envelope("MESSAGE_CREATED", "null")))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRealtimeViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class FakeRealtime : ChatRealtime {
        override val events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 16)
        val presenceState = MutableStateFlow(PresenceState())
        val typingState = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
        override val connected: Flow<Boolean> = flowOf(true)
        override val presence: Flow<PresenceState> = presenceState
        override val typingByConversation: Flow<Map<String, Set<String>>> = typingState
        override suspend fun typing(conversationId: String, started: Boolean) = Unit
    }

    private fun viewModel(realtime: ChatRealtime): ChatViewModel {
        val repository = FakeCommunicationRepository()
        return ChatViewModel(SavedStateHandle(mapOf("id" to "c1")), repository, testOutbox(repository), realtime, ChatCapabilities { false })
    }

    @Test fun showsTypingAndPresenceFromTheGlobalState() = runTest {
        val realtime = FakeRealtime()
        val viewModel = viewModel(realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            realtime.presenceState.value = PresenceState(known = true, online = setOf("ana"))
            realtime.typingState.value = mapOf("c1" to setOf("ana"))
            runCurrent()
            val typing = (expectMostRecentItem() as UiState.Content).data
            assertEquals(1, typing.typingCount)
            assertEquals(true, typing.online)
            realtime.typingState.value = emptyMap()
            realtime.presenceState.value = PresenceState(known = true, online = emptySet())
            advanceUntilIdle()
            val idle = (expectMostRecentItem() as UiState.Content).data
            assertEquals(0, idle.typingCount)
            assertEquals(false, idle.online)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun unknownPresenceIsNotShownAsOffline() = runTest {
        val viewModel = viewModel(FakeRealtime())
        viewModel.uiState.test {
            advanceUntilIdle()
            assertNull((expectMostRecentItem() as UiState.Content).data.online)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun ignoresTypingAndMessagesOfOtherConversations() = runTest {
        val realtime = FakeRealtime()
        val viewModel = viewModel(realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            val before = (expectMostRecentItem() as UiState.Content).data.messages.size
            realtime.typingState.value = mapOf("c2" to setOf("ana"))
            realtime.events.emit(ChatEvent.MessageCreated(MessageDto("other-1", "c2", "ana", "outra conversa", "2026-10-20T09:00:00Z", 9)))
            advanceUntilIdle()
            // Nada muda para esta conversa, então o estado não emite de novo: lê o valor atual.
            val state = (viewModel.uiState.value as UiState.Content).data
            assertEquals(0, state.typingCount)
            assertEquals(before, state.messages.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun appliesRecoveredMessagesOnceAndTheirLaterEdits() = runTest {
        val realtime = FakeRealtime()
        val viewModel = viewModel(realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            val before = (expectMostRecentItem() as UiState.Content).data.messages.size
            val recovered = MessageDto("lost-1", "c1", "ana", "perdida", "2026-10-20T09:00:00Z", 60)
            realtime.events.emit(ChatEvent.MessageRecovered(recovered))
            realtime.events.emit(ChatEvent.MessageRecovered(recovered.copy(content = "perdida, editada", edited = true)))
            advanceUntilIdle()
            val messages = (expectMostRecentItem() as UiState.Content).data.messages
            assertEquals(before + 1, messages.size)
            assertEquals("perdida, editada", messages.single { it.id == "lost-1" }.text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun appendsLiveMessagesOnceAndAppliesEdits() = runTest {
        val realtime = FakeRealtime()
        val viewModel = viewModel(realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            val before = (expectMostRecentItem() as UiState.Content).data.messages.size
            val created = MessageDto("live-1", "c1", "ana", "chegou agora", "2026-10-20T09:00:00Z", 50)
            realtime.events.emit(ChatEvent.MessageCreated(created))
            realtime.events.emit(ChatEvent.MessageCreated(created))
            advanceUntilIdle()
            assertEquals(before + 1, (expectMostRecentItem() as UiState.Content).data.messages.size)
            realtime.events.emit(ChatEvent.MessageChanged(created.copy(content = "editada", edited = true)))
            advanceUntilIdle()
            val edited = (expectMostRecentItem() as UiState.Content).data.messages.single { it.id == "live-1" }
            assertEquals("editada", edited.text)
            assertTrue(edited.edited)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

class MessengerEndpointTest {
    @Test fun derivesTheSecureWebSocketFromTheRestAddress() {
        assertEquals("wss://api-messenger-5g2n.onrender.com/messaging/ws", com.project.solaria_mobile.core.network.realtime.messengerWebSocketUrl("https://api-messenger-5g2n.onrender.com/"))
        assertEquals("wss://api-messenger.solarianetwork.site/messaging/ws", com.project.solaria_mobile.core.network.realtime.messengerWebSocketUrl("https://api-messenger.solarianetwork.site"))
        assertEquals("ws://10.0.2.2:8082/messaging/ws", com.project.solaria_mobile.core.network.realtime.messengerWebSocketUrl("http://10.0.2.2:8082/"))
        assertNull(com.project.solaria_mobile.core.network.realtime.messengerWebSocketUrl(" "))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSearchViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class FakePlaces(var result: com.project.solaria_mobile.core.common.ApiResult<List<com.project.solaria_mobile.feature.discovery.domain.GeoPoint>>) :
        com.project.solaria_mobile.feature.discovery.domain.PlaceRepository {
        val queries = mutableListOf<String>()
        override suspend fun geocode(address: String) = com.project.solaria_mobile.core.common.ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.NOT_FOUND)
        override suspend fun search(query: String): com.project.solaria_mobile.core.common.ApiResult<List<com.project.solaria_mobile.feature.discovery.domain.GeoPoint>> {
            queries += query
            return result
        }
        override suspend fun isAvailable() = com.project.solaria_mobile.core.common.ApiResult.Success(true)
    }

    private val paulista = com.project.solaria_mobile.feature.discovery.domain.GeoPoint("Av. Paulista, São Paulo - SP", -23.56, -46.65)

    @Test fun searchesOnlyAfterAPauseAndWithEnoughText() = runTest {
        val places = FakePlaces(com.project.solaria_mobile.core.common.ApiResult.Success(listOf(paulista)))
        val viewModel = com.project.solaria_mobile.feature.communication.LocationSearchViewModel(places)
        viewModel.onQueryChange("av")
        advanceUntilIdle()
        assertEquals(com.project.solaria_mobile.feature.communication.LocationSearch.Idle, viewModel.state.value)
        viewModel.onQueryChange("av p")
        viewModel.onQueryChange("av paulista")
        assertEquals(com.project.solaria_mobile.feature.communication.LocationSearch.Searching, viewModel.state.value)
        advanceUntilIdle()
        // Só a última digitação chega ao geocoder.
        assertEquals(listOf("av paulista"), places.queries)
        assertEquals(com.project.solaria_mobile.feature.communication.LocationSearch.Results(listOf(paulista)), viewModel.state.value)
    }

    @Test fun distinguishesNoResultFromAFailure() = runTest {
        val places = FakePlaces(com.project.solaria_mobile.core.common.ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.NOT_FOUND))
        val viewModel = com.project.solaria_mobile.feature.communication.LocationSearchViewModel(places)
        viewModel.onQueryChange("01310-100")
        advanceUntilIdle()
        assertEquals(com.project.solaria_mobile.feature.communication.LocationSearch.NotFound, viewModel.state.value)
        places.result = com.project.solaria_mobile.core.common.ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.CONNECTIVITY)
        viewModel.onQueryChange("01310-101")
        advanceUntilIdle()
        assertEquals(com.project.solaria_mobile.feature.communication.LocationSearch.Failed, viewModel.state.value)
        viewModel.clear()
        assertEquals(com.project.solaria_mobile.feature.communication.LocationSearch.Idle, viewModel.state.value)
    }
}

class MockProfilePersistenceTest {
    private class MemoryStore(var saved: com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto? = null) : com.project.solaria_mobile.core.mock.MockProfileStore {
        override fun load() = saved
        override fun save(profile: com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto?) { saved = profile }
    }

    private val complete = com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto(
        yearsExperience = 5, serviceRadiusKm = 10, bio = "oi", specialties = listOf("INSTALLATION"),
        address = com.project.solaria_mobile.feature.onboarding.data.TechnicianAddressDto(street = "Rua A", number = "1", neighborhood = "Centro", city = "São Paulo", state = "SP", zipCode = "01310100"),
    )

    @Test fun theProfileSurvivesARestartAndCanBeReset() {
        val store = MemoryStore()
        val first = com.project.solaria_mobile.core.mock.MockApiData(store)
        first.saveTechnicianProfile(complete)
        assertEquals(true, first.completeTechnicianProfile()?.completed)
        // Um novo processo lê o que o anterior guardou: o onboarding não recomeça.
        val second = com.project.solaria_mobile.core.mock.MockApiData(store)
        assertEquals(true, second.technicianProfile().completed)
        second.resetTechnicianProfile()
        assertEquals(false, second.technicianProfile().completed)
        assertNull(store.saved)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantSessionTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class RecordingAssistant(var result: com.project.solaria_mobile.core.common.ApiResult<com.project.solaria_mobile.feature.communication.domain.AssistantReply>) :
        com.project.solaria_mobile.feature.communication.domain.AssistantRepository {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun send(threadId: String, text: String): com.project.solaria_mobile.core.common.ApiResult<com.project.solaria_mobile.feature.communication.domain.AssistantReply> {
            calls += threadId to text
            return result
        }
    }

    private val reply = com.project.solaria_mobile.core.common.ApiResult.Success(com.project.solaria_mobile.feature.communication.domain.AssistantReply("Olá! Posso ajudar.", "ANSWERED", "m-1"))

    private fun repository() = object : com.project.solaria_mobile.feature.communication.domain.CommunicationRepository by FakeCommunicationRepository() {
        override suspend fun conversation(id: String) = com.project.solaria_mobile.core.common.ApiResult.Success(
            com.project.solaria_mobile.feature.communication.domain.Conversation(id, "", "", "", "", com.project.solaria_mobile.feature.communication.domain.AvatarTone.ORANGE, assistant = true, threadId = "thread-7"),
        )
        override suspend fun messages(conversationId: String) = com.project.solaria_mobile.core.common.ApiResult.Success(emptyList<ChatMessage>())
    }

    private fun viewModel(id: String, assistant: RecordingAssistant): ChatViewModel {
        val repository = repository()
        return ChatViewModel(SavedStateHandle(mapOf("id" to id)), repository, testOutbox(repository), com.project.solaria_mobile.feature.communication.data.NoChatRealtime, ChatCapabilities { true }, assistant)
    }

    @Test fun anAssistantSessionSendsThroughTheAssistantInsteadOfTheMessenger() = runTest {
        val assistant = RecordingAssistant(reply)
        val viewModel = viewModel("c-bot", assistant)
        viewModel.uiState.test {
            advanceUntilIdle()
            viewModel.onDraftChange("Preciso de um instalador")
            viewModel.send()
            advanceUntilIdle()
            val state = (expectMostRecentItem() as UiState.Content).data
            // A sessão existente reaproveita o `threadId` guardado na conversa.
            assertEquals(listOf("thread-7" to "Preciso de um instalador"), assistant.calls)
            assertEquals(listOf("Preciso de um instalador", "Olá! Posso ajudar."), state.messages.map { it.text })
            assertTrue(state.outgoing.isEmpty())
            assertFalse(state.assistantThinking)
            // Anexos não fazem sentido na sessão do assistente.
            assertFalse(state.attachmentsEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun aNewSessionUsesTheIdentifierCreatedOnTheDevice() = runTest {
        val assistant = RecordingAssistant(reply)
        val viewModel = viewModel(com.project.solaria_mobile.feature.communication.domain.NEW_SESSION_PREFIX + "abc-123", assistant)
        viewModel.uiState.test {
            advanceUntilIdle()
            viewModel.onDraftChange("Oi")
            viewModel.send()
            advanceUntilIdle()
            assertEquals(listOf("abc-123" to "Oi"), assistant.calls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun aFailedTurnKeepsTheMessageForRetry() = runTest {
        val assistant = RecordingAssistant(com.project.solaria_mobile.core.common.ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.TIMEOUT))
        val viewModel = viewModel("c-bot", assistant)
        viewModel.uiState.test {
            advanceUntilIdle()
            viewModel.onDraftChange("Pergunta longa")
            viewModel.send()
            advanceUntilIdle()
            val failed = (expectMostRecentItem() as UiState.Content).data
            assertEquals(com.project.solaria_mobile.core.common.ErrorKind.TIMEOUT, failed.outgoing.single().failure)
            assertFalse(failed.assistantThinking)
            assistant.result = reply
            viewModel.retry(failed.outgoing.single().id)
            advanceUntilIdle()
            val retried = (expectMostRecentItem() as UiState.Content).data
            assertTrue(retried.outgoing.isEmpty())
            assertEquals(2, assistant.calls.size)
            assertEquals("Olá! Posso ajudar.", retried.messages.last().text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun theConversationTypeMarksAnAssistantSession() {
        val dto = com.project.solaria_mobile.feature.communication.data.ConversationDto(
            "c1", "CHAT_BOT", userDetails = mapOf("threadId" to kotlinx.serialization.json.JsonPrimitive("t-9")),
        )
        val conversation = dto.toConversation()
        assertTrue(conversation.assistant)
        assertEquals("t-9", conversation.threadId)
        // Sem título, o nome fica vazio para a tela escolher o texto no idioma atual.
        assertEquals("", conversation.name)
    }
}
