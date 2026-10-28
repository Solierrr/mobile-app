package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.common.onSuccess
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.format.formatTimestamp
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.MessageStatus
import com.project.solaria_mobile.feature.communication.domain.NotificationGroup
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.Json

@Singleton
class RemoteCommunicationRepository @Inject constructor(
    private val api: MessengerApi,
    private val executor: ApiCallExecutor,
    private val credentials: CredentialProvider,
    private val outbox: MessageOutbox,
    private val realtime: RealtimeClient,
    private val networkMonitor: NetworkMonitor,
    private val json: Json,
    private val serviceCalls: FakeCommunicationRepository,
) : CommunicationRepository {
    private val messageTypes = ConcurrentHashMap<String, String>()

    override val online: Flow<Boolean> get() = networkMonitor.isOnline

    override suspend fun conversations(): ApiResult<List<Conversation>> =
        executor.execute { api.conversations() }
            .onSuccess { items -> items.forEach(::rememberType) }
            .map { items -> items.map { it.toConversation() } }

    override suspend fun conversation(id: String): ApiResult<Conversation> =
        executor.execute { api.conversation(id) }
            .onSuccess(::rememberType)
            .map { it.toConversation() }

    override suspend fun messages(conversationId: String, afterSequence: Int?): ApiResult<List<ChatMessage>> {
        val me = currentUserId()
        return executor.execute { api.messages(conversationId, sinceSequence = afterSequence) }
            .map { items -> items.sortedBy { it.sequence }.map { it.toMessage(me) } }
    }

    override fun liveMessages(conversationId: String): Flow<ChatMessage> {
        val me = currentUserId()
        val published = realtime.subscribe("/topic/conversations.$conversationId").mapNotNull(::messageEventOf)
        val confirmed = outbox.delivered.filter { it.conversationId == conversationId }
        return merge(published, confirmed).map { it.toMessage(me) }
    }

    override fun pendingMessages(conversationId: String): Flow<List<ChatMessage>> =
        outbox.observe(conversationId).map { rows -> rows.map { it.toMessage() } }

    override fun reconnections(): Flow<Unit> = merge(
        realtime.state.filter { it == RealtimeState.Connected }.map { },
        networkMonitor.isOnline.drop(1).filter { it }.map { },
    )

    override suspend fun sendMessage(conversationId: String, text: String): ApiResult<Unit> =
        when (val type = messageTypeOf(conversationId)) {
            is ApiResult.Failure -> type
            is ApiResult.Success -> outbox.enqueue(conversationId, type.data, text)
        }

    override suspend fun retryMessage(messageId: String) = outbox.retry(messageId)

    override suspend fun discardMessage(messageId: String) = outbox.discard(messageId)

    override suspend fun calls(): ApiResult<List<ServiceCall>> = serviceCalls.calls()

    override suspend fun call(id: String): ApiResult<ServiceCall> = serviceCalls.call(id)

    override suspend fun notifications(): ApiResult<List<NotificationGroup>> = serviceCalls.notifications()

    private suspend fun messageTypeOf(conversationId: String): ApiResult<String> {
        messageTypes[conversationId]?.let { return ApiResult.Success(it) }
        return executor.execute { api.conversation(conversationId) }
            .onSuccess(::rememberType)
            .map { it.messageType() }
    }

    private fun rememberType(conversation: ConversationDto) {
        messageTypes[conversation.id] = conversation.messageType()
    }

    private fun messageEventOf(body: String): MessageDto? {
        val event = runCatching { json.decodeFromString(RealtimeEventDto.serializer(), body) }.getOrNull()
            ?: return null
        if (event.type !in MESSAGE_EVENTS) return null
        val payload = event.payload ?: return null
        return runCatching { json.decodeFromJsonElement(MessageDto.serializer(), payload) }.getOrNull()
    }

    private fun currentUserId(): String? = credentials.accessToken()?.let(::accessTokenSubject)

    private companion object {
        val MESSAGE_EVENTS = setOf("MESSAGE_CREATED", "MESSAGE_EDITED", "MESSAGE_DELETED")
    }
}

private val tones = AvatarTone.entries

private const val DELETED_MESSAGE_TEXT = "Mensagem apagada"

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
    ChatMessage(
        id = id,
        text = if (deleted) DELETED_MESSAGE_TEXT else content,
        mine = currentUserId != null && senderId.equals(currentUserId, ignoreCase = true),
        time = formatTimestamp(timestamp),
        sequence = sequence,
        clientMessageId = clientMessageId,
    )

internal fun PendingMessageEntity.toMessage(): ChatMessage =
    ChatMessage(
        id = clientMessageId,
        text = content,
        mine = true,
        time = formatTimestamp(createdAt),
        clientMessageId = clientMessageId,
        status = if (failed) MessageStatus.FAILED else MessageStatus.PENDING,
    )
