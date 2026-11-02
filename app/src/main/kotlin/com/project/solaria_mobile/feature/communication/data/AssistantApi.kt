package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * `conversation_id` é o identificador da sessão (a "thread") que o próprio cliente escolhe: o ai-assistant cria a conversa no
 * api-messenger na primeira mensagem e devolve o id dela em `messenger_conversation_id`.
 */
@Serializable
data class AssistantChatRequest(
    @SerialName("conversation_id") val conversationId: String,
    val message: String,
)

/** `outcome` vem como `ANSWERED`, `REFUSED`, `DEGRADED` ou `TOOL_FAILED`; em todos há texto de resposta. */
@Serializable
data class AssistantChatResponse(
    val response: String = "",
    val outcome: String = "ANSWERED",
    val reason: String? = null,
    @SerialName("messenger_conversation_id") val messengerConversationId: String? = null,
)

interface AssistantApi {
    /** Um turno completo, sem streaming; a resposta pode levar mais de um minuto. */
    @POST("chat")
    suspend fun chat(@Body body: AssistantChatRequest): AssistantChatResponse
}
