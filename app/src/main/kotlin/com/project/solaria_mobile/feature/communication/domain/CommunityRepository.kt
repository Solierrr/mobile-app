package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult

/**
 * Comunidades de projeto do api-messenger. Todas as operações exigem internet e não usam cache local: as conversas
 * das comunidades já chegam pela lista de conversas do [CommunicationRepository].
 */
interface CommunityRepository {
    suspend fun mine(): ApiResult<List<Community>>
    suspend fun byId(communityId: String): ApiResult<Community>

    /** Comunidade do projeto, ou sucesso com `null` se o projeto ainda não tem uma (404 do servidor). */
    suspend fun byProject(projectId: String): ApiResult<Community?>
    suspend fun subgroups(communityId: String): ApiResult<List<CommunitySubgroup>>

    /** Cria a comunidade do projeto. Só quem contratou o projeto pode. */
    suspend fun create(projectId: String, memberIds: List<String> = emptyList()): ApiResult<Community>
    suspend fun setStatus(communityId: String, status: CommunityStatus): ApiResult<Community>
    suspend fun addMembers(communityId: String, userIds: List<String>, role: CommunityRole = CommunityRole.MEMBER): ApiResult<Community>
    suspend fun removeMember(communityId: String, userId: String, successors: List<Successor> = emptyList()): ApiResult<Unit>

    /** O usuário logado sai da comunidade. */
    suspend fun leave(communityId: String): ApiResult<Unit>
    suspend fun createConversation(communityId: String, title: String, participantIds: List<String> = emptyList()): ApiResult<GroupCreation>
}
