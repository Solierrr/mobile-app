package com.project.solaria_mobile.feature.project.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.common.onSuccess
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.offline.ImageDiskCache
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteProjectDetailRepository @Inject constructor(
    private val api: ProjectApi,
    private val executor: ApiCallExecutor,
    private val reader: OfflineReader,
    private val images: ImageDiskCache,
) : ProjectDetailRepository {
    override suspend fun location(projectId: String): ApiResult<ProjectLocationInfo> =
        reader.read("project:$projectId", ProjectDto.serializer()) { api.project(projectId) }.map { it.toLocationInfo() }

    override suspend fun members(projectId: String): ApiResult<List<ProjectMember>> =
        reader.read("members:$projectId", ListSerializer(ProjectMemberDto.serializer())) { api.members(projectId) }.map { items -> items.map { it.toMember() } }

    override suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>> =
        reader.read("activities:$projectId", ListSerializer(ProjectActivityDto.serializer())) { api.activities(projectId) }.map { items ->
            items.sortedBy { it.startsAt }.map { it.toActivity() }
        }

    override suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>> =
        reader.read("history:$projectId", ListSerializer(ProjectHistoryDto.serializer())) { api.history(projectId) }.map { items -> items.map { it.toEntry() } }

    override suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>> =
        reader.read("photos:$projectId", ListSerializer(ProjectPhotoDto.serializer())) { api.photos(projectId) }.map { items ->
            items.sortedByDescending { it.cover }.map { ProjectPhoto(it.id, it.cover) }
        }

    override suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray> {
        val key = "photo:$projectId:$photoId"
        val saved = withContext(Dispatchers.IO) { images.get(key) }
        if (saved != null) return ApiResult.Success(saved)
        return executor.execute { withContext(Dispatchers.IO) { api.photoContent(projectId, photoId).use { it.bytes() } } }
            .onSuccess { bytes -> images.put(key, bytes) }
    }
}

private val dateTime = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("pt-BR"))
private val dateOnly = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pt-BR"))
private val timeOnly = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))

internal fun formatInstant(value: String?, pattern: DateTimeFormatter, zone: ZoneId = ZoneId.systemDefault()): String {
    val parsed = value?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() } ?: return ""
    return pattern.format(parsed.atZoneSameInstant(zone)).replace(".", "")
}

internal fun ProjectDto.toLocationInfo(): ProjectLocationInfo {
    val street = listOfNotNull(
        location?.street?.takeIf { it.isNotBlank() },
        location?.number?.takeIf { it.isNotBlank() },
    ).joinToString(", ")
    val area = listOfNotNull(
        location?.neighborhood?.takeIf { it.isNotBlank() },
        location?.city?.takeIf { it.isNotBlank() },
        location?.state?.takeIf { it.isNotBlank() },
    ).joinToString(", ")
    val lines = listOfNotNull(
        street.ifBlank { null },
        location?.complement?.takeIf { it.isNotBlank() },
        area.ifBlank { null },
        location?.zipCode?.takeIf { it.isNotBlank() }?.let { "CEP $it" },
    )
    return ProjectLocationInfo(description = description.orEmpty().trim(), addressLines = lines)
}

internal fun ProjectMemberDto.toMember(): ProjectMember {
    val displayName = name?.takeIf { it.isNotBlank() } ?: "Participante"
    val roleLabel = functionName?.takeIf { it.isNotBlank() } ?: when (role) {
        "OWNER" -> "Responsável pelo projeto"
        "MANAGER" -> "Gerente"
        "MEMBER" -> "Membro"
        "VIEWER" -> "Observador"
        else -> role.orEmpty()
    }
    return ProjectMember(id = id, userId = userId, name = displayName, role = roleLabel, initials = initialsOf(displayName))
}

internal fun ProjectActivityDto.toActivity(): ProjectActivity {
    val kind = when (type) {
        "SERVICE" -> "Serviço"
        "VISIT" -> "Visita"
        "MEETING" -> "Reunião"
        else -> "Atividade"
    }
    val state = when (status) {
        "OPEN" -> "Aberto"
        "IN_PROGRESS" -> "Em andamento"
        "COMPLETED" -> "Concluído"
        "CANCELED" -> "Cancelado"
        else -> ""
    }
    val subtitle = listOfNotNull(
        formatInstant(startsAt, dateOnly).ifBlank { null },
        kind,
        if (onsite) "No local" else null,
        state.ifBlank { null },
    ).joinToString(" · ")
    return ProjectActivity(
        id = id,
        title = title?.takeIf { it.isNotBlank() } ?: kind,
        subtitle = subtitle,
        time = formatInstant(startsAt, timeOnly),
    )
}

private val statusWords = Regex("""\b(OPEN|IN_PROGRESS|COMPLETED|CANCELED)\b""")

internal fun translateStatuses(text: String): String = statusWords.replace(text) {
    when (it.value) {
        "OPEN" -> "Aberto"
        "IN_PROGRESS" -> "Em andamento"
        "COMPLETED" -> "Concluído"
        else -> "Cancelado"
    }
}

internal fun ProjectHistoryDto.toEntry(): ProjectHistoryEntry = ProjectHistoryEntry(
    id = id,
    title = translateStatuses(detail?.takeIf { it.isNotBlank() } ?: action.orEmpty()),
    detail = listOfNotNull(actor?.takeIf { it.isNotBlank() }, formatInstant(createdAt, dateTime).ifBlank { null }).joinToString(" · "),
)
