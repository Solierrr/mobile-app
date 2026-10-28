package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class ConversationDto(
    val id: String,
    val conversationType: String? = null,
    val title: String? = null,
    val lastInteractionAt: String? = null,
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String? = null,
    val clientMessageId: String? = null,
    val content: String = "",
    val deleted: Boolean = false,
    val timestamp: String? = null,
    val sequence: Int = 0,
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
    suspend fun conversations(): List<ConversationDto>

    @GET("messaging/conversations/{id}")
    suspend fun conversation(@Path("id") id: String): ConversationDto

    @GET("messaging/messages/conversation/{conversationId}")
    suspend fun messages(
        @Path("conversationId") conversationId: String,
        @Query("sinceSequence") sinceSequence: Int? = null,
        @Query("limit") limit: Int = 100,
    ): List<MessageDto>

    @POST("messaging/messages")
    suspend fun send(@Body body: SendMessageRequest): MessageDto
}
