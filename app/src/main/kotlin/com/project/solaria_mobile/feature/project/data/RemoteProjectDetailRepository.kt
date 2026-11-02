package com.project.solaria_mobile.feature.project.data

import com.project.solaria_mobile.feature.project.domain.ProjectInviteLink
import com.project.solaria_mobile.feature.project.domain.InviteLinkValidity
import kotlinx.coroutines.flow.Flow
import com.project.solaria_mobile.core.common.ReadResult

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import kotlinx.coroutines.flow.map
import com.project.solaria_mobile.core.common.onSuccess
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.offline.ImageDiskCache
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.LocationField
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import com.project.solaria_mobile.feature.project.domain.ProjectReport
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.project.solaria_mobile.feature.project.domain.ProjectStatusOption
import com.project.solaria_mobile.feature.project.domain.ProjectInvitation
import com.project.solaria_mobile.feature.project.domain.ProjectRoleOption
import com.project.solaria_mobile.feature.project.domain.ActivityDraft
import com.project.solaria_mobile.feature.project.domain.ActivityTypeOption

@Singleton
class RemoteProjectDetailRepository @Inject constructor(
    private val api: ProjectApi,
    private val executor: ApiCallExecutor,
    private val reader: OfflineReader,
    private val images: ImageDiskCache,
) : ProjectDetailRepository {
    override suspend fun location(projectId: String): ApiResult<ProjectLocationInfo> =
        reader.read("project:$projectId", ProjectCacheSerializer) { api.project(projectId) }.map { it.toLocationInfo() }

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

    override suspend fun uploadPhoto(projectId: String, image: ByteArray, mime: String): ApiResult<ProjectPhoto> {
        val extension = if (mime == "image/png") "png" else "jpg"
        val part = MultipartBody.Part.createFormData("file", "foto.$extension", image.toRequestBody(mime.toMediaType()))
        return executor.execute { api.uploadPhoto(projectId, part) }.map { ProjectPhoto(it.id, it.cover) }
    }

    override suspend fun report(projectId: String, reportId: String): ApiResult<ProjectReport> =
        executor.execute { api.report(projectId, reportId) }.map { it.toReport() }

    override suspend fun createReport(projectId: String, title: String, content: String, images: List<Pair<ByteArray, String>>): ApiResult<ProjectReport> {
        val text = "text/plain; charset=utf-8".toMediaType()
        val files = images.mapIndexed { index, (bytes, mime) ->
            MultipartBody.Part.createFormData("files", "imagem-${index + 1}.${if (mime == "image/png") "png" else "jpg"}", bytes.toRequestBody(mime.toMediaType()))
        }
        return executor.execute { api.createReport(projectId, title.trim().toRequestBody(text), content.trim().toRequestBody(text), files) }.map { it.toReport() }
    }

    override suspend fun invitations(projectId: String): ApiResult<List<ProjectInvitation>> =
        executor.execute { api.invitations(projectId) }.map { items -> items.map { it.toInvitation() }.filter { it.pending } }

    override suspend fun invite(projectId: String, email: String, role: ProjectRoleOption, functionName: String): ApiResult<Unit> =
        executor.execute { api.invite(projectId, ProjectInvitationRequestDto(email.trim(), role.api, functionName.trim().ifEmpty { null })) }.map { }

    override suspend fun revokeInvitation(invitationId: String): ApiResult<Unit> = executor.execute { api.revokeInvitation(invitationId) }.map { }

    override suspend fun resendInvitation(invitationId: String): ApiResult<Unit> = executor.execute { api.resendInvitation(invitationId) }.map { }

    override suspend fun inviteLinks(projectId: String): ApiResult<List<ProjectInviteLink>> =
        executor.execute { api.inviteLinks(projectId) }.map { items -> items.map { it.toInviteLink() }.filter { it.active } }

    override suspend fun createInviteLink(projectId: String, role: ProjectRoleOption, validity: InviteLinkValidity): ApiResult<ProjectInviteLink> =
        executor.execute { api.createInviteLink(projectId, ProjectInviteLinkRequestDto(role.api, validity.days)) }.map { it.toInviteLink() }

    override suspend fun revokeInviteLink(projectId: String, linkId: String): ApiResult<Unit> =
        executor.execute { api.revokeInviteLink(projectId, linkId) }.map { }

    override suspend fun removeMember(projectId: String, userId: String): ApiResult<Unit> = executor.execute { api.removeMember(projectId, userId) }

    override suspend fun saveActivity(projectId: String, activityId: String?, draft: ActivityDraft): ApiResult<ProjectActivity> {
        val body = draft.toRequest()
        return executor.execute {
            if (activityId == null) api.createActivity(projectId, body) else api.updateActivity(projectId, activityId, body)
        }.map { it.toActivity() }
    }

    override suspend fun changeActivityStatus(projectId: String, activityId: String, status: ProjectStatusOption): ApiResult<ProjectActivity> =
        executor.execute { api.changeActivityStatus(projectId, activityId, ProjectStatusRequest(status.api)) }.map { it.toActivity() }

    override suspend fun removePhoto(projectId: String, photoId: String): ApiResult<Unit> =
        executor.execute { api.removePhoto(projectId, photoId) }

    override suspend fun setCover(projectId: String, photoId: String): ApiResult<ProjectPhoto> =
        executor.execute { api.setCover(projectId, photoId) }.map { ProjectPhoto(it.id, it.cover) }

    override suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray> {
        val key = "photo:$projectId:$photoId"
        val request = images.request()
        val saved = withContext(Dispatchers.IO) { images.get(key, request)?.takeIf { com.project.solaria_mobile.core.images.isDecodableImage(it) } }
        if (saved != null) return ApiResult.Success(saved)
        return executor.execute { withContext(Dispatchers.IO) {
            api.photoContent(projectId, photoId).use { it.bytes() }.also {
                if (!com.project.solaria_mobile.core.images.isDecodableImage(it)) throw java.io.IOException("Imagem inválida")
            }
        } }
            .onSuccess { bytes -> withContext(Dispatchers.IO) { images.put(key, bytes, request) } }
    }
    override fun observeLocation(projectId: String): Flow<ReadResult<ProjectLocationInfo>> =
        reader.observe("project:$projectId", ProjectCacheSerializer) { api.project(projectId) }.map { read -> read.map { it.toLocationInfo() } }

    override fun observeMembers(projectId: String): Flow<ReadResult<List<ProjectMember>>> =
        reader.observe("members:$projectId", ListSerializer(ProjectMemberDto.serializer())) { api.members(projectId) }.map { read -> read.map { items -> items.map { it.toMember() } } }

    override fun observeActivities(projectId: String): Flow<ReadResult<List<ProjectActivity>>> =
        reader.observe("activities:$projectId", ListSerializer(ProjectActivityDto.serializer())) { api.activities(projectId) }.map { read -> read.map { items ->
            items.sortedBy { it.startsAt }.map { it.toActivity() }
        } }

    override fun observeHistory(projectId: String): Flow<ReadResult<List<ProjectHistoryEntry>>> =
        reader.observe("history:$projectId", ListSerializer(ProjectHistoryDto.serializer())) { api.history(projectId) }.map { read -> read.map { items -> items.map { it.toEntry() } } }

    override fun observePhotos(projectId: String): Flow<ReadResult<List<ProjectPhoto>>> =
        reader.observe("photos:$projectId", ListSerializer(ProjectPhotoDto.serializer())) { api.photos(projectId) }.map { read -> read.map { items ->
            items.sortedByDescending { it.cover }.map { ProjectPhoto(it.id, it.cover) }
        } }

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
    val fields = listOf(
        "Rua" to location?.street,
        "Número" to location?.number,
        "Complemento" to location?.complement,
        "Bairro" to location?.neighborhood,
        "Cidade" to location?.city,
        "Estado" to location?.state,
        "CEP" to location?.zipCode,
    ).mapNotNull { (label, value) -> value?.trim()?.takeIf { it.isNotBlank() }?.let { LocationField(label, it) } }
    return ProjectLocationInfo(description = description.orEmpty().trim(), addressLines = lines, fields = fields, latitude = location?.latitude, longitude = location?.longitude)
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
    return ProjectMember(id = id, userId = userId, name = displayName, role = roleLabel, initials = initialsOf(displayName), owner = role == "OWNER", viewer = role == "VIEWER")
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
        status = ProjectStatusOption.entries.firstOrNull { it.api == status },
        type = ActivityTypeOption.entries.firstOrNull { it.api == type },
        description = description.orEmpty(),
        startsAt = startsAt?.let { runCatching { java.time.OffsetDateTime.parse(it) }.getOrNull() },
        endsAt = endsAt?.let { runCatching { java.time.OffsetDateTime.parse(it) }.getOrNull() },
        onsite = onsite,
        assigneeIds = assigneeIds,
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
    title = translateStatuses(detail?.takeIf { it.isNotBlank() } ?: action.orEmpty()).let { if (reportId != null) "Relatório: $it" else it },
    detail = listOfNotNull(actor?.takeIf { it.isNotBlank() }, formatInstant(createdAt, dateTime).ifBlank { null }).joinToString(" · "),
    reportId = reportId,
)

internal fun ProjectReportDto.toReport(): ProjectReport = ProjectReport(
    id = id,
    title = title.orEmpty(),
    content = content.orEmpty(),
    author = author.orEmpty(),
    date = formatInstant(createdAt, dateTime),
    photoIds = photos.map { it.id },
)

internal fun ProjectInviteLinkDto.toInviteLink(): ProjectInviteLink = ProjectInviteLink(
    id = id,
    role = ProjectRoleOption.entries.firstOrNull { it.api == role }?.label ?: "Participante",
    active = status == null || status == "ACTIVE",
    expires = formatInstant(expiresAt, dateOnly).let { if (it.isBlank()) "" else "Válido até $it" },
    uses = useCount,
    url = url,
)

internal fun ProjectInvitationDto.toInvitation(): ProjectInvitation = ProjectInvitation(
    id = id,
    projectId = projectId.orEmpty(),
    projectName = projectName?.takeIf { it.isNotBlank() } ?: "Projeto",
    email = email.orEmpty(),
    role = functionName?.takeIf { it.isNotBlank() } ?: ProjectRoleOption.entries.firstOrNull { it.api == role }?.label ?: "Participante",
    pending = status == null || status == "PENDING",
    expires = formatInstant(expiresAt, dateOnly).let { if (it.isBlank()) "" else "Válido até $it" },
)

/** Data e horários escolhidos no aparelho, enviados com o fuso local. */
internal fun ActivityDraft.toRequest(): ProjectActivityRequestDto {
    val zone = java.time.ZoneId.systemDefault()
    return ProjectActivityRequestDto(
        title = title.trim(),
        description = description.trim().ifEmpty { null },
        type = type.api,
        startsAt = date.atTime(start).atZone(zone).toOffsetDateTime().toString(),
        endsAt = date.atTime(end).atZone(zone).toOffsetDateTime().toString(),
        onsite = onsite,
        assigneeIds = assigneeIds.distinct(),
    )
}
