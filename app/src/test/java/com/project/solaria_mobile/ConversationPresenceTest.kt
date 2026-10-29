package com.project.solaria_mobile

import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import com.project.solaria_mobile.feature.communication.data.ConversationPresence
import com.project.solaria_mobile.feature.communication.data.LiveState
import com.project.solaria_mobile.feature.communication.data.PresenceState
import com.project.solaria_mobile.feature.communication.data.RealtimeEventDto
import com.project.solaria_mobile.feature.communication.data.reduce
import com.project.solaria_mobile.feature.communication.data.withoutTyping
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes da presença e do "digitando": o redutor dos eventos de `/topic/conversations.{id}`, o prazo local do
 * "digitando" e o envio do "digitando" com a regra dos 3 s.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationPresenceTest {
    private val realtime = FakeRealtimeClient()
    private val json: Json = ApiJson.instance
    private val topic = "/topic/conversations.c1"

    private fun TestScope.presence(): ConversationPresence =
        ConversationPresence(realtime, json, object : CredentialProvider { override fun accessToken() = tokenFor("user-1") }, backgroundScope)
            .also { it.now = { testScheduler.currentTime } }

    private fun event(type: String, payload: String) = """{"type":"$type","conversationId":"c1","payload":$payload}"""

    private fun snapshot(vararg users: Pair<String, Boolean>, version: Long = 1, epoch: String = "p1") = event(
        "PRESENCE_SNAPSHOT",
        """{"presenceEpoch":"$epoch","presenceVersion":$version,"users":{${users.joinToString(",") { "\"${it.first}\":${it.second}" }}}}""",
    )

    private fun change(type: String, user: String, version: Long, epoch: String = "p1") =
        event(type, """{"userId":"$user","presenceEpoch":"$epoch","presenceVersion":$version}""")

    private fun typing(type: String, user: String) = event(type, """{"userId":"$user"}""")

    private fun dto(type: String, payload: String) =
        json.decodeFromString(RealtimeEventDto.serializer(), event(type, payload))

    private fun TestScope.watch(presence: ConversationPresence): List<LiveState> {
        val seen = mutableListOf<LiveState>()
        backgroundScope.launch { presence.observe("c1").collect { seen += it } }
        runCurrent()
        return seen
    }

    private fun publish(body: String) {
        realtime.frames.tryEmit(topic to body)
    }

    @Test fun unknownWhileTheWebSocketIsNotConnected() = runTest {
        val seen = watch(presence())

        assertEquals(LiveState(), seen.last())
    }

    @Test fun snapshotListsWhoIsOnlineWithoutTheUserHimself() = runTest {
        realtime.connection.value = RealtimeState.Connected
        val seen = watch(presence())

        publish(snapshot("user-1" to true, "user-2" to true, "user-3" to false))
        runCurrent()

        assertEquals(LiveState(known = true, online = setOf("user-2")), seen.last())
    }

    @Test fun onlineAndOfflineEventsFollowTheVersionAndStaleOnesAreIgnored() = runTest {
        realtime.connection.value = RealtimeState.Connected
        val seen = watch(presence())
        publish(snapshot("user-2" to false, version = 5))
        runCurrent()

        publish(change("USER_ONLINE", "user-2", 6))
        runCurrent()
        assertEquals(setOf("user-2"), seen.last().online)

        publish(change("USER_OFFLINE", "user-2", 6))
        runCurrent()
        assertEquals(setOf("user-2"), seen.last().online)

        publish(change("USER_OFFLINE", "user-2", 7))
        runCurrent()
        assertEquals(emptySet<String>(), seen.last().online)
    }

    @Test fun aDifferentEpochAlwaysApplies() {
        val start = PresenceState("p1", 50, LiveState(known = true, online = setOf("user-2")))

        val next = start.reduce(dto("USER_OFFLINE", """{"userId":"user-2","presenceEpoch":"p2","presenceVersion":1}"""), json, "user-1")

        assertEquals(emptySet<String>(), next.live.online)
        assertEquals("p2", next.epoch)
    }

    @Test fun presenceChangesBeforeTheSnapshotAreIgnored() {
        val state = PresenceState()

        val next = state.reduce(dto("USER_ONLINE", """{"userId":"user-2","presenceEpoch":"p1","presenceVersion":1}"""), json, "user-1")

        assertSame(state, next)
    }

    @Test fun typingOfOthersAppearsAndOwnTypingIsIgnored() {
        val started = PresenceState().reduce(dto("TYPING_STARTED", """{"userId":"user-2"}"""), json, "user-1")
        assertEquals(setOf("user-2"), started.live.typing)

        val own = started.reduce(dto("TYPING_STARTED", """{"userId":"USER-1"}"""), json, "user-1")
        assertEquals(setOf("user-2"), own.live.typing)

        val stopped = own.reduce(dto("TYPING_STOPPED", """{"userId":"user-2"}"""), json, "user-1")
        assertTrue(stopped.live.typing.isEmpty())
    }

    @Test fun malformedPayloadLeavesTheStateUntouched() {
        val state = PresenceState(live = LiveState(known = true))

        assertSame(state, state.reduce(dto("PRESENCE_SNAPSHOT", """{"users":"nope"}"""), json, "user-1"))
        assertSame(state, state.withoutTyping("user-2"))
    }

    @Test fun typingExpiresLocallyBecauseTheServerExpiresItWithoutWarning() = runTest {
        realtime.connection.value = RealtimeState.Connected
        val seen = watch(presence())
        publish(snapshot("user-2" to true))
        publish(typing("TYPING_STARTED", "user-2"))
        runCurrent()
        assertEquals(setOf("user-2"), seen.last().typing)

        advanceTimeBy(5_900)
        runCurrent()
        assertEquals(setOf("user-2"), seen.last().typing)

        advanceTimeBy(200)
        runCurrent()
        assertTrue(seen.last().typing.isEmpty())
    }

    @Test fun anotherStartRenewsTheLocalDeadline() = runTest {
        realtime.connection.value = RealtimeState.Connected
        val seen = watch(presence())
        publish(snapshot("user-2" to true))
        publish(typing("TYPING_STARTED", "user-2"))
        runCurrent()

        advanceTimeBy(4_000)
        publish(typing("TYPING_STARTED", "user-2"))
        runCurrent()
        advanceTimeBy(4_000)
        runCurrent()

        assertEquals(setOf("user-2"), seen.last().typing)
    }

    @Test fun losingTheConnectionMakesPresenceUnknownAgain() = runTest {
        realtime.connection.value = RealtimeState.Connected
        val seen = watch(presence())
        publish(snapshot("user-2" to true))
        runCurrent()
        assertTrue(seen.last().known)

        realtime.connection.value = RealtimeState.Reconnecting(1)
        runCurrent()

        assertFalse(seen.last().known)
        assertTrue(seen.last().online.isEmpty())
    }

    @Test fun startedIsSentAtMostEveryThreeSecondsAndStoppedAfterFourSecondsOfSilence() = runTest {
        val presence = presence()
        val body = """{"conversationId":"c1"}"""

        presence.typing("c1")
        runCurrent()
        assertEquals(listOf("/app/typing.started" to body), realtime.sent)

        advanceTimeBy(1_000)
        presence.typing("c1")
        runCurrent()
        assertEquals(1, realtime.sent.size)

        advanceTimeBy(2_100)
        presence.typing("c1")
        runCurrent()
        assertEquals(2, realtime.sent.size)

        advanceTimeBy(4_001)
        runCurrent()
        assertEquals("/app/typing.stopped" to body, realtime.sent.last())
        assertEquals(3, realtime.sent.size)

        presence.stopTyping("c1")
        runCurrent()
        assertEquals(3, realtime.sent.size)
    }

    @Test fun stoppedIsSentRightAwayAfterAStartAndCancelsTheAutomaticOne() = runTest {
        val presence = presence()

        presence.typing("c1")
        presence.stopTyping("c1")
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()

        assertEquals(listOf("/app/typing.started", "/app/typing.stopped"), realtime.sent.map { it.first })
    }
}
