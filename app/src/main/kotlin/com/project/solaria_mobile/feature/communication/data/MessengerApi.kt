package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class LastMessageDto(
    val id: String,
    val sequence: Int = 0,
    val content: String? = null,
    val deleted: Boolean = false,
)

@Serializable
data class ConversationDto(
    val id: String,
    val syncEpoch: String? = null,
    val syncVersion: Long = 0,
    val conversationType: String? = null,
    val participantIds: List<String> = emptyList(),
    val title: String? = null,
    val archived: Boolean? = null,
    val lastInteractionAt: String? = null,
    val lastMessage: LastMessageDto? = null,
    val lastReadSequence: Int? = null,
    val unreadCount: Long? = null,
    val muted: Boolean? = null,
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String? = null,
    val clientMessageId: String? = null,
    val content: String = "",
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val timestamp: String? = null,
    val sequence: Int = 0,
    val changeVersion: Long = 0,
)

@Serializable
data class MessageUpdatesDto(
    val syncEpoch: String,
    val cursor: Long,
    val hasMore: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
)

@Serializable
data class ConversationRemovedDto(
    val id: String,
    val syncEpoch: String = "",
    val syncVersion: Long = 0,
)

@Serializable
data class SendMessageRequest(
    val conversationId: String,
    val messageType: String,
    val content: String,
    val clientMessageId: String,
)

/**
 * Corpo do PATCH de estado da conversa. Campo `null` não vai no JSON, e o servidor lê campo ausente como "não mudar".
 */
@Serializable
data class ConversationStateRequest(
    val lastReadSequence: Int? = null,
    val muted: Boolean? = null,
    val archived: Boolean? = null,
)

@Serializable
data class ConversationStateDto(
    val conversationId: String,
    val syncEpoch: String? = null,
    val syncVersion: Long = 0,
    val lastReadSequence: Int = 0,
    val unreadCount: Long = 0,
    val muted: Boolean = false,
    val archived: Boolean = false,
    val effectiveMuted: Boolean = false,
)

@Serializable
data class EditMessageRequest(val content: String)

@Serializable
data class RealtimeEventDto(
    val type: String,
    val conversationId: String? = null,
    val payload: JsonElement? = null,
)

interface MessengerApi {
    @GET("messaging/conversations/me")
    suspend fun conversations(@Query("archived") archived: Boolean = false): List<ConversationDto>

    @GET("messaging/conversations/{id}")
    suspend fun conversation(@Path("id") id: String): ConversationDto

    @GET("messaging/messages/conversation/{conversationId}")
    suspend fun messages(
        @Path("conversationId") conversationId: String,
        @Query("sinceSequence") sinceSequence: Int? = null,
        @Query("beforeSequence") beforeSequence: Int? = null,
        @Query("limit") limit: Int = 100,
    ): List<MessageDto>

    @GET("messaging/updates")
    suspend fun updates(@Query("since") since: Long? = null, @Query("limit") limit: Int? = null): MessageUpdatesDto

    @POST("messaging/messages")
    suspend fun send(@Body body: SendMessageRequest): MessageDto

    @PATCH("messaging/conversations/{id}/state")
    suspend fun patchState(@Path("id") id: String, @Body body: ConversationStateRequest): ConversationStateDto

    @PATCH("messaging/messages/{id}")
    suspend fun edit(@Path("id") id: String, @Body body: EditMessageRequest): MessageDto

    @DELETE("messaging/messages/{id}")
    suspend fun delete(@Path("id") id: String): MessageDto
}
