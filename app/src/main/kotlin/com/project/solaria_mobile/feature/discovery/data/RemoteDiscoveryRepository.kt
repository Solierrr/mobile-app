package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import com.project.solaria_mobile.feature.project.data.ProjectActivityDto
import com.project.solaria_mobile.feature.project.data.ProjectApi
import com.project.solaria_mobile.feature.project.data.ProjectDto
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class RemoteDiscoveryRepository @Inject constructor(private val api: ProjectApi, private val reader: OfflineReader) : DiscoveryRepository {
    override suspend fun events(): ApiResult<List<ScheduleEvent>> =
        reader.read("schedule:activities", ListSerializer(ScheduleEvent.serializer()), ::loadEvents)

    override fun observeEvents() = reader.observe("schedule:activities", ListSerializer(ScheduleEvent.serializer()), ::loadEvents)

    private suspend fun loadEvents(): List<ScheduleEvent> = coroutineScope {
        val concurrency = Semaphore(4)
        api.projects().distinctBy { it.id }.map { project ->
            async { concurrency.withPermit { api.activities(project.id).mapNotNull { it.toScheduleEvent(project) } } }
        }.awaitAll().flatten()
    }

    override suspend fun savedLocations(): ApiResult<List<String>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    override suspend fun isLocationSaved(): ApiResult<Boolean> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    override suspend fun toggleLocationSaved(): ApiResult<Boolean> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
}

internal fun ProjectActivityDto.toScheduleEvent(project: ProjectDto): ScheduleEvent? {
    val start = startsAt?.takeIf { runCatching { OffsetDateTime.parse(it) }.isSuccess } ?: return null
    return ScheduleEvent(id, project.id, project.name, title?.takeIf { it.isNotBlank() } ?: "Atividade do projeto", start, endsAt, status, type)
}
