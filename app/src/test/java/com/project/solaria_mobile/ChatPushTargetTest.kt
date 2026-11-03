package com.project.solaria_mobile

import com.project.solaria_mobile.feature.communication.data.chatPushTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatPushTargetTest {
    @Test fun aNewMessageOpensItsConversationWithoutANotificationId() {
        assertEquals("c-1029", chatPushTarget(mapOf("type" to "NEW_MESSAGE", "conversationId" to "c-1029")))
        assertEquals("c-1029", chatPushTarget(mapOf("type" to "NEW_MESSAGE", "chatId" to "c-1029")))
    }
    @Test fun otherNotificationsAndInvalidDestinationsDoNotOpenAChat() {
        assertNull(chatPushTarget(mapOf("type" to "PROJECT_UPDATED", "conversationId" to "c-1029")))
        assertNull(chatPushTarget(mapOf("type" to "NEW_MESSAGE", "conversationId" to "../secret")))
        assertNull(chatPushTarget(mapOf("type" to "NEW_MESSAGE")))
    }
}
