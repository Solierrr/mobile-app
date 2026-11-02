package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.DELETE
import retrofit2.http.PATCH

@Serializable
data class ConversationDto(
    val id: String,
    val conversationType: String? = null,
    val title: String? = null,
    val lastInteractionAt: String? = null,
    val muted: Boolean? = null,
    val archived: Boolean? = null,
    val unreadCount: Long? = null,
    val lastMessage: MessagePreviewDto? = null,
    /** Detalhes que quem cria a conversa informa; o ai-assistant guarda aqui o `threadId` da sessão. */
    val userDetails: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    /** `authId` de quem participa; é a única pista de quem são as pessoas, porque o messenger não manda nomes. */
    val participantIds: List<String> = emptyList(),
)

/** Última mensagem que o messenger já manda na lista, com o marcador de excluída. */
@Serializable
data class MessagePreviewDto(
    val id: String? = null,
    val senderId: String? = null,
    val content: String? = null,
    val deleted: Boolean = false,
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String? = null,
    val content: String = "",
    val timestamp: String? = null,
    val sequence: Int = 0,
    val clientMessageId: String? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    /** Só o modo de demonstração preenche estes dois; o messenger real não envia nome (ele vem de [ParticipantNames]). */
    val senderName: String? = null,
    val attachment: AttachmentDto? = null,
)

/** Anexo de uma mensagem, `type` em `IMAGE`, `FILE` ou `LOCATION`. Proposta de contrato, ainda sem suporte no servidor. */
@Serializable
data class AttachmentDto(
    val type: String,
    val url: String? = null,
    val name: String? = null,
    val sizeBytes: Long? = null,
    val mimeType: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val label: String? = null,
)

@Serializable
data class SendMessageRequest(
    val conversationId: String,
    val messageType: String,
    val content: String,
    val role: String = "user",
    val clientMessageId: String? = null,
    val attachment: AttachmentDto? = null,
)

interface MessengerApi {
    @GET("messaging/conversations/me")
    suspend fun conversations(@Query("archived") archived: Boolean = false): List<ConversationDto>

    @GET("messaging/conversations/{id}")
    suspend fun conversation(@Path("id") id: String): ConversationDto

    @GET("messaging/messages/conversation/{conversationId}")
    suspend fun messages(
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int = 100,
        @Query("sinceSequence") sinceSequence: Int? = null,
        @Query("beforeSequence") beforeSequence: Int? = null,
    ): List<MessageDto>

    @POST("messaging/messages")
    suspend fun send(@Body body: SendMessageRequest): MessageDto

    /** Cria a conversa direta com o destinatário, ou devolve a que já existe com ele. */
    @POST("messaging/conversations/direct")
    suspend fun direct(@Body body: DirectConversationRequest): ConversationDto

    @PATCH("messaging/messages/{id}")
    suspend fun editMessage(@Path("id") id: String, @Body body: EditMessageRequest): MessageDto

    @DELETE("messaging/messages/{id}")
    suspend fun deleteMessage(@Path("id") id: String): MessageDto

    /** Estado pessoal da conversa; sem campos, devolve o estado atual sem alterar nada. */
    @PATCH("messaging/conversations/{id}/state")
    suspend fun conversationState(@Path("id") id: String, @Body body: ConversationStatePatch): ConversationStateDto
}

@Serializable
data class EditMessageRequest(val content: String)

/** `recipientId` é o identificador de login (auth) da pessoa, o mesmo `sub` do token. */
@Serializable
data class DirectConversationRequest(val recipientId: String)

/** Campos nulos ficam fora do JSON, e o messenger só altera o que foi enviado. */
@Serializable
data class ConversationStatePatch(val muted: Boolean? = null, val archived: Boolean? = null, val lastReadSequence: Int? = null)

@Serializable
data class ConversationStateDto(val conversationId: String, val muted: Boolean = false, val archived: Boolean = false)
