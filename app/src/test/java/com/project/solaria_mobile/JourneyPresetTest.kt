package com.project.solaria_mobile

import com.project.solaria_mobile.feature.address.domain.ServiceRadius
import com.project.solaria_mobile.feature.discovery.JourneyPreset
import com.project.solaria_mobile.feature.discovery.DaySlot
import com.project.solaria_mobile.feature.discovery.DaysSummary
import com.project.solaria_mobile.feature.discovery.daysSummary
import com.project.solaria_mobile.feature.discovery.JourneyDraft
import com.project.solaria_mobile.feature.discovery.pendingDays
import com.project.solaria_mobile.feature.discovery.toggled
import com.project.solaria_mobile.feature.discovery.withAllHours
import com.project.solaria_mobile.feature.discovery.withHours
import com.project.solaria_mobile.feature.discovery.domain.WorkJourney
import com.project.solaria_mobile.feature.discovery.domain.WorkShift
import com.project.solaria_mobile.feature.discovery.draftOf
import com.project.solaria_mobile.feature.discovery.preset
import com.project.solaria_mobile.feature.discovery.withPreset
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyPresetTest {
    private val nineToSix = LocalTime.of(9, 0) to LocalTime.of(18, 0)
    private val nineToSixShift = WorkShift(LocalTime.of(9, 0), LocalTime.of(18, 0))

    private fun on(draft: JourneyDraft) = draft.filterValues { it != null }.keys

    @Test fun businessDaysTurnOnMondayToFridayOnly() {
        val draft = draftOf(WorkJourney()).withPreset(JourneyPreset.WEEKDAYS)
        assertEquals(WorkJourney.Weekdays.toSet(), on(draft))
    }

    @Test fun everyDayTurnsOnAllSeven() {
        assertEquals(DayOfWeek.entries.toSet(), on(draftOf(WorkJourney()).withPreset(JourneyPreset.EVERY_DAY)))
    }

    @Test fun sundayOffTurnsOnEverythingButSunday() {
        assertEquals(DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY, on(draftOf(WorkJourney()).withPreset(JourneyPreset.SUNDAY_OFF)))
    }

    @Test fun anEmptyJourneyStartsWithoutHours() {
        val draft = draftOf(WorkJourney()).withPreset(JourneyPreset.WEEKDAYS)
        assertEquals(DaySlot(null), draft.getValue(DayOfWeek.MONDAY))
        assertEquals(WorkJourney.Weekdays.toSet(), pendingDays(draft))
    }

    @Test fun hoursForOneDayLeaveTheOtherDaysUnspecified() {
        val draft = draftOf(WorkJourney()).withPreset(JourneyPreset.WEEKDAYS).withHours(DayOfWeek.WEDNESDAY, nineToSix)
        assertEquals(DaySlot(nineToSix), draft.getValue(DayOfWeek.WEDNESDAY))
        assertEquals(DaySlot(null), draft.getValue(DayOfWeek.MONDAY))
        assertNull(draft.getValue(DayOfWeek.SUNDAY))
        assertEquals(WorkJourney.Weekdays.toSet() - DayOfWeek.WEDNESDAY, pendingDays(draft))
    }

    @Test fun hoursForAllDaysReachEveryWorkingDayButNotTheDaysOff() {
        val draft = draftOf(WorkJourney()).withPreset(JourneyPreset.SUNDAY_OFF).withHours(DayOfWeek.TUESDAY, LocalTime.of(7, 0) to LocalTime.of(11, 0)).withAllHours(nineToSix)
        DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }.forEach { assertEquals(DaySlot(nineToSix), draft.getValue(it)) }
        assertNull(draft.getValue(DayOfWeek.SUNDAY))
        assertEquals(emptySet<DayOfWeek>(), pendingDays(draft))
    }

    @Test fun changingOneDayKeepsTheHoursAlreadySetOnTheOthers() {
        val eightToNoon = LocalTime.of(8, 0) to LocalTime.of(12, 0)
        val draft = draftOf(WorkJourney.standard()).withHours(DayOfWeek.FRIDAY, eightToNoon)
        assertEquals(DaySlot(eightToNoon), draft.getValue(DayOfWeek.FRIDAY))
        assertEquals(DaySlot(LocalTime.of(8, 0) to LocalTime.of(17, 0)), draft.getValue(DayOfWeek.MONDAY))
    }

    @Test fun aDayTurnedOnTakesTheHoursOfTheOthers() {
        val draft = draftOf(WorkJourney.standard()).toggled(DayOfWeek.SATURDAY)
        assertEquals(draftOf(WorkJourney.standard()).getValue(DayOfWeek.MONDAY), draft.getValue(DayOfWeek.SATURDAY))
        assertNull(draft.toggled(DayOfWeek.SATURDAY)[DayOfWeek.SATURDAY])
    }

    @Test fun aDayTurnedBackOnTakesItsOwnPreviousHours() {
        val eightToNoon = LocalTime.of(8, 0) to LocalTime.of(12, 0)
        val draft = draftOf(WorkJourney.standard()).toggled(DayOfWeek.SATURDAY, eightToNoon)
        assertEquals(DaySlot(eightToNoon), draft.getValue(DayOfWeek.SATURDAY))
    }

    @Test fun thePresetKeepsTheHoursThePersonAlreadyChose() {
        val draft: JourneyDraft = DayOfWeek.entries.associateWith { if (it == DayOfWeek.TUESDAY) DaySlot(nineToSix) else null }
        val filled = draft.withPreset(JourneyPreset.EVERY_DAY)
        assertEquals(DaySlot(nineToSix), filled.getValue(DayOfWeek.SUNDAY))
        assertEquals(DaySlot(nineToSix), filled.getValue(DayOfWeek.MONDAY))
    }

    @Test fun theMatchingPresetIsRecognized() {
        assertEquals(JourneyPreset.WEEKDAYS, draftOf(WorkJourney.standard()).preset())
        assertEquals(JourneyPreset.EVERY_DAY, draftOf(WorkJourney()).withPreset(JourneyPreset.EVERY_DAY).preset())
        assertEquals(JourneyPreset.SUNDAY_OFF, draftOf(WorkJourney()).withPreset(JourneyPreset.SUNDAY_OFF).preset())
    }

    @Test fun aCustomJourneyMatchesNoPreset() {
        val custom = draftOf(WorkJourney.standard()) + (DayOfWeek.SUNDAY to DaySlot(nineToSix))
        assertNull(custom.preset())
        assertNull(draftOf(WorkJourney()).preset())
    }

    @Test fun theDaysSummaryNeverCarriesHours() {
        assertEquals(DaysSummary.Weekdays, WorkJourney.standard().daysSummary())
        assertEquals(DaysSummary.EveryDay, draftOf(WorkJourney()).withPreset(JourneyPreset.EVERY_DAY).daysSummary())
        assertEquals(
            DaysSummary.Some(listOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)),
            WorkJourney(mapOf(DayOfWeek.MONDAY to nineToSixShift, DayOfWeek.SATURDAY to nineToSixShift)).daysSummary(),
        )
        assertEquals(DaysSummary.None, WorkJourney().daysSummary())
    }

    @Test fun theRadiusCyclesThroughThePresetsAndBackToNone() {
        var current: Int? = null
        val seen = mutableListOf<Int?>()
        repeat(ServiceRadius.PRESETS.size + 1) {
            current = ServiceRadius.next(current)
            seen += current
        }
        assertEquals(ServiceRadius.PRESETS + listOf(null), seen)
    }

    @Test fun aRadiusOutsideThePresetsRestartsTheCycle() {
        assertEquals(ServiceRadius.PRESETS.first(), ServiceRadius.next(33))
    }
}
