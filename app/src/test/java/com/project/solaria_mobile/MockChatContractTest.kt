package com.project.solaria_mobile

import com.project.solaria_mobile.core.mock.MockApiData
import com.project.solaria_mobile.feature.communication.data.ConversationStateRequest
import com.project.solaria_mobile.feature.communication.data.SendMessageRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockChatContractTest {
    @Test fun restartingMockServerChangesSyncEpochConsistently() {
        val first = MockApiData()
        val restarted = MockApiData()
        assertEquals(first.updates().syncEpoch, first.conversation("qa-conversation-1")!!.syncEpoch)
        assertEquals(restarted.updates().syncEpoch, restarted.conversation("qa-conversation-1")!!.syncEpoch)
        assertFalse(first.updates().syncEpoch == restarted.updates().syncEpoch)
    }

    @Test fun discoveredPeopleHaveAuthIdentifiersForDirectChat() {
        val data = MockApiData()
        val people = data.people("", 1, false).items
        assertTrue(people.isNotEmpty())
        assertTrue(people.all { !it.authId.isNullOrBlank() })
        assertNotNull(data.person(people.first().id)?.authId)
    }

    @Test fun directChatReusesConversationAndMapsCurrentActor() {
        val data = MockApiData()
        val first = data.direct("qa-person-1", "real-session")
        val second = data.direct("qa-person-1", "real-session")
        val visible = data.chatForActor(first, "real-session")
        assertEquals(first.id, second.id)
        assertTrue("real-session" in visible.participantIds)
        assertFalse("qa-mock-user" in visible.participantIds)
    }

    @Test fun conversationSummaryReflectsSentMessageAndStableClientIdentifier() {
        val data = MockApiData()
        val sent = data.sendMessage(SendMessageRequest("qa-conversation-1", "USER_TO_USER", "Mensagem nova", "stable-client-id"), "Bearer ${tokenFor("actor")}")
        val summary = data.conversation("qa-conversation-1")!!
        assertEquals("stable-client-id", sent.clientMessageId)
        assertEquals("Mensagem nova", summary.lastMessage?.content)
        assertEquals(sent.sequence, summary.lastMessage?.sequence)
        assertTrue(data.updates().messages.any { it.id == sent.id })
    }

    @Test fun archivedChatsRemainAvailableSeparatelyFromActiveOnes() {
        val data = MockApiData()
        val state = data.conversationState("qa-conversation-1", ConversationStateRequest(archived = true, muted = true))!!
        assertTrue(state.archived)
        assertTrue(state.muted)
        assertFalse(data.conversations().any { it.id == "qa-conversation-1" })
        assertTrue(data.conversations(true).any { it.id == "qa-conversation-1" })
    }
}
