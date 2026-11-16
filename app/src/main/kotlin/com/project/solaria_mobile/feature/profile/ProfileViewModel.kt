package com.project.solaria_mobile.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.core.common.collectRead
import com.project.solaria_mobile.core.common.map
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import com.project.solaria_mobile.presentation.navigation.routeIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.project.solaria_mobile.feature.profile.domain.ProfilePhotoType
import com.project.solaria_mobile.feature.profile.domain.USERNAME_PATTERN
import com.project.solaria_mobile.core.designsystem.components.userMessage

data class ProfileUiState(val self: Boolean = true, val profile: UserProfile, val connected: Boolean = false, val actionError: ErrorKind? = null, val connecting: Boolean = false, val chatFailure: ErrorKind? = null)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val communication: CommunicationRepository,
) : ViewModel() {
    private val profileId: String? = savedStateHandle.routeIdOrNull()
    private val mutableState = MutableStateFlow<UiState<ProfileUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProfileUiState>> = mutableState

    private val chatToOpen = Channel<String>(Channel.BUFFERED)

    /** Id da conversa que a tela deve abrir, emitido uma vez quando [startChat] termina com sucesso. */
    val openChat: Flow<String> = chatToOpen.receiveAsFlow()

    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (profileId == null) {
                mutableState.collectRead(repository.observeOwnProfile().map { read -> read.map { ProfileUiState(true, it) } })
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

    /**
     * Abre a conversa direta com a pessoa do perfil, criando-a se ainda não existe. Exige internet.
     *
     * Em caso de sucesso emite o id em [openChat]. Em caso de falha guarda o motivo em [ProfileUiState.chatFailure].
     * Perfil sem `authId` (o do próprio usuário, ou dado incompleto do servidor) não abre conversa.
     */
    fun startChat() {
        val current = mutableState.value as? UiState.Content ?: return
        val authId = current.data.profile.authId ?: return
        viewModelScope.launch {
            when (val result = communication.startDirect(authId)) {
                is ApiResult.Success -> chatToOpen.send(result.data)
                is ApiResult.Failure -> mutableState.value = UiState.Content(current.data.copy(chatFailure = result.kind))
            }
        }
    }
}

enum class ProfileOperation { PHOTO, BANNER, REMOVE_PHOTO, USERNAME }

sealed interface ProfileEdit {
    data object Working : ProfileEdit
    data class Done(val operation: ProfileOperation) : ProfileEdit
    data class Failed(val kind: ErrorKind, val operation: ProfileOperation) : ProfileEdit
}

/** Texto da recusa de uma edição do perfil; a falha não traz a mensagem do servidor. */
internal fun ProfileEdit.Failed.message(): String = when (kind) {
    ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT -> "Sem conexão. O perfil não foi alterado."
    ErrorKind.VALIDATION, ErrorKind.CONFLICT -> when (operation) {
        ProfileOperation.USERNAME -> "Use de 3 a 30 caracteres: letras minúsculas, números ou sublinhado. O nome pode já estar em uso."
        ProfileOperation.REMOVE_PHOTO -> "Não há foto para remover."
        else -> "Use uma imagem JPG ou PNG de até 8 MB."
    }
    ErrorKind.FORBIDDEN -> "Você só pode alterar o seu próprio perfil."
    else -> kind.userMessage()
}
