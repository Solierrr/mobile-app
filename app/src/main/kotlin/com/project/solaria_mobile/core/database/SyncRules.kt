package com.project.solaria_mobile.core.database

// Regras puras que decidem se uma atualização do servidor entra no banco. Valem para página de histórico, catch-up,
// evento ao vivo e confirmação de envio. Como todo caminho passa por elas, reaplicar o que já chegou não muda nada e
// a ordem de chegada não importa.

/**
 * Decide o que gravar para uma mensagem que chegou do servidor.
 *
 * Uma atualização com [MessageEntity.changeVersion] menor ou igual à guardada é ignorada. Isso impede que uma página
 * de histórico lida antes de uma edição sobrescreva a edição que chegou ao vivo. Uma mensagem excluída perde o texto.
 *
 * @param stored linha que já está no banco, ou `null` se a mensagem é nova.
 * @param incoming mensagem recebida.
 * @return a linha a gravar, ou `null` se [incoming] não é mais nova que [stored] e nada muda.
 */
fun mergeMessage(stored: MessageEntity?, incoming: MessageEntity): MessageEntity? {
    if (stored != null && incoming.changeVersion <= stored.changeVersion) return null
    return if (incoming.deleted) incoming.copy(content = null) else incoming
}

/**
 * Decide o que gravar para uma conversa que chegou do servidor.
 *
 * A atualização vale se a época é diferente da guardada (o relógio recomeçou e as versões não se comparam) ou se a
 * versão é maior. Como a linha recebida traz `accessLost = false`, uma conversa que voltou a aparecer na lista deixa de
 * estar sem acesso.
 *
 * @param stored linha que já está no banco, ou `null` se a conversa é nova.
 * @param incoming conversa recebida.
 * @return a linha a gravar, ou `null` se [incoming] não é mais nova que [stored] e nada muda.
 */
fun mergeConversation(stored: ConversationEntity?, incoming: ConversationEntity): ConversationEntity? {
    if (stored == null) return incoming
    val newer = incoming.syncEpoch != stored.syncEpoch || incoming.syncVersion > stored.syncVersion
    return if (newer) incoming else null
}
