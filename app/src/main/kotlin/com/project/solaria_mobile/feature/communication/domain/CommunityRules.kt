package com.project.solaria_mobile.feature.communication.domain

// Regras de papel das comunidades, iguais às do api-messenger (`PureRulesHelper`). Servem só para mostrar ou esconder
// ações: quem decide é o servidor, que responde 403 ou 422 quando o app erra, por exemplo com a lista desatualizada.

/** Só o dono arquiva a comunidade. */
fun canArchive(role: CommunityRole?): Boolean = role == CommunityRole.OWNER

/** Dono e administrador reativam uma comunidade arquivada. */
fun canActivate(role: CommunityRole?): Boolean = role == CommunityRole.OWNER || role == CommunityRole.ADMIN

/** Dono e administrador adicionam membros comuns. */
fun canAddMembers(role: CommunityRole?): Boolean = role == CommunityRole.OWNER || role == CommunityRole.ADMIN

/** Só o dono promove alguém a administrador. */
fun canGrantAdmin(role: CommunityRole?): Boolean = role == CommunityRole.OWNER

/**
 * Dono e administrador removem outro membro. O dono nunca é removido.
 *
 * @param myRole papel de quem remove.
 * @param targetRole papel de quem seria removido.
 */
fun canRemoveMember(myRole: CommunityRole?, targetRole: CommunityRole): Boolean =
    (myRole == CommunityRole.OWNER || myRole == CommunityRole.ADMIN) && targetRole != CommunityRole.OWNER

/** Qualquer membro sai da comunidade, menos o dono. */
fun canLeave(role: CommunityRole?): Boolean = role == CommunityRole.ADMIN || role == CommunityRole.MEMBER

/** Qualquer membro cria subgrupo, desde que a comunidade esteja ativa. */
fun canCreateConversation(role: CommunityRole?, status: CommunityStatus): Boolean =
    role != null && status == CommunityStatus.ACTIVE
