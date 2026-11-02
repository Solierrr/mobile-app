package com.project.solaria_mobile.feature.discovery.domain

import com.project.solaria_mobile.core.common.ApiResult
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/** Horário de trabalho de um dia; fim sempre depois do início. */
data class WorkShift(val start: LocalTime, val end: LocalTime) {
    init { require(end.isAfter(start)) { "O fim da jornada deve ser depois do início." } }

    fun contains(time: LocalTime): Boolean = !time.isBefore(start) && time.isBefore(end)
}

/** Jornada semanal do profissional autônomo: um intervalo por dia, dias ausentes são folga. */
data class WorkJourney(val days: Map<DayOfWeek, WorkShift> = emptyMap()) {
    fun on(day: DayOfWeek): WorkShift? = days[day]

    companion object {
        val Weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

        /** Sugestão inicial quando o profissional ainda não definiu nada. */
        fun standard() = WorkJourney(Weekdays.associateWith { WorkShift(LocalTime.of(8, 0), LocalTime.of(17, 0)) })
    }
}

/** Faixa em que a agenda do Google do profissional está ocupada; só a ocupação vem do Google, nunca o título. */
data class BusySlot(val start: Instant, val end: Instant) {
    fun overlaps(from: Instant, to: Instant): Boolean = start.isBefore(to) && end.isAfter(from)
}

interface WorkJourneyRepository {
    suspend fun load(technicianId: String): ApiResult<WorkJourney>
    suspend fun save(technicianId: String, journey: WorkJourney): ApiResult<WorkJourney>
}

/** Integração com o Google Agenda do profissional. O app só autoriza e lê a ocupação; não escreve na agenda dele. */
interface CalendarLinkRepository {
    /** `true` quando há conexão ativa; falha de "não conectado" vira `false`. */
    suspend fun isConnected(technicianId: String): ApiResult<Boolean>
    suspend fun authorizationUrl(technicianId: String): ApiResult<String>
    suspend fun disconnect(technicianId: String): ApiResult<Unit>
    suspend fun busy(technicianId: String, from: OffsetDateTime, to: OffsetDateTime): ApiResult<List<BusySlot>>
}

internal fun BusySlot.occursOn(day: java.time.LocalDate, zone: ZoneId): Boolean =
    overlaps(day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant())
