package com.project.solaria_mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO da lista de conversas, sobre a tabela `conversations`.
 *
 * É uma classe abstrata porque [upsert], [applyList] e [remove] aplicam as regras de versão dentro de uma
 * transação, e essas regras rodam igual nos testes JVM.
 *
 * Principais operações:
 * - [observeList] -> acompanha a lista, da interação mais recente para a mais antiga, sem as arquivadas.
 * - [observe] -> acompanha uma conversa.
 * - [upsert] -> grava as conversas que são mais novas que as guardadas.
 * - [applyList] -> aplica a lista completa do servidor e marca as conversas que sumiram.
 * - [remove] -> marca a conversa como sem acesso, a pedido do servidor.
 * - [refreshPreview] -> corrige a prévia quando a última mensagem muda.
 */
@Dao
abstract class ConversationDao {
    /** Emite as conversas não arquivadas, da interação mais recente para a mais antiga. */
    @Query("SELECT * FROM conversations WHERE archived = 0 ORDER BY lastInteractionAt DESC")
    abstract fun observeList(): Flow<List<ConversationEntity>>

    /** Emite a conversa pelo id, ou `null` enquanto ela não está no banco. */
    @Query("SELECT * FROM conversations WHERE id = :id")
    abstract fun observe(id: String): Flow<ConversationEntity?>

    /** Busca a conversa pelo id, ou `null`. */
    @Query("SELECT * FROM conversations WHERE id = :id")
    abstract suspend fun find(id: String): ConversationEntity?

    /** Lista todas as conversas guardadas, inclusive arquivadas e sem acesso. */
    @Query("SELECT * FROM conversations")
    abstract suspend fun all(): List<ConversationEntity>

    /** Lista os ids das conversas com o chatbot. */
    @Query("SELECT id FROM conversations WHERE conversationType = 'CHAT_BOT'")
    abstract suspend fun chatbotIds(): List<String>

    /** Grava a linha, trocando a que tiver o mesmo id. Não aplica a regra de versão: use [upsert]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun put(conversation: ConversationEntity)

    /**
     * Troca a prévia da conversa, se [messageId] ainda for a última mensagem dela.
     * Mantém a prévia coerente quando a última mensagem é editada ou excluída.
     */
    @Query(
        "UPDATE conversations SET lastMessageContent = :content, lastMessageDeleted = :deleted " +
            "WHERE id = :conversationId AND lastMessageId = :messageId",
    )
    abstract suspend fun refreshPreview(conversationId: String, messageId: String, content: String?, deleted: Boolean)

    /**
     * Grava as conversas que são mais novas que as guardadas, numa transação.
     *
     * @param incoming conversas recebidas, da lista ou de um evento `CONVERSATION_UPDATED`.
     */
    @Transaction
    open suspend fun upsert(incoming: List<ConversationEntity>) {
        for (conversation in incoming) {
            put(mergeConversation(find(conversation.id), conversation) ?: continue)
        }
    }

    /**
     * Aplica a lista completa do servidor e marca como sem acesso as conversas guardadas que não vieram nela.
     *
     * Só marca a conversa com `syncVersion` menor ou igual à mais antiga da lista. Uma conversa criada por evento
     * enquanto a lista era montada tem versão maior e não pode ser tomada por perdida.
     *
     * @param items todas as conversas que o servidor devolveu, arquivadas ou não.
     */
    @Transaction
    open suspend fun applyList(items: List<ConversationEntity>) {
        upsert(items)
        val returned = items.mapTo(HashSet()) { it.id }
        val cutoff = items.minOfOrNull { it.syncVersion } ?: Long.MAX_VALUE
        for (stored in all()) {
            if (stored.id !in returned && !stored.accessLost && stored.syncVersion <= cutoff) {
                put(stored.copy(accessLost = true))
            }
        }
    }

    /**
     * Marca a conversa como sem acesso, se o aviso for mais novo que a linha guardada.
     * Conversa que não está no banco é ignorada.
     *
     * @param id conversa que o usuário perdeu.
     * @param syncEpoch época do aviso.
     * @param syncVersion versão do aviso.
     */
    @Transaction
    open suspend fun remove(id: String, syncEpoch: String, syncVersion: Long) {
        val stored = find(id) ?: return
        if (syncEpoch != stored.syncEpoch || syncVersion > stored.syncVersion) {
            put(stored.copy(accessLost = true, syncEpoch = syncEpoch, syncVersion = syncVersion))
        }
    }
}
