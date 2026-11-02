package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import java.util.Base64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O centro de eventos do chat: uma assinatura por conexão, no canal da conta, repartida entre mensagens, presença e
 * "digitando", sem depender de nenhum chat aberto.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StompChatRealtimeTest {
    private class FakeClient : RealtimeClient {
        override val state: MutableStateFlow<RealtimeState> = MutableStateFlow(RealtimeState.Disconnected)
        val frames = MutableSharedFlow<String>(extraBufferCapacity = 16)
        val subscribed = mutableListOf<String>()
        var connectCalls = 0
        override fun subscribe(destination: String): Flow<String> { subscribed += destination; return frames }
        override suspend fun send(destination: String, body: String): ApiResult<Unit> = ApiResult.Success(Unit)
        override fun connect() { connectCalls++ }
        override fun disconnect() = Unit
    }

    private val token = "h." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"me-id"}""".toByteArray()) + ".s"
    private val credentials = object : CredentialProvider { override fun accessToken(): String? = token }

    // Presença e retrato saem com `conversationId` nulo; o "digitando" leva a conversa.
    private fun frame(type: String, payload: String, conversationId: String? = null) =
        """{"type":"$type","conversationId":${conversationId?.let { "\"$it\"" } ?: "null"},"eventId":"e","serverTimestamp":"2026-11-02T10:00:00Z","payload":$payload}"""

    private class FakeRecovery(val recovered: List<ChatEvent> = emptyList()) : MessageRecovery {
        var calls = 0
        override suspend fun recover(deliver: suspend (ChatEvent) -> Unit) {
            calls++
            recovered.forEach { deliver(it) }
        }
    }

    private fun TestScope.connected(recovery: MessageRecovery = NoMessageRecovery): Pair<FakeClient, StompChatRealtime> {
        val client = FakeClient()
        val realtime = StompChatRealtime(client, credentials, backgroundScope, recovery)
        client.state.value = RealtimeState.Connected
        runCurrent()
        return client to realtime
    }

    private val snapshot = frame("PRESENCE_SNAPSHOT", """{"presenceEpoch":"e1","presenceVersion":1,"users":{"ana":true}}""")

    @Test fun recoveryRunsOnlyAfterTheAccountChannelAnswersWithItsSnapshot() = runTest {
        val recovery = FakeRecovery()
        val (client, _) = connected(recovery)
        assertEquals(0, recovery.calls)

        client.frames.emit(snapshot)
        runCurrent()
        assertEquals(1, recovery.calls)

        // O retrato parcial de uma conversa nova não repete a recuperação.
        client.frames.emit(frame("PRESENCE_SNAPSHOT", """{"presenceEpoch":"e1","presenceVersion":2,"users":{"beto":true}}"""))
        runCurrent()
        assertEquals(1, recovery.calls)
    }

    @Test fun recoveryRunsAgainAfterEachReconnection() = runTest {
        val recovery = FakeRecovery()
        val (client, _) = connected(recovery)
        client.frames.emit(snapshot)
        runCurrent()

        client.state.value = RealtimeState.Reconnecting(1)
        runCurrent()
        client.state.value = RealtimeState.Connected
        runCurrent()
        client.frames.emit(snapshot)
        runCurrent()

        assertEquals(2, recovery.calls)
    }

    @Test fun recoveredEventsReachTheCollectors() = runTest {
        val message = MessageDto("m9", "c3", "ana", "perdida", sequence = 9)
        val (client, realtime) = connected(FakeRecovery(listOf(ChatEvent.MessageRecovered(message), ChatEvent.Resynced)))
        val seen = mutableListOf<ChatEvent>()
        backgroundScope.launch { realtime.events.collect { seen += it } }
        runCurrent()

        client.frames.emit(snapshot)
        runCurrent()

        assertEquals(listOf<ChatEvent>(ChatEvent.MessageRecovered(message), ChatEvent.Resynced), seen)
    }

    @Test fun subscribesOnlyToTheAccountChannelOncePerConnection() = runTest {
        val (client, _) = connected()
        assertEquals(listOf("/topic/users.me-id.events"), client.subscribed)
    }

    @Test fun presenceStaysUnknownUntilTheInitialSnapshot() = runTest {
        val (client, realtime) = connected()

        client.frames.emit(frame("USER_ONLINE", """{"userId":"Ana","presenceEpoch":"e1","presenceVersion":3}"""))
        runCurrent()
        assertFalse(realtime.presence.first().known)

        client.frames.emit(frame("PRESENCE_SNAPSHOT", """{"subscriptionId":"s","presenceEpoch":"e1","presenceVersion":4,"users":{"beto":true,"caio":false}}"""))
        runCurrent()
        val state = realtime.presence.first()
        assertTrue(state.known)
        assertEquals(setOf("ana", "beto"), state.online)
    }

    @Test fun typingIsKeptPerConversationAndExpiresWithoutRenewal() = runTest {
        val (client, realtime) = connected()

        client.frames.emit(frame("TYPING_STARTED", """{"userId":"ana"}""", conversationId = "c1"))
        runCurrent()
        assertEquals(mapOf("c1" to setOf("ana")), realtime.typingByConversation.first())

        advanceTimeBy(8_001)
        assertEquals(emptyMap<String, Set<String>>(), realtime.typingByConversation.first())
    }

    @Test fun stoppingTypingClearsItAtOnce() = runTest {
        val (client, realtime) = connected()

        client.frames.emit(frame("TYPING_STARTED", """{"userId":"ana"}""", conversationId = "c1"))
        client.frames.emit(frame("TYPING_STOPPED", """{"userId":"ana"}""", conversationId = "c1"))
        runCurrent()

        assertEquals(emptyMap<String, Set<String>>(), realtime.typingByConversation.first())
    }

    @Test fun losingTheConnectionForgetsPresenceAndTyping() = runTest {
        val (client, realtime) = connected()
        client.frames.emit(frame("PRESENCE_SNAPSHOT", """{"presenceEpoch":"e1","presenceVersion":1,"users":{"ana":true}}"""))
        client.frames.emit(frame("TYPING_STARTED", """{"userId":"ana"}""", conversationId = "c1"))
        runCurrent()

        client.state.value = RealtimeState.Reconnecting(1)
        runCurrent()

        assertEquals(PresenceState(), realtime.presence.first())
        assertEquals(emptyMap<String, Set<String>>(), realtime.typingByConversation.first())
    }

    @Test fun reconnectingSubscribesAgainAndTakesANewSnapshot() = runTest {
        val (client, realtime) = connected()
        client.state.value = RealtimeState.Reconnecting(1)
        runCurrent()
        client.state.value = RealtimeState.Connected
        runCurrent()
        client.frames.emit(frame("PRESENCE_SNAPSHOT", """{"presenceEpoch":"e1","presenceVersion":2,"users":{"ana":true}}"""))
        runCurrent()

        assertEquals(2, client.subscribed.size)
        assertEquals(PresenceState(known = true, online = setOf("ana")), realtime.presence.first())
    }

    @Test fun messageEventsGoToEveryCollectorAndAskForTheConnection() = runTest {
        val (client, realtime) = connected()
        val seen = mutableListOf<ChatEvent>()
        backgroundScope.launch { realtime.events.collect { seen += it } }
        runCurrent()

        client.frames.emit(frame("MESSAGE_CREATED", """{"id":"m1","conversationId":"c2","senderId":"ana","content":"oi","sequence":1}""", conversationId = "c2"))
        client.frames.emit(frame("CONVERSATION_REMOVED", """{"id":"c9","syncEpoch":"x","syncVersion":2}"""))
        runCurrent()

        assertEquals(listOf("c2", "c9"), seen.map { it.conversationId() })
        assertEquals(1, client.connectCalls)
    }
}
