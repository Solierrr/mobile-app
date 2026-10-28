package com.project.solaria_mobile.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Linha da tabela `pending_messages`: uma mensagem do usuário que ainda não foi confirmada pelo servidor.
 *
 * Gravada por [MessageOutbox] antes de qualquer envio. A linha é apagada quando o servidor confirma.
 *
 * @property localId chave local gerada pelo Room. A ordem de envio segue esse valor (FIFO).
 * @property clientMessageId id gerado no aparelho, único no índice. Liga a linha à mensagem confirmada.
 * @property senderId `sub` do JWT em minúsculas. Só o usuário logado envia ou exibe a linha.
 * @property conversationId conversa de destino.
 * @property messageType tipo exigido pelo servidor para a conversa: `USER_TO_USER`, `USER_TO_GROUP` ou `USER_TO_CHATBOT`.
 * @property content texto da mensagem.
 * @property createdAt instante da gravação, em milissegundos. Vira o horário da bolha.
 * @property attempts envios já tentados. Incrementado antes de cada POST. Acima de zero, o worker confere o histórico antes de reenviar.
 * @property failed `true` quando o servidor recusou a mensagem. Fica parada até "Tentar de novo" ou "Descartar".
 */
@Entity(
    tableName = "pending_messages",
    indices = [Index(value = ["clientMessageId"], unique = true), Index(value = ["senderId", "conversationId"])],
)
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val clientMessageId: String,
    val senderId: String,
    val conversationId: String,
    val messageType: String,
    val content: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val failed: Boolean = false,
)
