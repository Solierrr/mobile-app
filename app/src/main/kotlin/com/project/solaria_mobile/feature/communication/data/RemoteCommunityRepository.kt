package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.currentUserId
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.common.onSuccess
import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.UserEntity
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.feature.communication.domain.Community
import com.project.solaria_mobile.feature.communication.domain.CommunityMember
import com.project.solaria_mobile.feature.communication.domain.CommunityRepository
import com.project.solaria_mobile.feature.communication.domain.CommunityRole
import com.project.solaria_mobile.feature.communication.domain.CommunityStatus
import com.project.solaria_mobile.feature.communication.domain.CommunitySubgroup
import com.project.solaria_mobile.feature.communication.domain.GroupCreation
import com.project.solaria_mobile.feature.communication.domain.Successor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

private const val DEFAULT_MEMBER_NAME = "Usuário"

internal fun roleOf(value: String): CommunityRole = CommunityRole.entries.firstOrNull { it.name == value } ?: CommunityRole.MEMBER

internal fun statusOf(value: String): CommunityStatus =
    CommunityStatus.entries.firstOrNull { it.name == value } ?: CommunityStatus.ACTIVE

/**
 * Converte a comunidade da rede no modelo do app.
 *
 * @param me `sub` do usuário logado em minúsculas. Define [Community.myRole].
 * @param names nomes guardados, pelo `authId` em minúsculas.
 */
internal fun CommunityDto.toCommunity(me: String?, names: Map<String, UserEntity>): Community {
    val people = members.map {
        val id = it.userId.lowercase()
        CommunityMember(id, names[id]?.username ?: DEFAULT_MEMBER_NAME, roleOf(it.role))
    }
    return Community(id, projectId.orEmpty(), statusOf(status), people, people.firstOrNull { it.authId == me }?.role)
}

internal fun SubgroupDto.toSubgroup(): CommunitySubgroup =
    CommunitySubgroup(id, title.orEmpty(), participantIds.size, managerUserId?.lowercase(), status != "DEACTIVATED")

/**
 * Comunidades de projeto, direto no api-messenger e sem cache local.
 *
 * Os nomes dos membros vêm do [UserDirectory]. Subgrupos criados por aqui entram na lista de conversas, e remover ou
 * sair pede uma sincronização completa para a lista refletir o acesso perdido mesmo sem o WebSocket.
 *
 * Depende de:
 * - [CommunityApi] -> rotas `/messaging/communities`.
 * - [ApiCallExecutor] -> executa as chamadas e devolve o [ApiResult].
 * - [CredentialProvider] -> `sub` do usuário logado.
 * - [UserDirectory] -> nomes dos membros.
 * - [ConversationDao] -> recebe o subgrupo recém-criado.
 * - [MessageSync] -> sincronização completa depois de remover ou sair.
 */
@Singleton
class RemoteCommunityRepository @Inject constructor(
    private val api: CommunityApi,
    private val executor: ApiCallExecutor,
    private val credentials: CredentialProvider,
    private val users: UserDirectory,
    private val conversations: ConversationDao,
    private val sync: MessageSync,
) : CommunityRepository {

    override suspend fun mine(): ApiResult<List<Community>> =
        executor.execute { api.mine() }.map { list -> list.map { community(it) } }

    override suspend fun byId(communityId: String): ApiResult<Community> =
        executor.execute { api.community(communityId) }.map { community(it) }

    override suspend fun byProject(projectId: String): ApiResult<Community?> =
        when (val result = executor.execute { api.byProject(projectId) }) {
            is ApiResult.Success -> ApiResult.Success(community(result.data))
            // O projeto ainda não tem comunidade: não é erro, a tela oferece criar
            is ApiResult.Failure -> if (result.kind == ErrorKind.NOT_FOUND) ApiResult.Success(null) else result
        }

    override suspend fun subgroups(communityId: String): ApiResult<List<CommunitySubgroup>> =
        executor.execute { api.subgroups(communityId) }.map { list -> list.map { it.toSubgroup() } }

    override suspend fun create(projectId: String, memberIds: List<String>): ApiResult<Community> =
        executor.execute { api.create(CommunityRequest(projectId, memberIds)) }.map { community(it) }

    override suspend fun setStatus(communityId: String, status: CommunityStatus): ApiResult<Community> =
        executor.execute { api.setStatus(communityId, CommunityStatusRequest(status.name)) }.map { community(it) }

    override suspend fun addMembers(communityId: String, userIds: List<String>, role: CommunityRole): ApiResult<Community> =
        executor.execute { api.addMembers(communityId, CommunityMembersRequest(userIds, role.name.takeIf { role != CommunityRole.MEMBER })) }
            .map { community(it) }

    override suspend fun removeMember(communityId: String, userId: String, successors: List<Successor>): ApiResult<Unit> =
        executor.execute {
            val body = MemberRemovalRequest(successors.map { SuccessorDto(it.conversationId, it.successorId) })
            val response = api.removeMember(communityId, userId, body)
            // Response<Unit> não lança em 4xx/5xx; o erro precisa passar pelo executor para ser classificado
            if (!response.isSuccessful) throw HttpException(response)
        }.onSuccess { sync.refresh() }

    override suspend fun leave(communityId: String): ApiResult<Unit> {
        val me = credentials.currentUserId() ?: return ApiResult.Failure(ErrorKind.UNAUTHORIZED)
        return removeMember(communityId, me)
    }

    override suspend fun createConversation(communityId: String, title: String, participantIds: List<String>): ApiResult<GroupCreation> =
        executor.execute { api.createConversation(communityId, CommunityConversationRequest(title, participantIds)) }.map { created ->
            // A resposta não traz os cabeçalhos de sincronização: só entra se a conversa ainda não está no banco, e a
            // sincronização completa a refaz com os dados reais
            if (conversations.find(created.conversation.id) == null) {
                conversations.upsert(listOf(created.conversation.toEntity()))
                sync.refresh()
            }
            GroupCreation(created.conversation.id, created.rejectedParticipants.map { it.userId })
        }

    private suspend fun community(dto: CommunityDto): Community {
        users.refreshIds(dto.members.map { it.userId })
        return dto.toCommunity(credentials.currentUserId(), users.observe().first())
    }
}
