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

    /** Repõe o tipo conhecido da conversa ao retomar uma mensagem persistida, sem uma consulta extra ao servidor. */
    fun rememberConversationType(conversationId: String, conversationType: String) = Unit

    /** Abre (ou reaproveita) a conversa direta com a pessoa de identificador de login [recipientAuthId] e devolve o id da conversa. */
    suspend fun startDirectConversation(recipientAuthId: String): ApiResult<String> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    suspend fun sendAttachment(conversationId: String, attachment: MessageAttachment, caption: String = "", clientMessageId: String? = null): ApiResult<ChatMessage> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun editMessage(id: String, text: String): ApiResult<ChatMessage> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun deleteMessage(id: String): ApiResult<ChatMessage> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Conversas que esta conta arquivou; desarquivar é `conversationState(id, archived = false)`. */
    suspend fun archivedConversations(): ApiResult<List<Conversation>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Grupo avulso, fora de qualquer comunidade; [GroupCreated.rejectedParticipants] conta os convidados recusados. */
    suspend fun createGroup(title: String, participantIds: Set<String>): ApiResult<GroupCreated> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Preserva o motivo de cada convite recusado para a tela explicar a admissão parcial. */
    suspend fun addParticipants(id: String, userIds: Set<String>): ApiResult<List<RejectedParticipant>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Remove [userId] ou, se for a própria conta, sai do grupo. O gestor que sai com outros no grupo indica [successorId]. */
    suspend fun removeParticipant(id: String, userId: String, successorId: String? = null): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    suspend fun transferManager(id: String, successorId: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Nome de exibição por `authId` das pessoas que o api-core soube resolver; quem falhar fica de fora. */
    suspend fun participantNames(authIds: Collection<String>): Map<String, String> = emptyMap()

    /** Nota de 0 a 10 do assistente. 409 `DUPLICATE_RESOURCE` quer dizer que já foi respondida. */
    suspend fun submitNps(conversationId: String, score: Int): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Baixa a mídia do servidor para o aparelho e devolve o URI do arquivo; `MEDIA_EXPIRED` quando passou dos 7 dias. */
    suspend fun downloadMedia(mediaId: String, name: String?): ApiResult<String> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Mídias ainda disponíveis, da mais recente para a mais antiga. */
    suspend fun availableMedia(conversationId: String, limit: Int = 30, cursor: String? = null): ApiResult<AvailableMediaPage> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)

    /** Sem valores, só lê o estado atual. `lastReadSequence` marca como lido até essa mensagem. */
    suspend fun conversationState(id: String, muted: Boolean? = null, archived: Boolean? = null, lastReadSequence: Int? = null): ApiResult<ConversationState> =
        ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
}
