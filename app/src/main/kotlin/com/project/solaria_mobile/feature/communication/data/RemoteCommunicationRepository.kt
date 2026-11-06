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
import com.project.solaria_mobile.feature.communication.domain.MessageAttachment
import com.project.solaria_mobile.feature.communication.domain.Conversation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.project.solaria_mobile.feature.communication.domain.ConversationState

@Singleton
class RemoteCommunicationRepository @Inject constructor(
    private val api: MessengerApi,
    private val executor: ApiCallExecutor,
    private val credentials: CredentialProvider,
) : CommunicationRepository {
    override suspend fun conversations(): ApiResult<List<Conversation>> {
        val me = currentUserId()
        return when (val result = executor.execute { api.conversations() }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(withPreviews(result.data, me))
        }
    }

    /**
     * A versão atual do messenger já manda a última mensagem e as não lidas na lista. A versão anterior não manda: nela, busca
     * só a mais recente de cada conversa, com poucas chamadas ao mesmo tempo. `unreadCount` presente indica o contrato novo,
     * em que conversa sem `lastMessage` realmente não tem mensagens.
     */
    private suspend fun withPreviews(items: List<ConversationDto>, me: String?): List<Conversation> = coroutineScope {
        val gate = Semaphore(PREVIEW_CONCURRENCY)
        items.map { dto ->
            val conversation = dto.toConversation()
            async {
                val listed = dto.lastMessage
                if (listed != null) return@async conversation.copy(preview = previewText(listed.content.orEmpty(), listed.deleted, listed.senderId, me))
                if (dto.unreadCount != null) return@async conversation
                gate.withPermit {
                    val latest = (executor.execute { api.messages(conversation.id, limit = 1) } as? ApiResult.Success)?.data?.lastOrNull()
                    if (latest == null) conversation else conversation.copy(preview = latest.preview(me))
                }
            }
        }.awaitAll()
    }

    override suspend fun conversation(id: String): ApiResult<Conversation> =
        executor.execute { api.conversation(id) }.map { it.toConversation() }

    override suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>> {
        val me = currentUserId()
        // O servidor devolve as mais recentes em ordem crescente; o histórico anterior vem por `beforeSequence`.
        return executor.execute { api.messages(conversationId, limit = PAGE_SIZE) }
            .map { items -> items.sortedBy { it.sequence }.map { it.toMessage(me) } }
    }

    override suspend fun olderMessages(conversationId: String, beforeSequence: Int): ApiResult<List<ChatMessage>> {
        val me = currentUserId()
        return executor.execute { api.messages(conversationId, limit = PAGE_SIZE, beforeSequence = beforeSequence) }
            .map { items -> items.sortedBy { it.sequence }.map { it.toMessage(me) } }
    }

    override suspend fun messagesSince(conversationId: String, sinceSequence: Int): ApiResult<List<ChatMessage>> {
        val me = currentUserId()
        return executor.execute { api.messages(conversationId, limit = MAX_CATCH_UP, sinceSequence = sinceSequence) }
            .map { items -> items.sortedBy { it.sequence }.map { it.toMessage(me) } }
    }

    override suspend fun startDirectConversation(recipientAuthId: String): ApiResult<String> =
        executor.execute { api.direct(DirectConversationRequest(recipientAuthId)) }.map { it.id }

    override suspend fun sendAttachment(conversationId: String, attachment: MessageAttachment, caption: String): ApiResult<ChatMessage> {
        val me = currentUserId()
        val conversation = when (val result = executor.execute { api.conversation(conversationId) }) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.data
        }
        val request = SendMessageRequest(conversationId, conversation.messageType(), caption, attachment = attachment.toDto())
        return executor.execute { api.send(request) }.map { it.toMessage(me) }
    }

    override suspend fun sendMessage(conversationId: String, text: String, clientMessageId: String?): ApiResult<ChatMessage> {
        val me = currentUserId()
        val conversation = when (val result = executor.execute { api.conversation(conversationId) }) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.data
        }
        val request = SendMessageRequest(conversationId, conversation.messageType(), text, clientMessageId = clientMessageId)
        return executor.execute { api.send(request) }.map { it.toMessage(me) }
    }

    override suspend fun editMessage(id: String, text: String): ApiResult<ChatMessage> {
        val me = currentUserId()
        return executor.execute { api.editMessage(id, EditMessageRequest(text)) }.map { it.toMessage(me) }
    }

    override suspend fun deleteMessage(id: String): ApiResult<ChatMessage> {
        val me = currentUserId()
        return executor.execute { api.deleteMessage(id) }.map { it.toMessage(me) }
    }

    override suspend fun conversationState(id: String, muted: Boolean?, archived: Boolean?, lastReadSequence: Int?): ApiResult<ConversationState> =
        executor.execute { api.conversationState(id, ConversationStatePatch(muted, archived, lastReadSequence)) }.map { ConversationState(it.muted, it.archived) }

    override fun currentUserId(): String? = credentials.accessToken()?.let(::accessTokenSubject)
}

private const val PREVIEW_CONCURRENCY = 4
internal const val PAGE_SIZE = 40
private const val MAX_CATCH_UP = 500
private const val DELETED_PREVIEW = "Mensagem excluída"

private val tones = AvatarTone.entries

private fun MessageDto.preview(currentUserId: String?): String = previewText(content, deleted, senderId, currentUserId)

private fun previewText(content: String, deleted: Boolean, senderId: String?, currentUserId: String?): String {
    if (deleted) return DELETED_PREVIEW
    val text = content.replace(Regex("[*_`#>~]"), "").replace(Regex("\\s+"), " ").trim()
    return if (currentUserId != null && senderId == currentUserId) "Você: $text" else text
}

private fun ConversationDto.messageType(): String = when (conversationType) {
    "GROUP" -> "USER_TO_GROUP"
    "CHAT_BOT" -> "USER_TO_CHATBOT"
    else -> "USER_TO_USER"
}

internal fun ConversationDto.toConversation(): Conversation {
    val name = title?.takeIf { it.isNotBlank() } ?: if (conversationType == "CHAT_BOT") "" else "Conversa"
    return Conversation(
        id = id,
        initials = initialsOf(name),
        name = name,
        preview = "",
        time = formatTimestamp(lastInteractionAt),
        tone = if (conversationType == "CHAT_BOT") AvatarTone.ORANGE else tones[(name.hashCode() and Int.MAX_VALUE) % tones.size],
        muted = muted == true,
        unread = (unreadCount ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        group = conversationType == "GROUP",
        assistant = conversationType == "CHAT_BOT",
        threadId = (userDetails?.get("threadId") as? kotlinx.serialization.json.JsonPrimitive)?.content,
    )
}

internal fun MessageDto.toMessage(currentUserId: String?): ChatMessage =
    ChatMessage(
        text = if (deleted) "" else content,
        mine = currentUserId != null && senderId == currentUserId,
        time = formatTimestamp(timestamp),
        clientMessageId = clientMessageId,
        id = id,
        edited = edited,
        deleted = deleted,
        sequence = sequence,
        sentAt = timestamp?.let { runCatching { java.time.OffsetDateTime.parse(it) }.getOrNull() },
        senderId = senderId,
        senderName = senderName,
        attachment = if (deleted) null else attachment?.toAttachment(),
    )

internal fun AttachmentDto.toAttachment(): MessageAttachment? = when (type) {
    "IMAGE" -> url?.let { MessageAttachment.Image(it) }
    "FILE" -> url?.let { MessageAttachment.File(name.orEmpty(), sizeBytes ?: 0L, mimeType.orEmpty(), it) }
    "LOCATION" -> if (latitude != null && longitude != null) MessageAttachment.Location(latitude, longitude, label.orEmpty()) else null
    else -> null
}

internal fun MessageAttachment.toDto(): AttachmentDto = when (this) {
    is MessageAttachment.Image -> AttachmentDto("IMAGE", url = url)
    is MessageAttachment.File -> AttachmentDto("FILE", url = url, name = name, sizeBytes = sizeBytes, mimeType = mimeType)
    is MessageAttachment.Location -> AttachmentDto("LOCATION", latitude = latitude, longitude = longitude, label = label)
}
