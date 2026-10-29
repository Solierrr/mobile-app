package com.project.solaria_mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO das mensagens confirmadas, sobre a tabela `messages`.
 *
 * É uma classe abstrata porque [upsert] aplica a regra [mergeMessage] dentro de uma transação, e essa regra roda
 * igual nos testes JVM, que só implementam as consultas simples.
 *
 * Principais operações:
 * - [observe] -> acompanha as mensagens de uma conversa, da mais antiga para a mais nova.
 * - [upsert] -> grava as mensagens que são mais novas que as guardadas.
 * - [stats] -> resume o que existe de uma conversa.
 * - [holeAfter] -> acha a `sequence` que antecede o primeiro buraco.
 * - [senders] -> lista quem enviou mensagens numa conversa.
 * - [clear] -> apaga todas as mensagens.
 * - [purgeChatbots] -> apaga mensagens de conversas com o chatbot.
 */
@Dao
abstract class MessageDao {
    /** Emite as mensagens da conversa em ordem de `sequence` a cada mudança na tabela. */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sequence")
    abstract fun observe(conversationId: String): Flow<List<MessageEntity>>

    /** Busca uma mensagem pelo id, ou `null`. */
    @Query("SELECT * FROM messages WHERE id = :id")
    abstract suspend fun find(id: String): MessageEntity?

    /** Grava a linha, trocando a que tiver o mesmo id. Não aplica a regra de versão: use [upsert]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun put(message: MessageEntity)

    /** Conta as mensagens da conversa e informa a menor e a maior `sequence`. */
    @Query(
        "SELECT COUNT(*) AS count, MIN(sequence) AS minSequence, MAX(sequence) AS maxSequence " +
            "FROM messages WHERE conversationId = :conversationId",
    )
    abstract suspend fun stats(conversationId: String): MessageStats

    /**
     * Devolve a `sequence` que vem logo antes do primeiro buraco, ou `null` se não há buraco.
     * Um pedido com `sinceSequence` igual a esse valor traz o trecho que falta.
     */
    @Query(
        "SELECT MIN(m.sequence) FROM messages m WHERE m.conversationId = :conversationId " +
            "AND m.sequence < (SELECT MAX(sequence) FROM messages WHERE conversationId = :conversationId) " +
            "AND NOT EXISTS (SELECT 1 FROM messages n WHERE n.conversationId = :conversationId AND n.sequence = m.sequence + 1)",
    )
    abstract suspend fun holeAfter(conversationId: String): Int?

    /** Lista os ids de quem enviou mensagens na conversa, sem repetir. */
    @Query("SELECT DISTINCT senderId FROM messages WHERE conversationId = :conversationId AND senderId IS NOT NULL")
    abstract suspend fun senders(conversationId: String): List<String>

    /** Apaga todas as mensagens. Usado quando a época do servidor muda. */
    @Query("DELETE FROM messages")
    abstract suspend fun clear()

    /** Apaga as mensagens das conversas com o chatbot, que não devem ficar no banco. */
    @Query("DELETE FROM messages WHERE conversationId IN (SELECT id FROM conversations WHERE conversationType = 'CHAT_BOT')")
    abstract suspend fun purgeChatbots()

    /**
     * Grava as mensagens que são mais novas que as guardadas, numa transação.
     *
     * Cada mensagem passa por [mergeMessage]: versão menor ou igual é ignorada e exclusão apaga o texto.
     *
     * @param incoming mensagens recebidas do servidor.
     * @return as linhas que de fato mudaram.
     */
    @Transaction
    open suspend fun upsert(incoming: List<MessageEntity>): List<MessageEntity> {
        val applied = ArrayList<MessageEntity>()
        for (message in incoming) {
            val merged = mergeMessage(find(message.id), message) ?: continue
            put(merged)
            applied += merged
        }
        return applied
    }
}
