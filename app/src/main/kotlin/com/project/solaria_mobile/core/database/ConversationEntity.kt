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
 * @property lastReadSequence cursor de leitura que o servidor guarda para o usuário. `0` sem leitura ou no chatbot.
 * @property localReadSequence maior `sequence` que o usuário já viu neste aparelho. Passa de [lastReadSequence] enquanto a
 *   leitura espera para ser enviada, e a diferença entre os dois é a fila de leitura, sem tabela própria.
 * @property unreadCount quantas mensagens o servidor conta como não lidas. `0` no chatbot.
 * @property muted `true` se o usuário silenciou a conversa.
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
    val lastReadSequence: Int = 0,
    val localReadSequence: Int = 0,
    val unreadCount: Long = 0,
    val muted: Boolean = false,
)

/**
 * Quantas mensagens a lista deve mostrar como não lidas.
 *
 * Zero quando o usuário já viu a última mensagem neste aparelho, mesmo que o servidor ainda não saiba. Fora disso vale
 * a contagem do servidor.
 */
val ConversationEntity.displayedUnread: Long
    get() = if (localReadSequence >= lastMessageSequence) 0L else unreadCount

/** `true` se há leitura feita neste aparelho que o servidor ainda não recebeu. */
val ConversationEntity.hasPendingRead: Boolean
    get() = localReadSequence > lastReadSequence

/** Valor de [ConversationEntity.conversationType] das conversas com o chatbot. */
const val CHATBOT_TYPE = "CHAT_BOT"

/** `true` se a conversa é com o chatbot, cujas mensagens nunca são guardadas no banco. */
val ConversationEntity.isChatbot: Boolean get() = conversationType == CHATBOT_TYPE
