package com.project.solaria_mobile.feature.communication.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationPreviewTest {
    @Test fun previewComesFromServerSummaryWithoutFetchingEveryHistory() {
        val summary = ConversationDto("c1", conversationType = "GROUP", title = "Equipe", lastMessage = LastMessageDto("m1", 3, "Até amanhã"), unreadCount = 3)
        val conversation = summary.toEntity().toConversation("eu", emptyMap())
        assertEquals("Até amanhã", conversation.preview)
        assertEquals(3, conversation.unreadCount)
    }

    @Test fun deletedSummaryNeverExposesOldContent() {
        val summary = ConversationDto("c1", lastMessage = LastMessageDto("m1", 3, "segredo", deleted = true))
        assertEquals("Mensagem apagada", summary.toEntity().toConversation("eu", emptyMap()).preview)
    }
}
