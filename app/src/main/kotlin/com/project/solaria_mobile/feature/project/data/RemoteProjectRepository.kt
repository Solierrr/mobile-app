package com.project.solaria_mobile.feature.project.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
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
) : ProjectRepository {
    override suspend fun projects(): ApiResult<List<ProjectSummary>> =
        reader.read("projects", ListSerializer(ProjectDto.serializer())) { api.projects() }.map { items -> items.map { it.toSummary() } }

    override suspend fun project(id: String): ApiResult<ProjectSummary> =
        reader.read("project:$id", ProjectDto.serializer()) { api.project(id) }.map { it.toSummary() }

    override suspend fun create(draft: NewProjectDraft): ApiResult<String> =
        executor.execute { api.create(draft.toRequest()) }.map { it.id }
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
    createdAtMillis = createdAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0,
)
