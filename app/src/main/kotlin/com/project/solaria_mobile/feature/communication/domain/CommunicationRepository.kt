package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind

interface CommunicationRepository {
    /** Identificador da própria conta no messenger, usado para reconhecer a presença e as mensagens próprias. */
    fun currentUserId(): String? = null

    suspend fun conversations(): ApiResult<List<Conversation>>
    suspend fun conversation(id: String): ApiResult<Conversation>
    suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>>

    /** Mensagens anteriores a `beforeSequence`, em ordem crescente; lista vazia quando não há mais histórico. */
    suspend fun olderMessages(conversationId: String, beforeSequence: Int): ApiResult<List<ChatMessage>> = ApiResult.Success(emptyList())

    /** Mensagens depois de `sinceSequence`, para recuperar o que passou enquanto a conexão em tempo real estava fora. */
    suspend fun messagesSince(conversationId: String, sinceSequence: Int): ApiResult<List<ChatMessage>> = ApiResult.Success(emptyList())

    suspend fun sendMessage(conversationId: String, text: String, clientMessageId: String? = null): ApiResult<ChatMessage>

    /** Envio com anexo; só existe no modo de demonstração enquanto o messenger não tem contrato para isso. */
    /** Abre (ou reaproveita) a conversa direta com a pessoa de identificador de login [recipientAuthId] e devolve o id da conversa. */
    suspend fun startDirectConversation(recipientAuthId: String): ApiResult<String> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    suspend fun sendAttachment(conversationId: String, attachment: MessageAttachment, caption: String = ""): ApiResult<ChatMessage> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun editMessage(id: String, text: String): ApiResult<ChatMessage> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun deleteMessage(id: String): ApiResult<ChatMessage> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Sem valores, só lê o estado atual. `lastReadSequence` marca como lido até essa mensagem. */
    suspend fun conversationState(id: String, muted: Boolean? = null, archived: Boolean? = null, lastReadSequence: Int? = null): ApiResult<ConversationState> =
        ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
}
