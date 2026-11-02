package com.project.solaria_mobile

import com.project.solaria_mobile.feature.discovery.buildScheduleState
import com.project.solaria_mobile.feature.discovery.data.toScheduleEvent
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import com.project.solaria_mobile.feature.project.data.ProjectActivityDto
import com.project.solaria_mobile.feature.project.data.ProjectDto
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun event(start: String, end: String? = null, id: String = "a1") = ScheduleEvent(id, "p1", "Projeto", "Atividade", start, end)

    @Test fun dateUsesLocalTimezoneAndDoesNotRepeatInAnotherYear() {
        val activity = event("2027-01-01T01:00:00Z")
        val state = buildScheduleState(listOf(activity), LocalDate.of(2026, 12, 31), zone)
        assertEquals(listOf(activity), state.selectedEvents)
        assertTrue(state.calendarDays.single { it.inCurrentMonth && it.number == 31 }.hasEvents)
        assertTrue(buildScheduleState(listOf(activity), LocalDate.of(2027, 12, 31), zone).selectedEvents.isEmpty())
        assertTrue(buildScheduleState(listOf(activity), LocalDate.of(2027, 1, 1), zone).selectedEvents.isEmpty())
    }

    @Test fun multiDayActivityIncludesEveryDayButNotItsExclusiveMidnightEnd() {
        val activity = event("2026-10-25T23:00:00-03:00", "2026-10-28T00:00:00-03:00")
        assertFalse(activity.occursOn(LocalDate.of(2026, 10, 24), zone))
        for (day in 25..27) assertTrue(activity.occursOn(LocalDate.of(2026, 10, day), zone))
        assertFalse(activity.occursOn(LocalDate.of(2026, 10, 28), zone))
    }

    @Test fun invalidOrEarlierEndIsTreatedAsPointEvent() {
        for (end in listOf(null, "invalid", "2026-10-24T10:00:00-03:00")) {
            val activity = event("2026-10-25T10:00:00-03:00", end)
            assertTrue(activity.occursOn(LocalDate.of(2026, 10, 25), zone))
            assertFalse(activity.occursOn(LocalDate.of(2026, 10, 26), zone))
        }
    }

    @Test fun selectedEventsAreChronologicalAndEmptyCalendarStillHasLeapDay() {
        val early = event("2028-02-29T09:00:00-03:00", id = "early")
        val late = event("2028-02-29T18:00:00-03:00", id = "late")
        val state = buildScheduleState(listOf(late, early), LocalDate.of(2028, 2, 29), zone)
        assertEquals(listOf(early, late), state.selectedEvents)
        val empty = buildScheduleState(emptyList(), LocalDate.of(2028, 2, 29), zone)
        assertEquals(42, empty.calendarDays.size)
        assertEquals(29, empty.calendarDays.count { it.inCurrentMonth })
        assertTrue(empty.selectedEvents.isEmpty())
    }

    @Test fun onlyActivitiesWithValidStartAreScheduledAndProjectIdentityIsPreserved() {
        val project = ProjectDto("p2", "Obra")
        assertNull(ProjectActivityDto("a", startsAt = null).toScheduleEvent(project))
        assertNull(ProjectActivityDto("a", startsAt = "bad-date").toScheduleEvent(project))
        val mapped = ProjectActivityDto("a", title = "Vistoria", startsAt = "2026-10-26T09:00:00Z", status = "CANCELED").toScheduleEvent(project)!!
        assertEquals("p2", mapped.projectId)
        assertEquals("Obra", mapped.projectName)
        assertEquals("CANCELED", mapped.status)
    }
}
