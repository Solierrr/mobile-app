package com.project.solaria_mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO da fila offline de mensagens do chat, sobre a tabela `pending_messages`.
 *
 * Principais operações:
 * - [insert] -> grava a mensagem antes de qualquer envio.
 * - [observe] -> acompanha as mensagens pendentes de uma conversa.
 * - [sendable] -> lista, em ordem de gravação, o que ainda pode ser enviado.
 * - [countAttempt] -> registra uma tentativa de envio.
 * - [setFailed] -> marca ou libera uma mensagem recusada.
 * - [delete] -> remove a mensagem depois da confirmação ou do descarte.
 */
@Dao
interface PendingMessageDao {
    /**
     * Grava uma mensagem nova na fila. Falha se o `clientMessageId` já existir
     */
    @Insert
    suspend fun insert(message: PendingMessageEntity)

    /**
     * Emite as mensagens da conversa do remetente, da mais antiga para a mais nova, a cada mudança na tabela.
     */
    @Query("SELECT * FROM pending_messages WHERE senderId = :senderId AND conversationId = :conversationId ORDER BY localId")
    fun observe(senderId: String, conversationId: String): Flow<List<PendingMessageEntity>>

    /**
     * Lista, na ordem de gravação, as mensagens do remetente que não foram recusadas.
     * As recusadas ficam de fora até serem liberadas por [setFailed].
     */
    @Query("SELECT * FROM pending_messages WHERE senderId = :senderId AND failed = 0 ORDER BY localId")
    suspend fun sendable(senderId: String): List<PendingMessageEntity>

    /**
     * Soma uma tentativa de envio. O worker chama antes do POST: se a conexão cair depois que o servidor gravou,
     * o contador já indica que é preciso conferir o histórico antes de reenviar.
     */
    @Query("UPDATE pending_messages SET attempts = attempts + 1 WHERE clientMessageId = :clientMessageId")
    suspend fun countAttempt(clientMessageId: String)

    /**
     * Marca a mensagem como recusada (`failed = true`) ou a libera para novo envio (`false`).
     */
    @Query("UPDATE pending_messages SET failed = :failed WHERE clientMessageId = :clientMessageId")
    suspend fun setFailed(clientMessageId: String, failed: Boolean)

    /** Remove a mensagem da fila, após a confirmação do servidor ou o descarte pelo usuário. */
    @Query("DELETE FROM pending_messages WHERE clientMessageId = :clientMessageId")
    suspend fun delete(clientMessageId: String)
}
