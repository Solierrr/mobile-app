package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.discovery.domain.BusySlot
import com.project.solaria_mobile.feature.discovery.domain.CalendarLinkRepository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteCalendarLinkRepository @Inject constructor(
    private val api: RegistryApi,
    private val executor: ApiCallExecutor,
) : CalendarLinkRepository {
    private val enabled = BuildConfig.GOOGLE_REGISTRY_BASE_URL.isNotBlank()

    override suspend fun isConnected(technicianId: String): ApiResult<Boolean> {
        if (!enabled) return ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        val now = OffsetDateTime.now()
        return when (val result = busy(technicianId, now, now.plusHours(1))) {
            is ApiResult.Success -> ApiResult.Success(true)
            // 404: nunca conectou; 409: o Google revogou o acesso. Nos dois casos a pessoa precisa autorizar de novo.
            is ApiResult.Failure -> if (result.kind == ErrorKind.NOT_FOUND || result.kind == ErrorKind.CONFLICT) ApiResult.Success(false) else result
        }
    }

    override suspend fun authorizationUrl(technicianId: String): ApiResult<String> {
        if (!enabled) return ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        return when (val result = executor.execute { api.calendarConnect(technicianId) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(result.data.authorizationUrl)
        }
    }

    override suspend fun disconnect(technicianId: String): ApiResult<Unit> {
        if (!enabled) return ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        return when (val result = executor.execute { api.calendarDisconnect(technicianId) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    override suspend fun busy(technicianId: String, from: OffsetDateTime, to: OffsetDateTime): ApiResult<List<BusySlot>> {
        if (!enabled) return ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        val format = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return when (val result = executor.execute { api.calendarAvailability(technicianId, from.format(format), to.format(format)) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(result.data.busy.mapNotNull { slot ->
                runCatching { BusySlot(Instant.from(format.parse(slot.start)), Instant.from(format.parse(slot.end))) }.getOrNull()
            })
        }
    }
}
