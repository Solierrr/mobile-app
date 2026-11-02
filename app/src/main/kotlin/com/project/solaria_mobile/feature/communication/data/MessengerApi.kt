package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Streaming

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
    /** `authId` de quem participa: cruzado com a presença e usado para buscar os nomes, que o messenger não manda. */
    val participantIds: List<String> = emptyList(),
    /** Quem gere o grupo: só essa pessoa adiciona, remove e passa a gestão. */
    val managerUserId: String? = null,
    /** Convidados que o servidor não admitiu, na resposta de `POST /participants`. */
    val rejectedParticipants: List<RejectedGroupParticipantDto> = emptyList(),
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
    /** Versão do relógio de sincronização em que a mensagem mudou pela última vez; ordena o catch-up. Zero em mensagem antiga. */
    val changeVersion: Long = 0,
    /** Só o modo de demonstração preenche estes dois; o messenger real não envia nome (ele vem de [ParticipantNames]). */
    val senderName: String? = null,
    val attachment: AttachmentDto? = null,
)

/**
 * Resposta de `GET /messaging/updates`: o estado atual das mensagens que mudaram depois do `since`, em ordem de [MessageDto.changeVersion].
 * [cursor] é o `since` da próxima chamada; sem `since` na chamada ele é a versão atual e [messages] vem vazia. Se [syncEpoch] muda, as
 * versões guardadas deixaram de valer.
 */
@Serializable
data class MessageUpdatesDto(
    val syncEpoch: String = "",
    val cursor: Long = 0,
    val hasMore: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
)

/**
 * Anexo de uma mensagem, `type` em `IMAGE`, `VIDEO`, `AUDIO`, `FILE` ou `LOCATION`. No envio e no recebimento o arquivo é citado
 * por [mediaId]; [url] só existe no modo de demonstração. [downloadPath] e [expiresAt] só vêm do servidor (a mídia some em 7 dias).
 */
@Serializable
data class AttachmentDto(
    val type: String,
    val url: String? = null,
    val mediaId: String? = null,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val expiresAt: String? = null,
    val downloadPath: String? = null,
    val name: String? = null,
    val sizeBytes: Long? = null,
    val mimeType: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val label: String? = null,
)

@Serializable
data class GroupConversationRequest(val title: String, val participantIds: Set<String>)

@Serializable
data class ParticipantsRequest(val userIds: Set<String>)

@Serializable
data class TransferManagerRequest(val successorUserId: String)

@Serializable
data class MediaUploadRequest(val conversationId: String, val type: String, val mimeType: String, val sizeBytes: Long)

@Serializable
data class MediaUploadDto(val mediaId: String, val upload: MediaUploadTargetDto, val uploadExpiresAt: String? = null, val maxBytes: Long = 0)

@Serializable
data class MediaUploadTargetDto(val url: String, val method: String = "PUT", val headers: Map<String, String> = emptyMap())

/** `environment` é opcional; o servidor assume o do próprio ambiente. */
@Serializable
data class NpsRequest(val conversationId: String, val score: Int)

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

    /** Mensagens de todas as conversas da conta que mudaram depois de `since`; sem `since` devolve só o ponto de partida. */
    @GET("messaging/updates")
    suspend fun updates(@Query("since") since: Long? = null, @Query("limit") limit: Int = 500): MessageUpdatesDto

    @POST("messaging/messages")
    suspend fun send(@Body body: SendMessageRequest): MessageDto

    /** Grupo avulso: quem cria vira o gestor. Os convidados que o servidor recusou voltam em `rejectedParticipants`. */
    @POST("messaging/conversations/group")
    suspend fun createGroup(@Body body: GroupConversationRequest): GroupCreationDto

    @POST("messaging/conversations/{id}/participants")
    suspend fun addParticipants(@Path("id") id: String, @Body body: ParticipantsRequest): ConversationDto

    @PUT("messaging/conversations/{id}/manager")
    suspend fun transferManager(@Path("id") id: String, @Body body: TransferManagerRequest): kotlinx.serialization.json.JsonElement

    /** Remover outra pessoa ou sair; o gestor que sai com outros no grupo precisa de [successorUserId]. */
    @DELETE("messaging/conversations/{id}/participants/{userId}")
    suspend fun removeParticipant(@Path("id") id: String, @Path("userId") userId: String, @Query("successorUserId") successorUserId: String? = null)

    /** Pede a permissão de envio de um arquivo; os bytes vão direto para o bucket. */
    @POST("messaging/media")
    suspend fun mediaUpload(@Body body: MediaUploadRequest): MediaUploadDto

    /** O servidor responde 302 para um link assinado do bucket e o OkHttp o segue; sem o `Authorization` no outro host. */
    @Streaming
    @GET("messaging/media/{mediaId}/content")
    suspend fun mediaContent(@Path("mediaId") mediaId: String): okhttp3.ResponseBody

    /** Nota de 0 a 10 de uma sessão do assistente; 409 `DUPLICATE_RESOURCE` quando a pessoa já respondeu. */
    @POST("messaging/nps")
    suspend fun nps(@Body body: NpsRequest): kotlinx.serialization.json.JsonElement

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
