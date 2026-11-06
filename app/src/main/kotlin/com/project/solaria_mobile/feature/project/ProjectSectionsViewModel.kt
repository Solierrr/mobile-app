package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.feature.project.domain.ProjectInviteLink
import com.project.solaria_mobile.feature.project.domain.InviteLinkValidity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import kotlinx.coroutines.Job
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import com.project.solaria_mobile.feature.project.domain.ProjectReport
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.project.domain.ProjectStatusOption
import com.project.solaria_mobile.feature.project.domain.ProjectInvitation
import com.project.solaria_mobile.feature.project.domain.ProjectRoleOption
import com.project.solaria_mobile.feature.project.domain.ActivityDraft

@HiltViewModel
class ProjectSectionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProjectDetailRepository,
) : ViewModel() {
    val projectId = savedStateHandle.routeId()

    private val mutableLocation = MutableStateFlow<UiState<ProjectLocationInfo>>(UiState.Loading)
    private val mutableMembers = MutableStateFlow<UiState<List<ProjectMember>>>(UiState.Loading)
    private val mutableActivities = MutableStateFlow<UiState<List<ProjectActivity>>>(UiState.Loading)
    private val mutableHistory = MutableStateFlow<UiState<List<ProjectHistoryEntry>>>(UiState.Loading)
    private val mutablePhotos = MutableStateFlow<UiState<List<ProjectPhoto>>>(UiState.Loading)

    val location: StateFlow<UiState<ProjectLocationInfo>> = mutableLocation
    val members: StateFlow<UiState<List<ProjectMember>>> = mutableMembers
    val activities: StateFlow<UiState<List<ProjectActivity>>> = mutableActivities
    val history: StateFlow<UiState<List<ProjectHistoryEntry>>> = mutableHistory
    val photos: StateFlow<UiState<List<ProjectPhoto>>> = mutablePhotos

    private val loaded = mutableSetOf<ProjectSheet>()
    private val jobs = mutableMapOf<ProjectSheet, Job>()

    init { loadPhotos() }

    fun open(sheet: ProjectSheet) {
        if (sheet in loaded) return
        load(sheet)
    }

    fun retry(sheet: ProjectSheet) = load(sheet)

    suspend fun photoContent(photoId: String): ByteArray? =
        (repository.photoContent(projectId, photoId) as? com.project.solaria_mobile.core.common.ApiResult.Success)?.data

    private fun loadPhotos() {
        loaded += ProjectSheet.GALLERY
        jobs[ProjectSheet.GALLERY] = viewModelScope.launch { mutablePhotos.collectRead(repository.observePhotos(projectId), isEmpty = { it.isEmpty() }) }
    }

    private fun load(sheet: ProjectSheet) {
        loaded += sheet
        jobs.remove(sheet)?.cancel()
        jobs[sheet] = viewModelScope.launch {
            when (sheet) {
                ProjectSheet.LOCATION -> mutableLocation.collectRead(repository.observeLocation(projectId))
                ProjectSheet.GALLERY -> mutablePhotos.collectRead(repository.observePhotos(projectId), isEmpty = { it.isEmpty() })
                ProjectSheet.AGENDA -> mutableActivities.collectRead(repository.observeActivities(projectId), isEmpty = { it.isEmpty() })
                ProjectSheet.HISTORY -> mutableHistory.collectRead(repository.observeHistory(projectId), isEmpty = { it.isEmpty() })
                ProjectSheet.TEAM -> {
                    loadInvitations()
                    mutableMembers.collectRead(repository.observeMembers(projectId), isEmpty = { it.isEmpty() })
                }
                ProjectSheet.CONVERSATIONS, ProjectSheet.REPORT -> Unit
            }
        }
    }

    /** Convites pendentes do projeto; vazio para quem não gerencia o projeto (o servidor recusa a listagem). */
    private val mutableInvitations = MutableStateFlow<List<ProjectInvitation>>(emptyList())
    val invitations: StateFlow<List<ProjectInvitation>> = mutableInvitations

    private val mutableTeamAction = MutableStateFlow<ProjectAction?>(null)
    val teamAction: StateFlow<ProjectAction?> = mutableTeamAction

    /** Links de convite ativos; o endereço só aparece em [createdLink], logo depois da criação. */
    private val mutableInviteLinks = MutableStateFlow<List<ProjectInviteLink>>(emptyList())
    val inviteLinks: StateFlow<List<ProjectInviteLink>> = mutableInviteLinks

    private val mutableCreatedLink = MutableStateFlow<ProjectInviteLink?>(null)
    val createdLink: StateFlow<ProjectInviteLink?> = mutableCreatedLink

    private fun loadInvitations() {
        viewModelScope.launch {
            mutableInvitations.value = (repository.invitations(projectId) as? ApiResult.Success)?.data.orEmpty()
            mutableInviteLinks.value = (repository.inviteLinks(projectId) as? ApiResult.Success)?.data.orEmpty()
        }
    }

    fun createInviteLink(role: ProjectRoleOption, validity: InviteLinkValidity) {
        if (mutableTeamAction.value == ProjectAction.Working) return
        mutableTeamAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutableTeamAction.value = when (val result = repository.createInviteLink(projectId, role, validity)) {
                is ApiResult.Success -> {
                    mutableCreatedLink.value = result.data
                    loadInvitations()
                    ProjectAction.Done(ProjectOperation.CREATE_LINK)
                }
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, ProjectOperation.CREATE_LINK)
            }
        }
    }

    fun revokeInviteLink(id: String) = teamOperation(ProjectOperation.REVOKE_LINK) {
        repository.revokeInviteLink(projectId, id).also { result ->
            if (result is ApiResult.Success && mutableCreatedLink.value?.id == id) mutableCreatedLink.value = null
        }
    }

    fun clearCreatedLink() {
        mutableCreatedLink.value = null
    }

    fun invite(email: String, role: ProjectRoleOption, functionName: String) =
        teamOperation(ProjectOperation.INVITE) { repository.invite(projectId, email, role, functionName) }

    fun revokeInvitation(id: String) = teamOperation(ProjectOperation.REVOKE_INVITE) { repository.revokeInvitation(id) }

    fun resendInvitation(id: String) = teamOperation(ProjectOperation.RESEND_INVITE) { repository.resendInvitation(id) }

    fun removeMember(userId: String) = teamOperation(ProjectOperation.REMOVE_MEMBER) { repository.removeMember(projectId, userId) }

    /** Recusa no aparelho um e-mail que o servidor recusaria, sem chamar a rede. */
    fun rejectInvite() {
        mutableTeamAction.value = ProjectAction.Failed(ErrorKind.VALIDATION, ProjectOperation.INVITE)
    }

    fun clearTeamAction() {
        if (mutableTeamAction.value != ProjectAction.Working) mutableTeamAction.value = null
    }

    private fun teamOperation(operation: ProjectOperation, call: suspend () -> ApiResult<*>) {
        if (mutableTeamAction.value == ProjectAction.Working) return
        mutableTeamAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutableTeamAction.value = when (val result = call()) {
                is ApiResult.Success -> {
                    retry(ProjectSheet.TEAM)
                    ProjectAction.Done(operation)
                }
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, operation)
            }
        }
    }

    private val mutableReportAction = MutableStateFlow<ProjectAction?>(null)
    val reportAction: StateFlow<ProjectAction?> = mutableReportAction

    suspend fun report(reportId: String): ApiResult<ProjectReport> = repository.report(projectId, reportId)

    /** Envia o relatório; o histórico e as imagens do local são recarregados depois da confirmação do servidor. */
    fun createReport(title: String, content: String, images: List<Pair<ByteArray, String>>) {
        if (mutableReportAction.value == ProjectAction.Working) return
        mutableReportAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutableReportAction.value = when (val result = repository.createReport(projectId, title, content, images)) {
                is ApiResult.Success -> {
                    retry(ProjectSheet.HISTORY)
                    retry(ProjectSheet.GALLERY)
                    ProjectAction.Done(ProjectOperation.CREATE_REPORT)
                }
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, ProjectOperation.CREATE_REPORT)
            }
        }
    }

    fun clearReportAction() {
        if (mutableReportAction.value != ProjectAction.Working) mutableReportAction.value = null
    }

    private val mutablePhotoAction = MutableStateFlow<ProjectAction?>(null)
    val photoAction: StateFlow<ProjectAction?> = mutablePhotoAction

    /** Envia uma imagem já validada no aparelho; a galeria e a capa são recarregadas do servidor depois. */
    fun uploadPhoto(image: ByteArray, mime: String) = photoOperation(ProjectOperation.UPLOAD_PHOTO) { repository.uploadPhoto(projectId, image, mime) }

    fun removePhoto(photoId: String) = photoOperation(ProjectOperation.REMOVE_PHOTO) { repository.removePhoto(projectId, photoId) }

    fun setCover(photoId: String) = photoOperation(ProjectOperation.SET_COVER) { repository.setCover(projectId, photoId) }

    /** Recusa no aparelho, antes de enviar, o que o servidor recusaria (formato ou tamanho). */
    fun rejectPhoto() {
        mutablePhotoAction.value = ProjectAction.Failed(ErrorKind.VALIDATION, ProjectOperation.UPLOAD_PHOTO)
    }

    fun clearPhotoAction() {
        if (mutablePhotoAction.value != ProjectAction.Working) mutablePhotoAction.value = null
    }

    private val mutableActivityAction = MutableStateFlow<ProjectAction?>(null)
    val activityAction: StateFlow<ProjectAction?> = mutableActivityAction

    /** Muda a situação da atividade no servidor; a agenda é recarregada só depois da confirmação. */
    fun changeActivityStatus(activityId: String, status: ProjectStatusOption) {
        if (mutableActivityAction.value == ProjectAction.Working) return
        mutableActivityAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutableActivityAction.value = when (val result = repository.changeActivityStatus(projectId, activityId, status)) {
                is ApiResult.Success -> {
                    retry(ProjectSheet.AGENDA)
                    null
                }
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, ProjectOperation.ACTIVITY_STATUS, status)
            }
        }
    }

    /** Cria (sem id) ou edita uma atividade; o rascunho é validado no aparelho antes de chamar o servidor. */
    fun saveActivity(activityId: String?, draft: ActivityDraft) {
        if (mutableActivityAction.value == ProjectAction.Working) return
        if (draft.validate().isNotEmpty()) {
            mutableActivityAction.value = ProjectAction.Failed(ErrorKind.VALIDATION, ProjectOperation.SAVE_ACTIVITY)
            return
        }
        mutableActivityAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutableActivityAction.value = when (val result = repository.saveActivity(projectId, activityId, draft)) {
                is ApiResult.Success -> {
                    retry(ProjectSheet.AGENDA)
                    ProjectAction.Done(ProjectOperation.SAVE_ACTIVITY)
                }
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, ProjectOperation.SAVE_ACTIVITY)
            }
        }
    }

    fun clearActivityAction() {
        if (mutableActivityAction.value != ProjectAction.Working) mutableActivityAction.value = null
    }

    private fun photoOperation(operation: ProjectOperation, call: suspend () -> ApiResult<*>) {
        if (mutablePhotoAction.value == ProjectAction.Working) return
        mutablePhotoAction.value = ProjectAction.Working
        viewModelScope.launch {
            mutablePhotoAction.value = when (val result = call()) {
                is ApiResult.Success -> {
                    retry(ProjectSheet.GALLERY)
                    null
                }
                is ApiResult.Failure -> ProjectAction.Failed(result.kind, operation)
            }
        }
    }

}
