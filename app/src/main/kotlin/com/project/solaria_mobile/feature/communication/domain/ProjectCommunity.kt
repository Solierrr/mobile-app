package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult
import kotlinx.serialization.Serializable

@Serializable
data class CommunityGroup(
    val id: String, val title: String, val participants: Int, val active: Boolean, val canOpen: Boolean,
    /** Quem gere o subgrupo e quem participa, por `authId`: base da escolha de sucessor ao remover um integrante. */
    val managerId: String? = null, val participantIds: List<String> = emptyList(),
)

/** [role] é `OWNER`, `ADMIN` ou `MEMBER` para quem já está na comunidade. */
@Serializable data class CommunityPerson(val authId: String, val name: String, val joined: Boolean, val self: Boolean = false, val role: String? = null)

@Serializable
data class ProjectCommunity(
    val id: String? = null,
    val projectId: String,
    val active: Boolean = true,
    val canCreate: Boolean = false,
    val members: Int = 0,
    val groups: List<CommunityGroup> = emptyList(),
    val canManage: Boolean = false,
    val people: List<CommunityPerson> = emptyList(),
    /** Papel da própria conta na comunidade: só o dono rebaixa e remove administradores. */
    val myRole: String? = null,
    /** A comunidade foi arquivada: todos leem, ninguém escreve, até um administrador reativá-la. */
    val archived: Boolean = false,
)

/** Uma comunidade da lista "minhas comunidades": o projeto a que pertence, o estado e o papel da conta. */
data class CommunitySummary(val id: String, val projectId: String, val projectName: String, val archived: Boolean, val members: Int, val myRole: String?)

data class RejectedParticipant(val userId: String?, val reasonCode: String?)
data class GroupCreated(val conversationId: String, val rejectedParticipants: Int, val rejected: List<RejectedParticipant> = emptyList())

interface ProjectCommunityRepository {
    suspend fun read(projectId: String): ReadResult<ProjectCommunity>
    suspend fun create(projectId: String): ApiResult<Unit>
    suspend fun createGroup(communityId: String, title: String, participants: Set<String> = emptySet()): ApiResult<GroupCreated>
    suspend fun addMember(communityId: String, authId: String): ApiResult<Unit>

    /** Arquivar ou reativar a comunidade. */
    suspend fun setArchived(communityId: String, archived: Boolean): ApiResult<Unit>

    /** Concede (`admin`) ou retira o papel de administrador de um integrante; só o dono rebaixa. */
    suspend fun setAdmin(communityId: String, authId: String, admin: Boolean): ApiResult<Unit>

    /** Remove um integrante (ou a própria conta). [successors] indica, por subgrupo que ele gere, quem assume. */
    suspend fun removeMember(communityId: String, authId: String, successors: Map<String, String> = emptyMap()): ApiResult<Unit>

    /** Comunidades de que a conta participa, de todos os projetos. */
    suspend fun mine(): ApiResult<List<CommunitySummary>>
}

/** Os subgrupos que [authId] gere e que ficam com gente depois que ele sai: cada um precisa de um sucessor na remoção. */
fun ProjectCommunity.groupsNeedingSuccessor(authId: String): List<CommunityGroup> =
    groups.filter { it.managerId.equals(authId, ignoreCase = true) && it.participantIds.any { id -> !id.equals(authId, ignoreCase = true) } }

/**
 * Quem a conta pode tirar da comunidade, pelas regras do servidor: o dono remove qualquer um que não seja dono, o administrador
 * remove integrantes comuns, e todo integrante que não é dono pode sair sozinho.
 */
fun ProjectCommunity.canRemove(person: CommunityPerson): Boolean = person.joined && person.role != "OWNER" && when {
    person.self -> true
    myRole == "OWNER" -> true
    myRole == "ADMIN" -> person.role == "MEMBER"
    else -> false
}

/** Conceder o papel de administrador cabe ao dono e a administradores; retirar, só ao dono. */
fun ProjectCommunity.canChangeAdmin(person: CommunityPerson): Boolean = person.joined && !person.self && when (person.role) {
    "MEMBER" -> myRole == "OWNER" || myRole == "ADMIN"
    "ADMIN" -> myRole == "OWNER"
    else -> false
}
