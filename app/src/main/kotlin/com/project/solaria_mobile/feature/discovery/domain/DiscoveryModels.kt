package com.project.solaria_mobile.feature.discovery.domain
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleEvent(
    val id: String,
    val projectId: String,
    val projectName: String,
    val title: String,
    val startsAt: String,
    val endsAt: String? = null,
    val status: String? = null,
    val type: String? = null,
) {
    fun start(zone: ZoneId) = OffsetDateTime.parse(startsAt).atZoneSameInstant(zone)
    fun end(zone: ZoneId) = endsAt?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(zone) }.getOrNull() }

    fun occursOn(date: LocalDate, zone: ZoneId): Boolean {
        val start = start(zone)
        val end = end(zone)?.takeIf { it.isAfter(start) }
        return if (end == null) start.toLocalDate() == date
        else start.toInstant().isBefore(date.plusDays(1).atStartOfDay(zone).toInstant()) && end.toInstant().isAfter(date.atStartOfDay(zone).toInstant())
    }
}

data class CalendarDay(val number: Int, val inCurrentMonth: Boolean, val hasEvents: Boolean = false)
