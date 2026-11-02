package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.discovery.domain.WorkJourney
import com.project.solaria_mobile.feature.discovery.domain.WorkJourneyRepository
import com.project.solaria_mobile.feature.discovery.domain.WorkShift
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** O api-core guarda data e hora, mas a jornada é semanal: só o dia da semana e a hora valem; a data é sempre esta. */
internal val SHIFT_REFERENCE_DATE: LocalDate = LocalDate.of(2000, 1, 1)

@Singleton
class RemoteWorkJourneyRepository @Inject constructor(
    private val api: ShiftApi,
    private val executor: ApiCallExecutor,
) : WorkJourneyRepository {
    override suspend fun load(technicianId: String): ApiResult<WorkJourney> =
        when (val result = executor.execute { api.byTechnician(technicianId) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(result.data.toJourney())
        }

    override suspend fun save(technicianId: String, journey: WorkJourney): ApiResult<WorkJourney> {
        val current = when (val result = executor.execute { api.byTechnician(technicianId) }) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.data
        }
        val plan = planShiftChanges(current, journey)
        for (id in plan.delete) executor.execute { api.delete(id) }.let { if (it is ApiResult.Failure) return it }
        for ((id, day, shift) in plan.update) executor.execute { api.update(id, shift.toRequest(technicianId, day)) }.let { if (it is ApiResult.Failure) return it }
        for ((day, shift) in plan.create) executor.execute { api.create(shift.toRequest(technicianId, day)) }.let { if (it is ApiResult.Failure) return it }
        return load(technicianId)
    }
}

internal data class ShiftChanges(
    val delete: List<String>,
    val update: List<Triple<String, DayOfWeek, WorkShift>>,
    val create: List<Pair<DayOfWeek, WorkShift>>,
)

/** Diferença entre o que o servidor tem e a jornada pedida: remove dias de folga e duplicados, atualiza o que mudou, cria o que falta. */
internal fun planShiftChanges(current: List<ShiftDto>, wanted: WorkJourney): ShiftChanges {
    val byDay = current.groupBy { it.dayWeek.toDayOfWeekOrNull() }
    val delete = mutableListOf<String>()
    val update = mutableListOf<Triple<String, DayOfWeek, WorkShift>>()
    val create = mutableListOf<Pair<DayOfWeek, WorkShift>>()
    byDay[null]?.forEach { delete += it.id }
    DayOfWeek.entries.forEach { day ->
        val existing = byDay[day].orEmpty()
        val shift = wanted.on(day)
        if (shift == null) existing.forEach { delete += it.id }
        else if (existing.isEmpty()) create += day to shift
        else {
            existing.drop(1).forEach { delete += it.id }
            if (existing.first().toShift() != shift) update += Triple(existing.first().id, day, shift)
        }
    }
    return ShiftChanges(delete, update, create)
}

internal fun List<ShiftDto>.toJourney(): WorkJourney =
    WorkJourney(mapNotNull { dto -> dto.dayWeek.toDayOfWeekOrNull()?.let { day -> dto.toShift()?.let { day to it } } }.toMap())

private fun ShiftDto.toShift(): WorkShift? = runCatching {
    WorkShift(LocalDateTime.parse(startDate).toLocalTime(), LocalDateTime.parse(endDate).toLocalTime())
}.getOrNull()

private fun WorkShift.toRequest(technicianId: String, day: DayOfWeek) = ShiftRequestDto(
    technicianId, day.name, SHIFT_REFERENCE_DATE.atTime(start).toString(), SHIFT_REFERENCE_DATE.atTime(end).toString(),
)

private fun String.toDayOfWeekOrNull(): DayOfWeek? = runCatching { DayOfWeek.valueOf(uppercase()) }.getOrNull()
