package com.project.solaria_mobile.feature.project.domain

import kotlinx.coroutines.flow.Flow
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.readOnce

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind

data class LocationField(val label: String, val value: String)

data class ProjectLocationInfo(val description: String, val addressLines: List<String>, val fields: List<LocationField> = emptyList(), val latitude: Double? = null, val longitude: Double? = null) {
    val copyText: String get() = addressLines.joinToString(", ")
}

data class ProjectMember(val id: String, val userId: String, val name: String, val role: String, val initials: String, val owner: Boolean = false, val viewer: Boolean = false)

data class ProjectActivity(
    val id: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val status: ProjectStatusOption? = null,
    val type: ActivityTypeOption? = null,
    val description: String = "",
    val startsAt: java.time.OffsetDateTime? = null,
    val endsAt: java.time.OffsetDateTime? = null,
    val onsite: Boolean = false,
    val assigneeIds: List<String> = emptyList(),
)

/** Entrada do histórico; as que vêm de um relatório trazem o [reportId] para abri-lo. */
data class ProjectHistoryEntry(val id: String, val title: String, val detail: String, val reportId: String? = null)

/** Relatório do projeto: título, conteúdo e imagens, que também são fotos do projeto (as "imagens do local"). */
data class ProjectReport(val id: String, val title: String, val content: String, val author: String, val date: String, val photoIds: List<String>)

data class ProjectPhoto(val id: String, val cover: Boolean)

interface ProjectDetailRepository {
    suspend fun location(projectId: String): ApiResult<ProjectLocationInfo>
    suspend fun members(projectId: String): ApiResult<List<ProjectMember>>
    suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>>
    suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>>
    suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>>
    suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray>
    suspend fun uploadPhoto(projectId: String, image: ByteArray, mime: String): ApiResult<ProjectPhoto> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun report(projectId: String, reportId: String): ApiResult<ProjectReport> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun createReport(projectId: String, title: String, content: String, images: List<Pair<ByteArray, String>>): ApiResult<ProjectReport> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun removePhoto(projectId: String, photoId: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun setCover(projectId: String, photoId: String): ApiResult<ProjectPhoto> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun invitations(projectId: String): ApiResult<List<ProjectInvitation>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun invite(projectId: String, email: String, role: ProjectRoleOption, functionName: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun revokeInvitation(invitationId: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun resendInvitation(invitationId: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun inviteLinks(projectId: String): ApiResult<List<ProjectInviteLink>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun createInviteLink(projectId: String, role: ProjectRoleOption, validity: InviteLinkValidity): ApiResult<ProjectInviteLink> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun revokeInviteLink(projectId: String, linkId: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun removeMember(projectId: String, userId: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun saveActivity(projectId: String, activityId: String?, draft: ActivityDraft): ApiResult<ProjectActivity> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun changeActivityStatus(projectId: String, activityId: String, status: ProjectStatusOption): ApiResult<ProjectActivity> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    fun observeLocation(projectId: String): Flow<ReadResult<ProjectLocationInfo>> =
        readOnce { location(projectId) }
    fun observeMembers(projectId: String): Flow<ReadResult<List<ProjectMember>>> =
        readOnce { members(projectId) }
    fun observeActivities(projectId: String): Flow<ReadResult<List<ProjectActivity>>> =
        readOnce { activities(projectId) }
    fun observeHistory(projectId: String): Flow<ReadResult<List<ProjectHistoryEntry>>> =
        readOnce { history(projectId) }
    fun observePhotos(projectId: String): Flow<ReadResult<List<ProjectPhoto>>> =
        readOnce { photos(projectId) }
}

/** Papéis que um convite pode dar; a propriedade do projeto não é transferida por convite. */
enum class ProjectRoleOption(val api: String, val label: String) {
    MANAGER("MANAGER", "Gerente"),
    MEMBER("MEMBER", "Membro"),
    VIEWER("VIEWER", "Observador"),
}

/** Link de convite do projeto; `url` só existe logo depois de criado, depois disso o servidor não o devolve mais. */
data class ProjectInviteLink(
    val id: String,
    val role: String,
    val active: Boolean,
    val expires: String,
    val uses: Int,
    val url: String? = null,
)

enum class InviteLinkValidity(val days: Int, val label: String) {
    DAY(1, "1 dia"),
    WEEK(7, "7 dias"),
    MONTH(30, "30 dias"),
}

/** Gerente só é convidado por e-mail: um link repassável não deve conceder gestão do projeto. */
val INVITE_LINK_ROLES = listOf(ProjectRoleOption.MEMBER, ProjectRoleOption.VIEWER)

data class ProjectInvitation(
    val id: String,
    val projectId: String,
    val projectName: String,
    val email: String,
    val role: String,
    val pending: Boolean,
    val expires: String,
)

enum class ActivityTypeOption(val api: String, val label: String) {
    SERVICE("SERVICE", "Serviço"),
    VISIT("VISIT", "Visita"),
    MEETING("MEETING", "Reunião"),
    OTHER("OTHER", "Outra"),
}

enum class ActivityField { TITLE, PERIOD }

/** Atividade em edição no aparelho; o horário é local e vira data com fuso só no envio. */
data class ActivityDraft(
    val title: String = "",
    val description: String = "",
    val type: ActivityTypeOption = ActivityTypeOption.VISIT,
    val date: java.time.LocalDate = java.time.LocalDate.now(),
    val start: java.time.LocalTime = java.time.LocalTime.of(9, 0),
    val end: java.time.LocalTime = java.time.LocalTime.of(10, 0),
    val onsite: Boolean = true,
    val assigneeIds: List<String> = emptyList(),
) {
    /** Mesmas regras do api-core: título de até 160 caracteres e término depois do início. */
    fun validate(): Set<ActivityField> = buildSet {
        if (title.isBlank() || title.trim().length > 160) add(ActivityField.TITLE)
        if (!end.isAfter(start)) add(ActivityField.PERIOD)
    }

    companion object {
        fun from(activity: ProjectActivity): ActivityDraft {
            val zone = java.time.ZoneId.systemDefault()
            val starts = activity.startsAt?.atZoneSameInstant(zone)
            val ends = activity.endsAt?.atZoneSameInstant(zone)
            return ActivityDraft(
                title = activity.title,
                description = activity.description,
                type = activity.type ?: ActivityTypeOption.OTHER,
                date = starts?.toLocalDate() ?: java.time.LocalDate.now(),
                start = starts?.toLocalTime()?.withSecond(0)?.withNano(0) ?: java.time.LocalTime.of(9, 0),
                end = ends?.toLocalTime()?.withSecond(0)?.withNano(0) ?: java.time.LocalTime.of(10, 0),
                onsite = activity.onsite,
                assigneeIds = activity.assigneeIds,
            )
        }
    }
}
