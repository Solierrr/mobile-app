package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.readOnce
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow

enum class ServiceDecision { ACCEPT, COMPLETE, CANCEL, SCHEDULE }

interface ServiceCallRepository {
    suspend fun calls(): ApiResult<List<ServiceCall>>
    suspend fun call(id: String): ApiResult<ServiceCall>
    fun observeCalls(): Flow<ReadResult<List<ServiceCall>>> = readOnce { calls() }
    fun observeCall(id: String): Flow<ReadResult<ServiceCall>> = readOnce { call(id) }
    suspend fun change(id: String, decision: ServiceDecision, scheduledAt: OffsetDateTime? = null): ApiResult<ServiceCall>
}
