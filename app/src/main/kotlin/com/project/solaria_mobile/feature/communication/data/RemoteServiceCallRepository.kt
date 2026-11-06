package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.core.offline.CacheIdentity
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.domain.ServiceCallIcon
import com.project.solaria_mobile.feature.communication.domain.ServiceCallRepository
import com.project.solaria_mobile.feature.communication.domain.ServiceDecision
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class RemoteServiceCallRepository @Inject constructor(
    retrofit: RetrofitFactory,
    private val executor: ApiCallExecutor,
    private val reader: OfflineReader,
    private val network: NetworkMonitor,
    private val mock: MockModeStore,
    private val identity: CacheIdentity,
) : ServiceCallRepository {
    private val api = retrofit.create(BuildConfig.CORE_BASE_URL).create(ServiceCallApi::class.java)
    override suspend fun calls() = reader.read("services:list", ListSerializer(ServiceCallDto.serializer())) { api.list() }.map { it.map(ServiceCallDto::toCall) }
    override suspend fun call(id: String) = reader.read("services:$id", ServiceCallDto.serializer()) { api.detail(id) }.map(ServiceCallDto::toCall)
    override fun observeCalls() = reader.observe("services:list", ListSerializer(ServiceCallDto.serializer())) { api.list() }.map { read -> read.map { it.map(ServiceCallDto::toCall) } }
    override fun observeCall(id: String) = reader.observe("services:$id", ServiceCallDto.serializer()) { api.detail(id) }.map { it.map(ServiceCallDto::toCall) }

    override suspend fun change(id: String, decision: ServiceDecision, scheduledAt: OffsetDateTime?): ApiResult<ServiceCall> {
        val account = identity.current() ?: return ApiResult.Failure(ErrorKind.UNAUTHORIZED)
        val simulated = mock.isEnabled()
        val connected = simulated || network.isOnline.first()
        if (account != identity.current() || simulated != mock.isEnabled()) return ApiResult.Failure(ErrorKind.CONFLICT)
        if (!connected) return ApiResult.Failure(ErrorKind.CONNECTIVITY)
        if (decision == ServiceDecision.SCHEDULE && (scheduledAt == null || !scheduledAt.isAfter(OffsetDateTime.now())))
            return ApiResult.Failure(ErrorKind.VALIDATION)
        val result = executor.execute {
            when (decision) {
                ServiceDecision.ACCEPT -> api.accept(id)
                ServiceDecision.COMPLETE -> api.complete(id)
                ServiceDecision.CANCEL -> api.cancel(id)
                ServiceDecision.SCHEDULE -> api.schedule(id, ServiceScheduleRequest(requireNotNull(scheduledAt).toString()))
            }
        }
        if (account != identity.current() || simulated != mock.isEnabled()) return ApiResult.Failure(ErrorKind.CONFLICT)
        return when (result) {
            is ApiResult.Success -> executor.execute { reader.invalidatePrefix("services:"); result.data.toCall() }
            is ApiResult.Failure -> {
                if (result.kind in setOf(ErrorKind.UNAUTHORIZED, ErrorKind.FORBIDDEN, ErrorKind.NOT_FOUND)) executor.execute { reader.invalidatePrefix("services:") }
                result
            }
        }
    }
}

internal fun ServiceCallDto.toCall(): ServiceCall {
    fun date(value: String?) = value?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
    val scheduled = date(scheduledDate)
    val label = when (status) { "OPEN" -> "Aberto"; "IN_PROGRESS" -> "Em andamento"; "COMPLETED" -> "Concluído"; "CANCELED" -> "Cancelado"; else -> "Situação indisponível" }
    val address = location?.let { listOfNotNull(it.street, it.number, it.neighborhood, it.city, it.state).filter(String::isNotBlank).joinToString(", ") }.orEmpty()
    val detail = listOf(projectName, scheduled?.atZoneSameInstant(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")) ?: "Sem agendamento").joinToString(" · ")
    return ServiceCall(id, purpose.ifBlank { "Serviço técnico" }, detail, ServiceCallIcon.PHONE, label,
        projectId, projectName, status, scheduled, date(acceptedAt), date(endDate), address, canManage, hasExecutors, managedByAgenda)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceCallModule {
    @Binds abstract fun repository(implementation: RemoteServiceCallRepository): ServiceCallRepository
}
