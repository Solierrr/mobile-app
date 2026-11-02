package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.toListConversation
import com.project.solaria_mobile.feature.communication.domain.Conversation
import java.time.OffsetDateTime

/**
 * Aplica à lista de conversas um evento do tempo real, sem recarregar. Eventos de outra natureza deixam a lista como está.
 *
 * - `CONVERSATION_UPDATED` troca a linha (prévia, não lidas, silenciada) ou cria uma nova. Conversa arquivada sai, porque a lista
 *   mostra só as ativas. Se a última interação mudou, a linha sobe para o lugar que o horário pede e as outras não se mexem;
 *   se não mudou (silenciar, ler), a linha fica onde estava.
 * - `CONVERSATION_REMOVED` tira a linha.
 *
 * @param me identificador da conta, para o "Você:" da prévia.
 */
internal fun List<Conversation>.applyLive(event: ChatEvent, me: String?): List<Conversation> = when (event) {
    is ChatEvent.ConversationUpdated -> applyUpdate(event, me)
    is ChatEvent.ConversationRemoved -> filterNot { it.id == event.id }
    else -> this
}

private fun List<Conversation>.applyUpdate(event: ChatEvent.ConversationUpdated, me: String?): List<Conversation> {
    val dto = event.conversation
    if (dto.archived == true) return filterNot { it.id == dto.id }
    val index = indexOfFirst { it.id == dto.id }
    // O evento não traz nomes: a conversa direta reaproveita os que a linha já tinha resolvido.
    val updated = dto.toListConversation(me, if (index >= 0) this[index].participantNames else emptyMap())
    // Mesma última interação: só o conteúdo da linha muda.
    if (index >= 0 && this[index].activityAt == updated.activityAt) return toMutableList().also { it[index] = updated }
    val rest = filterNot { it.id == updated.id }
    val at = updated.activityAt.toInstantOrNull()
    // Sem horário não há onde encaixar: fica onde estava, ou no topo se é nova.
    if (at == null) return rest.toMutableList().also { it.add(index.coerceAtLeast(0), updated) }
    // Antes da primeira linha igual ou mais antiga (sem horário conta como a mais antiga); as demais mantêm a ordem.
    val position = rest.indexOfFirst { (it.activityAt.toInstantOrNull() ?: java.time.Instant.MIN) <= at }
    return rest.toMutableList().also { it.add(if (position < 0) rest.size else position, updated) }
}

private fun String?.toInstantOrNull(): java.time.Instant? =
    this?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
