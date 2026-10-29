package com.project.solaria_mobile.feature.communication.domain

enum class CommunityRole { OWNER, ADMIN, MEMBER }

enum class CommunityStatus { ACTIVE, ARCHIVED }

/**
 * Participante de uma comunidade.
 *
 * @property authId `sub` da pessoa, em minúsculas.
 * @property name nome conhecido, ou "Usuário" enquanto o api-core não respondeu.
 * @property role papel na comunidade.
 */
data class CommunityMember(val authId: String, val name: String, val role: CommunityRole)

/**
 * Comunidade de um projeto, vista pelo usuário logado.
 *
 * @property myRole papel do usuário nela, ou `null` se ele não é membro.
 */
data class Community(
    val id: String,
    val projectId: String,
    val status: CommunityStatus,
    val members: List<CommunityMember>,
    val myRole: CommunityRole?,
)

/**
 * Subgrupo (conversa) de uma comunidade.
 *
 * @property active `false` se a conversa foi desativada.
 */
data class CommunitySubgroup(
    val id: String,
    val title: String,
    val participantCount: Int,
    val managerId: String?,
    val active: Boolean,
)

/**
 * Sucessor de um subgrupo, exigido ao remover quem o gere.
 *
 * @property conversationId subgrupo que perde o gestor.
 * @property successorId `authId` de quem passa a gerir.
 */
data class Successor(val conversationId: String, val successorId: String)

/**
 * Resultado da criação de um subgrupo.
 *
 * @property conversationId id da conversa criada. Já está na lista de conversas do app.
 * @property rejected pessoas que o servidor não admitiu, por não serem membros ou estarem inativas.
 */
data class GroupCreation(val conversationId: String, val rejected: List<String>)
