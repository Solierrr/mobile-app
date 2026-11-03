package com.project.solaria_mobile

import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.canModify
import java.time.Instant
import java.time.OffsetDateTime
import org.junit.Assert.*
import org.junit.Test

class MessageEditWindowTest {
    private val now = Instant.parse("2026-11-03T00:00:00Z")
    @Test fun onlyOwnMessagesInsideThirtyMinutesCanBeChanged() {
        val own = ChatMessage("oi", true, "", id = "m1", sentAt = OffsetDateTime.parse("2026-11-02T23:30:01Z"))
        assertTrue(own.canModify(now))
        assertTrue(own.copy(sentAt = OffsetDateTime.parse("2026-11-02T23:30:00Z")).canModify(now))
        assertFalse(own.copy(sentAt = OffsetDateTime.parse("2026-11-02T23:29:59Z")).canModify(now))
        assertFalse(own.copy(mine = false).canModify(now))
        assertFalse(own.copy(deleted = true).canModify(now))
        assertFalse(own.copy(sentAt = null).canModify(now))
    }
}
