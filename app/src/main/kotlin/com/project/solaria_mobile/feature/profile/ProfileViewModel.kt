package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.collectRead
import com.project.solaria_mobile.core.common.map
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.ProfileStats
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import com.project.solaria_mobile.presentation.navigation.routeIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.project.solaria_mobile.feature.profile.domain.ProfilePhotoType
import com.project.solaria_mobile.feature.profile.domain.USERNAME_PATTERN
import com.project.solaria_mobile.core.designsystem.components.userMessage

data class ProfileUiState(val self: Boolean = true, val profile: UserProfile, val connected: Boolean = false, val actionError: ErrorKind? = null, val connecting: Boolean = false, val stats: ProfileStats? = null)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val projects: ProjectRepository,
) : ViewModel() {
    private val profileId: String? = savedStateHandle.routeIdOrNull()
    private val mutableState = MutableStateFlow<UiState<ProfileUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProfileUiState>> = mutableState

    init { load() }

    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (profileId == null) {
                loadStats()
                mutableState.collectRead(repository.observeOwnProfile().map { read -> read.map { ProfileUiState(true, it, stats = stats) } })
                return@launch
            }
            mutableState.value = UiState.Loading
            val self = false
            val profile = repository.profile(profileId)
            if (profile is ApiResult.Failure) {
                mutableState.value = UiState.Error(profile.kind)
                return@launch
            }
            val data = (profile as? ApiResult.Success)?.data
            if (data == null) {
                mutableState.value = UiState.Error(ErrorKind.UNKNOWN)
                return@launch
            }
            mutableState.value = UiState.Content(ProfileUiState(self, data, data.connected))
        }
    }

    private var stats: ProfileStats? = null

    /** Projetos e conexões do perfil próprio, de endpoints que o app já usa; cada número some se a sua consulta falhar. */
    private fun loadStats() {
        viewModelScope.launch {
            val items = (projects.projects() as? ApiResult.Success)?.data
            val people = (repository.connections() as? ApiResult.Success)?.data
            stats = ProfileStats(
                projects = items?.size,
                activeProjects = items?.count { it.status in ACTIVE_PROJECT_STATUSES },
                connections = people?.size,
            )
            (mutableState.value as? UiState.Content)?.let { mutableState.value = it.copy(data = it.data.copy(stats = stats)) }
        }
    }

    private val mutableEdit = MutableStateFlow<ProfileEdit?>(null)
    val edit: StateFlow<ProfileEdit?> = mutableEdit

    fun uploadPhoto(type: ProfilePhotoType, image: ByteArray, mime: String) {
        val userId = ownId() ?: return
        editOperation(if (type == ProfilePhotoType.BANNER) ProfileOperation.BANNER else ProfileOperation.PHOTO) {
            repository.uploadOwnPhoto(userId, type, image, mime)
        }
    }

    fun removePhoto() {
        val userId = ownId() ?: return
        editOperation(ProfileOperation.REMOVE_PHOTO) { repository.removeOwnPhoto(userId, ProfilePhotoType.PROFILE) }
    }

    /** O nome de usuário é conferido no aparelho com a regra do servidor antes de enviar. */
    fun changeUsername(username: String) {
        if (!USERNAME_PATTERN.matches(username.trim())) {
            mutableEdit.value = ProfileEdit.Failed(ErrorKind.VALIDATION, ProfileOperation.USERNAME)
            return
        }
        editOperation(ProfileOperation.USERNAME) { repository.changeUsername(username) }
    }

    /** Imagem recusada no aparelho (formato ou tamanho), sem chamar o servidor. */
    fun rejectImage(operation: ProfileOperation) {
        mutableEdit.value = ProfileEdit.Failed(ErrorKind.VALIDATION, operation)
    }

    fun clearEdit() {
        if (mutableEdit.value != ProfileEdit.Working) mutableEdit.value = null
    }

    private fun ownId(): String? = (mutableState.value as? UiState.Content)?.data?.takeIf { it.self }?.profile?.id

    private fun editOperation(operation: ProfileOperation, call: suspend () -> ApiResult<Unit>) {
        if (mutableEdit.value == ProfileEdit.Working) return
        mutableEdit.value = ProfileEdit.Working
        viewModelScope.launch {
            mutableEdit.value = when (val result = call()) {
                is ApiResult.Success -> {
                    load()
                    ProfileEdit.Done(operation)
                }
                is ApiResult.Failure -> ProfileEdit.Failed(result.kind, operation)
            }
        }
    }

    fun toggleConnection() {
        val current = mutableState.value as? UiState.Content ?: return
        if (current.data.connecting) return
        mutableState.value = current.copy(data = current.data.copy(connecting = true, actionError = null))
        viewModelScope.launch {
            val result = repository.setConnection(current.data.profile.id, !current.data.connected)
            mutableState.value = when (result) {
                is ApiResult.Success -> UiState.Content(current.data.copy(connected = result.data, actionError = null, connecting = false))
                is ApiResult.Failure -> UiState.Content(current.data.copy(actionError = result.kind, connecting = false))
            }
        }
    }
}

private val ACTIVE_PROJECT_STATUSES = setOf("Aberto", "Em andamento")

enum class ProfileOperation { PHOTO, BANNER, REMOVE_PHOTO, USERNAME }

sealed interface ProfileEdit {
    data object Working : ProfileEdit
    data class Done(val operation: ProfileOperation) : ProfileEdit
    data class Failed(val kind: ErrorKind, val operation: ProfileOperation) : ProfileEdit
}

/** Texto da recusa de uma edição do perfil; a falha não traz a mensagem do servidor. */
internal fun ProfileEdit.Failed.message(resources: android.content.res.Resources): String = when (kind) {
    ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT -> resources.getString(R.string.pv_sem_conexao_o_perfil_nao)
    ErrorKind.VALIDATION, ErrorKind.CONFLICT -> when (operation) {
        ProfileOperation.USERNAME -> resources.getString(R.string.pv_use_de_3_a_30)
        ProfileOperation.REMOVE_PHOTO -> resources.getString(R.string.pv_nao_ha_foto_para_remover)
        else -> resources.getString(R.string.pr_use_uma_imagem_jpg_ou)
    }
    ErrorKind.FORBIDDEN -> resources.getString(R.string.pv_voce_so_pode_alterar_o)
    else -> kind.userMessage(resources)
}

@androidx.compose.runtime.Composable
internal fun ProfileEdit.Failed.message(): String = message(androidx.compose.ui.platform.LocalContext.current.resources)
