package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.database.ConversationEntity
import com.project.solaria_mobile.core.database.MessageEntity
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.database.UserEntity
import com.project.solaria_mobile.core.format.formatTimestamp
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.MessageStatus
import java.time.Instant

/*
 * Conversões entre os três formatos de uma conversa ou mensagem: o DTO da rede, a linha do Room e o modelo da tela.
 * Ficam juntas porque a rede só escreve no Room e a tela só lê do Room, então toda conversão passa por [MessageEntity]
 * ou [ConversationEntity].
 */

private val tones = AvatarTone.entries

private const val DELETED_MESSAGE_TEXT = "Mensagem apagada"
private const val DEFAULT_CONVERSATION_NAME = "Conversa"
internal const val DIRECT_TYPE = "DIRECT"
internal const val GROUP_TYPE = "GROUP"

/**
 * Converte um instante ISO-8601 do servidor em milissegundos.
 *
 * @param value texto como `2026-10-28T10:00:00.123Z`.
 * @return os milissegundos, ou `null` se [value] é nulo ou não é um instante válido.
 */
internal fun parseMillis(value: String?): Long? =
    value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

/** Converte a mensagem da rede na linha do Room. Mensagem excluída chega sem texto e continua assim. */
internal fun MessageDto.toEntity(): MessageEntity =
    MessageEntity(
        id = id,
        conversationId = conversationId,
        sequence = sequence,
        senderId = senderId,
        clientMessageId = clientMessageId,
        content = if (deleted) null else content,
        edited = edited,
        deleted = deleted,
        timestamp = parseMillis(timestamp) ?: 0L,
        changeVersion = changeVersion,
    )

/** Converte o item da lista de conversas da rede na linha do Room, com o acesso presente. */
internal fun ConversationDto.toEntity(): ConversationEntity =
    ConversationEntity(
        id = id,
        syncEpoch = syncEpoch.orEmpty(),
        syncVersion = syncVersion,
        conversationType = conversationType.orEmpty(),
        participantIds = participantIds.joinToString(","),
        title = title?.takeIf { it.isNotBlank() },
        archived = archived == true,
        lastInteractionAt = parseMillis(lastInteractionAt),
        lastMessageId = lastMessage?.id,
        lastMessageSequence = lastMessage?.sequence ?: 0,
        lastMessageContent = lastMessage?.takeUnless { it.deleted }?.content,
        lastMessageDeleted = lastMessage?.deleted == true,
        accessLost = false,
    )

/** Tipo que o servidor exige nas mensagens enviadas para a conversa. */
internal fun ConversationEntity.messageType(): String = when (conversationType) {
    GROUP_TYPE -> "USER_TO_GROUP"
    "CHAT_BOT" -> "USER_TO_CHATBOT"
    else -> "USER_TO_USER"
}

/**
 * Ids dos participantes da conversa que não são o usuário logado.
 *
 * @param me `sub` do usuário logado, ou `null` sem sessão.
 */
internal fun ConversationEntity.peerIds(me: String?): List<String> =
    participantIds.split(',').filter { it.isNotBlank() && !it.equals(me, ignoreCase = true) }

/**
 * Converte a conversa do Room no modelo da tela.
 *
 * A conversa direta usa o nome do outro participante, quando já foi baixado. Sem ele, usa o título e, por último,
 * "Conversa".
 *
 * @param me `sub` do usuário logado.
 * @param names nomes guardados, pelo `authId` em minúsculas.
 */
internal fun ConversationEntity.toConversation(me: String?, names: Map<String, UserEntity>): Conversation {
    val peerName = if (conversationType == DIRECT_TYPE) {
        peerIds(me).firstNotNullOfOrNull { names[it.lowercase()]?.username }
    } else {
        null
    }
    val name = peerName ?: title ?: DEFAULT_CONVERSATION_NAME
    return Conversation(
        id = id,
        initials = initialsOf(name),
        name = name,
        preview = if (lastMessageDeleted) DELETED_MESSAGE_TEXT else lastMessageContent.orEmpty(),
        time = lastInteractionAt?.let { formatTimestamp(it) }.orEmpty(),
        tone = tones[(name.hashCode() and Int.MAX_VALUE) % tones.size],
        accessLost = accessLost,
    )
}

/**
 * Converte a mensagem do Room no modelo da tela.
 *
 * @param me `sub` do usuário logado. Decide se a bolha é "minha".
 * @param names nomes guardados, pelo `authId` em minúsculas.
 * @param showSender `true` nas conversas em grupo, onde a bolha dos outros mostra o nome de quem enviou.
 */
internal fun MessageEntity.toMessage(me: String?, names: Map<String, UserEntity>, showSender: Boolean): ChatMessage {
    val mine = me != null && senderId.equals(me, ignoreCase = true)
    return ChatMessage(
        id = id,
        text = if (deleted) DELETED_MESSAGE_TEXT else content.orEmpty(),
        mine = mine,
        time = if (timestamp > 0) formatTimestamp(timestamp) else "",
        sequence = sequence,
        clientMessageId = clientMessageId,
        sender = if (showSender && !mine) senderId?.let { names[it.lowercase()]?.username } else null,
        edited = edited && !deleted,
    )
}

/** Converte a mensagem que espera envio no modelo da tela, como pendente ou recusada. */
internal fun PendingMessageEntity.toMessage(): ChatMessage =
    ChatMessage(
        id = clientMessageId,
        text = content,
        mine = true,
        time = formatTimestamp(createdAt),
        clientMessageId = clientMessageId,
        status = if (failed) MessageStatus.FAILED else MessageStatus.PENDING,
    )

/**
 * Junta as mensagens confirmadas e as que esperam envio na ordem em que a tela mostra.
 *
 * As confirmadas vêm primeiro, por `sequence`. Uma pendente cujo `clientMessageId` já foi confirmado some, para a
 * bolha não aparecer duas vezes entre a confirmação e a saída da fila.
 *
 * @param delivered mensagens confirmadas pelo servidor.
 * @param pending mensagens da fila de envio, em ordem de gravação.
 */
internal fun timelineOf(delivered: Collection<ChatMessage>, pending: List<ChatMessage>): List<ChatMessage> {
    val sent = delivered.sortedBy { it.sequence ?: Int.MAX_VALUE }
    val confirmed = sent.mapNotNullTo(HashSet()) { it.clientMessageId }
    return sent + pending.filterNot { it.clientMessageId in confirmed }
}
