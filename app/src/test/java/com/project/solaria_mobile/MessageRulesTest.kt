package com.project.solaria_mobile

import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.MUTATION_WINDOW_MILLIS
import com.project.solaria_mobile.feature.communication.domain.MessageStatus
import com.project.solaria_mobile.feature.communication.domain.canChange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes da regra que decide se o usuário pode editar ou excluir uma mensagem: autoria, estado, conversa, rede e prazo. */
class MessageRulesTest {
    private val now = 10_000_000L

    private fun message(
        mine: Boolean = true,
        status: MessageStatus = MessageStatus.SENT,
        deleted: Boolean = false,
        sentAt: Long? = now - 60_000,
    ) = ChatMessage("m1", "oi", mine, "10:00", sequence = 1, status = status, deleted = deleted, sentAt = sentAt)

    private fun conversation(chatbot: Boolean = false, lost: Boolean = false) =
        Conversation("c1", "AB", "Ana", "oi", "10:00", AvatarTone.BLUE, accessLost = lost, chatbot = chatbot)

    private fun allowed(message: ChatMessage = message(), conversation: Conversation = conversation(), online: Boolean = true) =
        canChange(message, conversation, online, now)

    @Test fun ownConfirmedMessageInsideTheWindowCanChange() {
        assertTrue(allowed())
    }

    @Test fun theWindowIsThirtyMinutesAndTheEdgeIsExcluded() {
        assertTrue(allowed(message(sentAt = now - MUTATION_WINDOW_MILLIS + 1)))
        assertFalse(allowed(message(sentAt = now - MUTATION_WINDOW_MILLIS)))
        assertFalse(allowed(message(sentAt = now - MUTATION_WINDOW_MILLIS - 1)))
    }

    @Test fun otherPeoplesMessagesNeverChange() {
        assertFalse(allowed(message(mine = false)))
    }

    @Test fun queuedAndFailedMessagesCannotBeChangedBecauseTheServerDoesNotKnowThem() {
        assertFalse(allowed(message(status = MessageStatus.PENDING)))
        assertFalse(allowed(message(status = MessageStatus.FAILED)))
    }

    @Test fun deletedMessageCannotBeChangedAgain() {
        assertFalse(allowed(message(deleted = true)))
    }

    @Test fun chatbotAndLostAccessConversationsBlockChanges() {
        assertFalse(allowed(conversation = conversation(chatbot = true)))
        assertFalse(allowed(conversation = conversation(lost = true)))
    }

    @Test fun changesNeedInternetBecauseTheyAreNotQueued() {
        assertFalse(allowed(online = false))
    }

    @Test fun messageWithoutSendTimeCannotBeChecked() {
        assertFalse(allowed(message(sentAt = null)))
    }
}
