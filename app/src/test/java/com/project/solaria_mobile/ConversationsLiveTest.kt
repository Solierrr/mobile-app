package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ConversationsViewModel
import com.project.solaria_mobile.feature.communication.applyLive
import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.MessagePreviewDto
import com.project.solaria_mobile.feature.communication.data.PresenceState
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.Conversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun row(id: String, activityAt: String?) =
    Conversation(id, id.uppercase(), id, "prévia $id", "10:00", AvatarTone.BLUE, activityAt = activityAt)

private fun update(
    id: String,
    at: String?,
    unread: Long? = null,
    archived: Boolean? = null,
    lastMessage: MessagePreviewDto? = null,
    muted: Boolean? = null,
) = ChatEvent.ConversationUpdated(
    ConversationDto(id, "USER", id, at, muted = muted, archived = archived, unreadCount = unread, lastMessage = lastMessage),
)

class ConversationListLiveTest {
    private val list = listOf(
        row("a", "2026-11-02T10:00:00Z"),
        row("b", "2026-11-02T09:00:00Z"),
        row("c", "2026-11-02T08:00:00Z"),
    )

    @Test fun aNewerInteractionMovesTheRowToTheTopAndKeepsTheOthersInOrder() {
        val result = list.applyLive(update("c", "2026-11-02T11:00:00Z", unread = 2), "me")
        assertEquals(listOf("c", "a", "b"), result.map { it.id })
        assertEquals(2, result.first().unread)
    }

    @Test fun anInteractionBetweenTwoRowsLandsBetweenThem() {
        val result = list.applyLive(update("c", "2026-11-02T09:30:00Z"), "me")
        assertEquals(listOf("a", "c", "b"), result.map { it.id })
    }

    @Test fun sameLastInteractionOnlyChangesTheRowInPlace() {
        val result = list.applyLive(update("b", "2026-11-02T09:00:00Z", muted = true), "me")
        assertEquals(listOf("a", "b", "c"), result.map { it.id })
        assertTrue(result[1].muted)
    }

    @Test fun anUnknownConversationIsInsertedByItsTime() {
        val result = list.applyLive(update("d", "2026-11-02T08:30:00Z"), "me")
        assertEquals(listOf("a", "b", "d", "c"), result.map { it.id })
    }

    @Test fun anOlderUnknownConversationGoesLast() {
        assertEquals(listOf("a", "b", "c", "d"), list.applyLive(update("d", "2026-10-01T08:30:00Z"), "me").map { it.id })
    }

    @Test fun anArchivedUpdateTakesTheRowOutOfTheList() {
        assertEquals(listOf("a", "c"), list.applyLive(update("b", "2026-11-02T09:00:00Z", archived = true), "me").map { it.id })
    }

    @Test fun removalTakesTheRowOut() {
        assertEquals(listOf("a", "b"), list.applyLive(ChatEvent.ConversationRemoved("c"), "me").map { it.id })
    }

    @Test fun otherEventsLeaveTheListAsItIs() {
        assertEquals(list, list.applyLive(ChatEvent.Typing("a", "u1", true), "me"))
        assertEquals(list, list.applyLive(ChatEvent.PresenceSnapshot(mapOf("u1" to true)), "me"))
    }

    @Test fun previewUsesTheServerLastMessageAndMarksTheOwnOnes() {
        val own = list.applyLive(update("a", "2026-11-02T12:00:00Z", lastMessage = MessagePreviewDto("m1", "me", "**oi**")), "me")
        assertEquals("Você: oi", own.first().preview)
        val other = list.applyLive(update("a", "2026-11-02T12:00:00Z", lastMessage = MessagePreviewDto("m2", "ana", "tudo bem?")), "me")
        assertEquals("tudo bem?", other.first().preview)
        val deleted = list.applyLive(update("a", "2026-11-02T12:00:00Z", lastMessage = MessagePreviewDto("m3", "ana", "", deleted = true)), "me")
        assertEquals("Mensagem excluída", deleted.first().preview)
    }

    @Test fun anUpdateWithoutTimeKeepsTheRowWhereItWas() {
        val result = list.applyLive(update("b", null, unread = 1), "me")
        assertEquals(listOf("a", "b", "c"), result.map { it.id })
        assertEquals(1, result[1].unread)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsLiveViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class FakeRealtime : ChatRealtime {
        override val events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 16)
        val presenceState = MutableStateFlow(PresenceState())
        val typingState = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
        override val connected: Flow<Boolean> = MutableStateFlow(true)
        override val presence: Flow<PresenceState> = presenceState
        override val typingByConversation: Flow<Map<String, Set<String>>> = typingState
        override suspend fun typing(conversationId: String, started: Boolean) = Unit
    }

    @Test fun aLiveUpdateReordersTheListWithoutReloading() = runTest {
        val realtime = FakeRealtime()
        val viewModel = ConversationsViewModel(FakeCommunicationRepository(), realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            val before = (expectMostRecentItem() as UiState.Content).data.conversations.map { it.id }
            assertEquals("c1", before.first())

            realtime.events.emit(ChatEvent.ConversationUpdated(ConversationDto("c3", "USER", "Bruce Banner", "2026-11-02T11:00:00Z", unreadCount = 4)))
            advanceUntilIdle()

            val after = (expectMostRecentItem() as UiState.Content).data.conversations
            assertEquals("c3", after.first().id)
            assertEquals(4, after.first().unread)
            assertEquals(before.size, after.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun presenceAndTypingMarkTheRows() = runTest {
        val realtime = FakeRealtime()
        val viewModel = ConversationsViewModel(FakeCommunicationRepository(), realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            // Antes do retrato inicial ninguém aparece online.
            assertEquals(emptySet<String>(), (expectMostRecentItem() as UiState.Content).data.online)

            realtime.presenceState.value = PresenceState(known = true, online = setOf("ana"))
            realtime.typingState.value = mapOf("c2" to setOf("beto"))
            advanceUntilIdle()

            val state = (expectMostRecentItem() as UiState.Content).data
            assertEquals(setOf("c1", "c1-b"), state.online)
            assertEquals(setOf("c2"), state.typing)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
