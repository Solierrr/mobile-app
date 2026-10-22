package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.format.formatTimestamp
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.NotificationGroup
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteCommunicationRepository @Inject constructor(
    private val api: MessengerApi,
    private val executor: ApiCallExecutor,
    private val credentials: CredentialProvider,
) : CommunicationRepository {
    override suspend fun conversations(): ApiResult<List<Conversation>> =
        executor.execute { api.conversations() }.map { items -> items.map { it.toConversation() } }

    override suspend fun conversation(id: String): ApiResult<Conversation> =
        executor.execute { api.conversation(id) }.map { it.toConversation() }

    override suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>> {
        val me = currentUserId()
        return executor.execute { api.messages(conversationId) }
            .map { items -> items.sortedBy { it.sequence }.map { it.toMessage(me) } }
    }

    override suspend fun sendMessage(conversationId: String, text: String): ApiResult<ChatMessage> {
        val me = currentUserId()
        val conversation = when (val result = executor.execute { api.conversation(conversationId) }) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.data
        }
        val request = SendMessageRequest(conversationId, conversation.messageType(), text)
        return executor.execute { api.send(request) }.map { it.toMessage(me) }
    }

    override suspend fun calls(): ApiResult<List<ServiceCall>> = ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.NOT_SUPPORTED)

    override suspend fun call(id: String): ApiResult<ServiceCall> = ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.NOT_SUPPORTED)

    override suspend fun notifications(): ApiResult<List<NotificationGroup>> = ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.NOT_SUPPORTED)

    private fun currentUserId(): String? = credentials.accessToken()?.let(::accessTokenSubject)
}

private val tones = AvatarTone.entries

private fun ConversationDto.messageType(): String = when (conversationType) {
    "GROUP" -> "USER_TO_GROUP"
    "CHAT_BOT" -> "USER_TO_CHATBOT"
    else -> "USER_TO_USER"
}

internal fun ConversationDto.toConversation(): Conversation {
    val name = title?.takeIf { it.isNotBlank() } ?: "Conversa"
    return Conversation(
        id = id,
        initials = initialsOf(name),
        name = name,
        preview = "",
        time = formatTimestamp(lastInteractionAt),
        tone = tones[(name.hashCode() and Int.MAX_VALUE) % tones.size],
    )
}

internal fun MessageDto.toMessage(currentUserId: String?): ChatMessage =
    ChatMessage(text = content, mine = currentUserId != null && senderId == currentUserId, time = formatTimestamp(timestamp))
