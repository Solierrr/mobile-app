package com.project.solaria_mobile.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Linha da tabela `conversations`: uma conversa da lista do usuário, com a prévia da última mensagem.
 *
 * Alimentada por `GET /messaging/conversations/me` e pelos eventos `CONVERSATION_UPDATED` e `CONVERSATION_REMOVED`.
 * A atualização só vale se for mais nova que a guardada ([mergeConversation]). Conversas de chatbot ficam na tabela,
 * mas as mensagens delas não.
 *
 * @property id id da conversa. Chave primária.
 * @property syncEpoch época do relógio de sincronização em que [syncVersion] foi emitida. Texto vazio se desconhecida.
 * @property syncVersion versão da última atualização recebida. A maior vence dentro da mesma época.
 * @property conversationType `DIRECT`, `GROUP` ou `CHAT_BOT`.
 * @property participantIds ids dos participantes separados por vírgula. Sem participantes, texto vazio.
 * @property title título do grupo. Fica `null` na conversa direta.
 * @property archived `true` se o usuário arquivou a conversa. Ela continua guardada, mas sai da lista.
 * @property lastInteractionAt instante da última interação, em milissegundos, ou `null` se desconhecido.
 * @property lastMessageId id da última mensagem, ou `null` se a conversa não tem mensagens.
 * @property lastMessageSequence `sequence` da última mensagem. `0` sem mensagens. Mostra até onde o histórico deve ir.
 * @property lastMessageContent texto da prévia. `null` se a última mensagem foi excluída ou não existe.
 * @property lastMessageDeleted `true` se a última mensagem foi excluída.
 * @property accessLost `true` se o usuário perdeu o acesso à conversa. Ela fica em somente leitura e não sincroniza mais.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val syncEpoch: String,
    val syncVersion: Long,
    val conversationType: String,
    val participantIds: String,
    val title: String?,
    val archived: Boolean,
    val lastInteractionAt: Long?,
    val lastMessageId: String?,
    val lastMessageSequence: Int,
    val lastMessageContent: String?,
    val lastMessageDeleted: Boolean,
    val accessLost: Boolean,
)

/** Valor de [ConversationEntity.conversationType] das conversas com o chatbot. */
const val CHATBOT_TYPE = "CHAT_BOT"

/** `true` se a conversa é com o chatbot, cujas mensagens nunca são guardadas no banco. */
val ConversationEntity.isChatbot: Boolean get() = conversationType == CHATBOT_TYPE
