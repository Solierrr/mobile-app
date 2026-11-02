package com.project.solaria_mobile.feature.communication.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PresenceTrackerTest {
    @Test fun snapshotListsWhoIsOnlineInLowercase() {
        val tracker = PresenceTracker()
        tracker.snapshot("e1", 5, mapOf("ANA" to true, "rafael" to false))
        assertEquals(setOf("ana"), tracker.online())
    }

    @Test fun partialSnapshotsOnlyAddToWhatIsKnown() {
        val tracker = PresenceTracker()
        tracker.snapshot("e1", 5, mapOf("ana" to true))
        tracker.snapshot("e1", 6, mapOf("beto" to true))
        assertEquals(setOf("ana", "beto"), tracker.online())
    }

    @Test fun anOlderSnapshotDoesNotUndoANewerChangeOfTheSamePerson() {
        val tracker = PresenceTracker()
        tracker.change("ana", online = false, epoch = "e1", version = 9)
        tracker.snapshot("e1", 8, mapOf("ana" to true))
        assertEquals(emptySet<String>(), tracker.online())
    }

    @Test fun versionsAreKeptPerPersonNotForEveryone() {
        val tracker = PresenceTracker()
        tracker.change("ana", online = true, epoch = "e1", version = 20)
        // A versão de Beto é menor que a de Ana, mas é a primeira que se tem dele.
        tracker.change("beto", online = true, epoch = "e1", version = 3)
        assertEquals(setOf("ana", "beto"), tracker.online())
    }

    @Test fun staleChangeOfTheSamePersonIsIgnored() {
        val tracker = PresenceTracker()
        tracker.change("ana", online = true, epoch = "e1", version = 10)
        tracker.change("ana", online = false, epoch = "e1", version = 10)
        tracker.change("ana", online = false, epoch = "e1", version = 7)
        assertEquals(setOf("ana"), tracker.online())
    }

    @Test fun aNewEpochAlwaysApplies() {
        val tracker = PresenceTracker()
        tracker.change("ana", online = true, epoch = "e1", version = 50)
        tracker.change("ana", online = false, epoch = "e2", version = 1)
        assertEquals(emptySet<String>(), tracker.online())
    }

    @Test fun clearForgetsEveryone() {
        val tracker = PresenceTracker()
        tracker.snapshot("e1", 1, mapOf("ana" to true))
        tracker.clear()
        assertEquals(emptySet<String>(), tracker.online())
    }
}
