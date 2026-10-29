package com.project.solaria_mobile.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Linha da tabela `messages`: uma mensagem confirmada pelo servidor, de qualquer conversa humana.
 *
 * É o cache de leitura do chat. A tela só lê esta tabela; quem escreve é a sincronização ([MessageDao.upsert]),
 * sempre pela regra de versão maior vence. Mensagens de chatbot não entram aqui.
 *
 * @property id id da mensagem no servidor. Chave primária.
 * @property conversationId conversa a que a mensagem pertence.
 * @property sequence posição da mensagem na conversa, sem repetição. O índice único com `conversationId` ordena o chat.
 * @property senderId `sub` do remetente, como veio do servidor.
 * @property clientMessageId id gerado no aparelho que enviou. Liga a linha à pendente da fila de envio.
 * @property content texto da mensagem. Fica `null` quando [deleted] é `true`, porque a exclusão apaga o texto do banco.
 * @property edited `true` se o remetente editou a mensagem.
 * @property deleted `true` se o remetente excluiu a mensagem (tombstone).
 * @property timestamp instante do envio, em milissegundos. `0` se o servidor não informou.
 * @property changeVersion versão da última mudança. A maior vence; `0` em mensagem anterior ao carimbo de versão.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId", "sequence"], unique = true)],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sequence: Int,
    val senderId: String?,
    val clientMessageId: String?,
    val content: String?,
    val edited: Boolean,
    val deleted: Boolean,
    val timestamp: Long,
    val changeVersion: Long,
)

/**
 * Resumo das mensagens guardadas de uma conversa, que a sincronização usa para achar o que falta.
 *
 * @property count quantas mensagens a conversa tem no banco.
 * @property minSequence menor `sequence` guardada, ou `null` sem mensagens.
 * @property maxSequence maior `sequence` guardada, ou `null` sem mensagens.
 */
data class MessageStats(
    val count: Int,
    val minSequence: Int?,
    val maxSequence: Int?,
)

/** `true` se existe uma `sequence` faltando entre a menor e a maior guardadas. */
val MessageStats.hasHole: Boolean
    get() = minSequence != null && maxSequence != null && count != maxSequence - minSequence + 1
