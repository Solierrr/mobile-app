package com.project.solaria_mobile.feature.project.data

import kotlinx.coroutines.flow.Flow
import com.project.solaria_mobile.core.common.ReadResult

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.project.domain.NewProjectDraft
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class RemoteProjectRepository @Inject constructor(
    private val api: ProjectApi,
    private val executor: ApiCallExecutor,
    private val reader: OfflineReader,
    private val network: com.project.solaria_mobile.core.network.NetworkMonitor,
) : ProjectRepository {
    override suspend fun resolveLocation(id: String): ApiResult<ProjectSummary> =
        executor.execute { api.resolveLocation(id) }.map { it.toSummary() }

    override suspend fun projects(): ApiResult<List<ProjectSummary>> =
        reader.read("projects", ListSerializer(ProjectCacheSerializer)) { api.projects() }.map { items -> items.map { it.toSummary() } }

    override suspend fun project(id: String): ApiResult<ProjectSummary> =
        reader.read("project:$id", ProjectCacheSerializer) { api.project(id) }.map { it.toSummary() }

    override suspend fun create(draft: NewProjectDraft): ApiResult<String> =
        if (!network.isOnline.first()) ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.CONNECTIVITY)
        else executor.execute { api.create(draft.toRequest()) }.map { it.id }
    override fun observeProjects(): Flow<ReadResult<List<ProjectSummary>>> =
        reader.observe("projects", ListSerializer(ProjectCacheSerializer)) { api.projects() }.map { read -> read.map { items -> items.map { it.toSummary() } } }

    override fun observeProject(id: String): Flow<ReadResult<ProjectSummary>> = flow {
        if (reader.cached("project:$id", ProjectCacheSerializer) == null) {
            val list = reader.cached("projects", ListSerializer(ProjectCacheSerializer))
            val project = (list?.result as? ApiResult.Success)?.data?.firstOrNull { it.id == id }
            if (project != null) emit(ReadResult(ApiResult.Success(project.toSummary()), list.syncedAt, cached = true, refreshing = network.isOnline.first()))
        }
        emitAll(reader.observe("project:$id", ProjectCacheSerializer) { api.project(id) }.map { read -> read.map { it.toSummary() } })
    }

}

private fun String.orNull(): String? = trim().takeIf { it.isNotEmpty() }

internal fun NewProjectDraft.toRequest(): ProjectRequestDto = ProjectRequestDto(
    name = name.trim(),
    description = description.orNull(),
    location = if (!hasLocation) null else ProjectLocationDto(
        state = state.orNull()?.uppercase(),
        city = city.orNull(),
        neighborhood = neighborhood.orNull(),
        zipCode = zipDigits.ifEmpty { null },
        street = street.orNull(),
        number = number.orNull(),
        complement = complement.orNull(),
    ),
)

internal fun ProjectDto.toSummary(): ProjectSummary = ProjectSummary(
    id = id,
    name = name,
    place = listOfNotNull(location?.city?.takeIf { it.isNotBlank() }, location?.state?.takeIf { it.isNotBlank() }).joinToString(", "),
    status = when (status) {
        "OPEN" -> "Aberto"
        "IN_PROGRESS" -> "Em andamento"
        "COMPLETED" -> "Concluído"
        "CANCELED" -> "Cancelado"
        else -> status.orEmpty()
    },
    people = ownerName?.takeIf { it.isNotBlank() }?.let { "Responsável: $it" }.orEmpty(),
    companyId = ownerCompanyId?.takeIf { it.isNotBlank() },
    companyName = if (ownerCompanyId.isNullOrBlank()) "" else ownerName.orEmpty().trim(),
    companyLogoUrl = ownerCompanyLogoUrl?.takeIf { it.isNotBlank() },
    latitude = location?.latitude,
    longitude = location?.longitude,
    geocodedAt = location?.geocodedAt,
    address = listOfNotNull(location?.street, location?.number, location?.neighborhood, location?.city, location?.state, location?.zipCode)
        .filter { it.isNotBlank() }.joinToString(", "),
    createdAtMillis = createdAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0,
)
