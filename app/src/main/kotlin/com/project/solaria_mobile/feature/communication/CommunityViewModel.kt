package com.project.solaria_mobile.feature.communication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.domain.Community
import com.project.solaria_mobile.feature.communication.domain.CommunityRepository
import com.project.solaria_mobile.feature.communication.domain.CommunityRole
import com.project.solaria_mobile.feature.communication.domain.CommunityStatus
import com.project.solaria_mobile.feature.communication.domain.CommunitySubgroup
import com.project.solaria_mobile.feature.communication.domain.Successor
import com.project.solaria_mobile.feature.communication.domain.canActivate
import com.project.solaria_mobile.feature.communication.domain.canAddMembers
import com.project.solaria_mobile.feature.communication.domain.canArchive
import com.project.solaria_mobile.feature.communication.domain.canCreateConversation
import com.project.solaria_mobile.feature.communication.domain.canGrantAdmin
import com.project.solaria_mobile.feature.communication.domain.canLeave
import com.project.solaria_mobile.feature.communication.domain.canRemoveMember
import com.project.solaria_mobile.presentation.navigation.routeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Aviso curto depois de uma ação de comunidade que o servidor recusou ou que falhou. A tela traduz em texto. */
enum class CommunityNotice {
    /** Sem internet: todas as ações de comunidade exigem conexão. */
    OFFLINE,

    /** O usuário não tem o papel exigido, ou não é a pessoa que contratou o projeto (403). */
    NOT_ALLOWED,

    /** O servidor recusou pelo estado da comunidade: arquivada, dono que não sai, subgrupo com menos de 2 pessoas (400/422). */
    REFUSED,

    /** Conflito (409): o projeto já tem comunidade, falta sucessor do subgrupo, ou alguém mudou ao mesmo tempo. */
    CONFLICT,

    /** Serviço fora do ar (503), por exemplo a checagem de quem contratou o projeto no api-core. */
    UNAVAILABLE,

    /** A comunidade, o projeto ou a pessoa não existe (404). */
    NOT_FOUND,

    /** Qualquer outra falha. */
    FAILED,
}

/**
 * Estado da tela da comunidade de um projeto.
 *
 * @property community comunidade do projeto, ou `null` se o projeto ainda não tem uma. A tela oferece criar.
 * @property subgroups subgrupos que o usuário enxerga: dono e administrador veem todos, membro só os seus.
 * @property notice aviso a mostrar, ou `null`.
 * @property lastCreation resultado da última criação de subgrupo, com as pessoas que o servidor não admitiu.
 */
data class CommunityUiState(
    val community: Community? = null,
    val subgroups: List<CommunitySubgroup> = emptyList(),
    val notice: CommunityNotice? = null,
    val lastCreation: GroupCreationResult? = null,
) {
    private val role: CommunityRole? get() = community?.myRole
    private val active: Boolean get() = community?.status == CommunityStatus.ACTIVE

    val mayArchive: Boolean get() = active && canArchive(role)
    val mayActivate: Boolean get() = community?.status == CommunityStatus.ARCHIVED && canActivate(role)
    val mayAddMembers: Boolean get() = active && canAddMembers(role)
    val mayGrantAdmin: Boolean get() = active && canGrantAdmin(role)
    val mayLeave: Boolean get() = canLeave(role)
    val mayCreateConversation: Boolean get() = community?.let { canCreateConversation(role, it.status) } ?: false

    /** `authId` dos membros que o usuário pode remover agora. */
    val removable: Set<String>
        get() = if (!active) emptySet() else community?.members.orEmpty().filter { canRemoveMember(role, it.role) }.map { it.authId }.toSet()
}

/**
 * Resultado da criação de um subgrupo.
 *
 * @property conversationId conversa criada, já na lista de conversas.
 * @property rejected pessoas que o servidor não admitiu.
 */
data class GroupCreationResult(val conversationId: String, val rejected: List<String>)

/**
 * ViewModel da comunidade de um projeto. O id da rota é o do projeto.
 *
 * Todas as ações exigem internet e, depois de cada sucesso, a tela é recarregada do servidor.
 *
 * Depende de:
 * - [CommunityRepository] -> as rotas de comunidade.
 *
 * Principais operações:
 * - [load] -> carrega a comunidade do projeto e os subgrupos.
 * - [create] -> cria a comunidade do projeto. Só quem contratou o projeto pode.
 * - [archive] | [activate] -> arquiva ou reativa a comunidade.
 * - [addMembers] -> adiciona membros, ou muda o papel de quem já é membro.
 * - [remove] | [leave] -> remove um membro ou o próprio usuário sai.
 * - [createConversation] -> cria um subgrupo.
 */
@HiltViewModel
class CommunityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CommunityRepository,
) : ViewModel() {
    private val projectId = savedStateHandle.routeId()
    private val mutableState = MutableStateFlow<UiState<CommunityUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<CommunityUiState>> = mutableState

    private val left = Channel<Unit>(Channel.BUFFERED)

    /** Emite uma vez quando o usuário saiu da comunidade, para a tela voltar. */
    val leftCommunity: Flow<Unit> = left.receiveAsFlow()

    init { load() }

    /**
     * Carrega a comunidade do projeto e os subgrupos.
     *
     * @param creation resultado de uma criação de subgrupo que acabou de acontecer, para entrar no estado novo.
     */
    fun load(creation: GroupCreationResult? = null) {
        viewModelScope.launch {
            mutableState.value = UiState.Loading
            mutableState.value = when (val found = repository.byProject(projectId)) {
                is ApiResult.Failure -> UiState.Error(found.kind)
                is ApiResult.Success -> {
                    val community = found.data
                    if (community == null) {
                        UiState.Content(CommunityUiState())
                    } else {
                        when (val groups = repository.subgroups(community.id)) {
                            is ApiResult.Failure -> UiState.Error(groups.kind)
                            is ApiResult.Success -> UiState.Content(CommunityUiState(community, groups.data, lastCreation = creation))
                        }
                    }
                }
            }
        }
    }

    /**
     * Cria a comunidade do projeto.
     *
     * @param memberIds `authId` das pessoas convidadas, além do próprio usuário, que vira dono.
     */
    fun create(memberIds: List<String> = emptyList()) = act { repository.create(projectId, memberIds).map() }

    fun archive() = withCommunity { repository.setStatus(it.id, CommunityStatus.ARCHIVED).map() }

    fun activate() = withCommunity { repository.setStatus(it.id, CommunityStatus.ACTIVE).map() }

    /**
     * Adiciona membros, ou muda o papel de quem já é membro.
     *
     * @param userIds `authId` das pessoas.
     * @param role papel. `ADMIN` só o dono concede, e `OWNER` o servidor recusa.
     */
    fun addMembers(userIds: List<String>, role: CommunityRole = CommunityRole.MEMBER) =
        withCommunity { repository.addMembers(it.id, userIds, role).map() }

    /**
     * Remove um membro. Quem gere subgrupos exige um sucessor por subgrupo, senão o servidor responde 409.
     *
     * @param userId `authId` do membro.
     * @param successors sucessores dos subgrupos que ele gere.
     */
    fun remove(userId: String, successors: List<Successor> = emptyList()) =
        withCommunity { repository.removeMember(it.id, userId, successors) }

    /** O usuário sai da comunidade. Em caso de sucesso emite [leftCommunity]. */
    fun leave() {
        val community = (mutableState.value as? UiState.Content)?.data?.community ?: return
        viewModelScope.launch {
            when (val result = repository.leave(community.id)) {
                is ApiResult.Success -> left.send(Unit)
                is ApiResult.Failure -> notify(result.kind)
            }
        }
    }

    /**
     * Cria um subgrupo.
     *
     * @param title nome do subgrupo.
     * @param participantIds `authId` dos participantes. Vazio significa todos os membros da comunidade.
     */
    fun createConversation(title: String, participantIds: List<String> = emptyList()) {
        val community = (mutableState.value as? UiState.Content)?.data?.community ?: return
        viewModelScope.launch {
            when (val result = repository.createConversation(community.id, title, participantIds)) {
                is ApiResult.Success -> load(GroupCreationResult(result.data.conversationId, result.data.rejected))
                is ApiResult.Failure -> notify(result.kind)
            }
        }
    }

    /** Fecha o aviso atual. */
    fun dismissNotice() {
        (mutableState.value as? UiState.Content)?.let { mutableState.value = UiState.Content(it.data.copy(notice = null)) }
    }

    private fun withCommunity(block: suspend (Community) -> ApiResult<Unit>) {
        val community = (mutableState.value as? UiState.Content)?.data?.community ?: return
        act { block(community) }
    }

    private fun act(block: suspend () -> ApiResult<Unit>) {
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> notify(result.kind)
            }
        }
    }

    private fun notify(kind: ErrorKind) {
        val current = mutableState.value as? UiState.Content ?: return
        mutableState.value = UiState.Content(current.data.copy(notice = noticeOf(kind)))
    }

    private fun noticeOf(kind: ErrorKind): CommunityNotice = when (kind) {
        ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT -> CommunityNotice.OFFLINE
        ErrorKind.FORBIDDEN -> CommunityNotice.NOT_ALLOWED
        ErrorKind.VALIDATION -> CommunityNotice.REFUSED
        ErrorKind.CONFLICT -> CommunityNotice.CONFLICT
        ErrorKind.SERVER -> CommunityNotice.UNAVAILABLE
        ErrorKind.NOT_FOUND -> CommunityNotice.NOT_FOUND
        else -> CommunityNotice.FAILED
    }
}

// Descarta o valor de um sucesso, para as ações que só recarregam a tela
private fun <T> ApiResult<T>.map(): ApiResult<Unit> = when (this) {
    is ApiResult.Success -> ApiResult.Success(Unit)
    is ApiResult.Failure -> this
}
