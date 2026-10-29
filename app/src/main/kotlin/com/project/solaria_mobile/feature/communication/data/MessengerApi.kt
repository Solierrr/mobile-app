package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
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
}
