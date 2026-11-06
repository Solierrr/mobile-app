package com.project.solaria_mobile

import com.project.solaria_mobile.feature.discovery.data.ShiftDto
import com.project.solaria_mobile.feature.discovery.data.planShiftChanges
import com.project.solaria_mobile.feature.discovery.data.toJourney
import com.project.solaria_mobile.feature.discovery.domain.BusySlot
import com.project.solaria_mobile.feature.discovery.domain.WorkJourney
import com.project.solaria_mobile.feature.discovery.domain.WorkShift
import com.project.solaria_mobile.feature.profile.data.SelfAffiliationDto
import com.project.solaria_mobile.feature.profile.data.SelfPersonDto
import com.project.solaria_mobile.feature.profile.data.SelfProfileDto
import com.project.solaria_mobile.feature.profile.data.SelfTechnicianDto
import com.project.solaria_mobile.feature.profile.data.toProfile
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkJourneyTest {
    private fun shift(id: String, day: String, start: String = "08:00", end: String = "17:00") =
        ShiftDto(id, "t1", day, "2000-01-01T$start:00", "2000-01-01T$end:00")

    private val nineToFive = WorkShift(LocalTime.of(9, 0), LocalTime.of(17, 0))

    @Test fun aShiftMustEndAfterItStarts() {
        assertThrows(IllegalArgumentException::class.java) { WorkShift(LocalTime.of(17, 0), LocalTime.of(8, 0)) }
        assertThrows(IllegalArgumentException::class.java) { WorkShift(LocalTime.of(8, 0), LocalTime.of(8, 0)) }
    }

    @Test fun theShiftContainsItsStartButNotItsEnd() {
        assertTrue(nineToFive.contains(LocalTime.of(9, 0)))
        assertTrue(nineToFive.contains(LocalTime.of(16, 59)))
        assertFalse(nineToFive.contains(LocalTime.of(17, 0)))
        assertFalse(nineToFive.contains(LocalTime.of(8, 59)))
    }

    @Test fun serverShiftsBecomeAJourneyByWeekday() {
        val journey = listOf(shift("1", "MONDAY"), shift("2", "SATURDAY", "09:00", "12:00")).toJourney()
        assertEquals(WorkShift(LocalTime.of(8, 0), LocalTime.of(17, 0)), journey.on(DayOfWeek.MONDAY))
        assertEquals(WorkShift(LocalTime.of(9, 0), LocalTime.of(12, 0)), journey.on(DayOfWeek.SATURDAY))
        assertNull(journey.on(DayOfWeek.SUNDAY))
    }

    @Test fun savingCreatesWhatIsMissingAndRemovesDaysOff() {
        val current = listOf(shift("1", "MONDAY"), shift("2", "TUESDAY"))
        val wanted = WorkJourney(mapOf(DayOfWeek.MONDAY to WorkShift(LocalTime.of(8, 0), LocalTime.of(17, 0)), DayOfWeek.WEDNESDAY to nineToFive))

        val plan = planShiftChanges(current, wanted)

        assertEquals(listOf("2"), plan.delete)
        assertEquals(emptyList<Any>(), plan.update)
        assertEquals(listOf(DayOfWeek.WEDNESDAY to nineToFive), plan.create)
    }

    @Test fun savingUpdatesOnlyTheDaysWhoseHoursChanged() {
        val current = listOf(shift("1", "MONDAY"), shift("2", "TUESDAY"))
        val wanted = WorkJourney(mapOf(DayOfWeek.MONDAY to WorkShift(LocalTime.of(8, 0), LocalTime.of(17, 0)), DayOfWeek.TUESDAY to nineToFive))

        val plan = planShiftChanges(current, wanted)

        assertEquals(listOf(Triple("2", DayOfWeek.TUESDAY, nineToFive)), plan.update)
        assertEquals(emptyList<String>(), plan.delete)
        assertEquals(emptyList<Any>(), plan.create)
    }

    @Test fun duplicatedServerShiftsOfTheSameDayAreCollapsed() {
        val current = listOf(shift("1", "MONDAY"), shift("2", "MONDAY", "13:00", "18:00"))
        val wanted = WorkJourney(mapOf(DayOfWeek.MONDAY to WorkShift(LocalTime.of(8, 0), LocalTime.of(17, 0))))

        assertEquals(listOf("2"), planShiftChanges(current, wanted).delete)
    }

    @Test fun anEmptyJourneyRemovesEverything() {
        val plan = planShiftChanges(listOf(shift("1", "MONDAY"), shift("2", "FRIDAY")), WorkJourney())
        assertEquals(listOf("1", "2"), plan.delete)
    }

    @Test fun aBusySlotOverlapsOnlyWhenItIntersectsTheWindow() {
        val slot = BusySlot(Instant.parse("2026-10-31T15:00:00Z"), Instant.parse("2026-10-31T16:00:00Z"))
        assertTrue(slot.overlaps(Instant.parse("2026-10-31T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z")))
        assertFalse(slot.overlaps(Instant.parse("2026-10-31T16:00:00Z"), Instant.parse("2026-11-01T00:00:00Z")))
        assertFalse(slot.overlaps(Instant.parse("2026-10-30T00:00:00Z"), Instant.parse("2026-10-31T15:00:00Z")))
    }

    @Test fun onlyAnIndependentProfessionalIsMarkedAsIndependent() {
        fun profile(vararg types: String, technician: Boolean = true) = SelfProfileDto(
            id = "u1",
            person = SelfPersonDto("Ana Lima"),
            technician = if (technician) SelfTechnicianDto("t1") else null,
            activeAffiliations = types.map { SelfAffiliationDto(it) },
        ).toProfile()

        assertTrue(profile("INDEPENDENT").independent)
        assertFalse(profile("AFFILIATED").independent)
        assertEquals("t1", profile("INDEPENDENT").technicianId)
        assertNull(profile("INDEPENDENT", technician = false).technicianId)
    }
}
