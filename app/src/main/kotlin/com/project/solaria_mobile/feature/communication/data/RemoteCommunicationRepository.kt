package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
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
import com.project.solaria_mobile.feature.communication.domain.GroupCreated
import java.util.concurrent.ConcurrentHashMap
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
    private val names: ParticipantNames,
    private val media: MediaGateway,
) : CommunicationRepository {
    /** Tipo de mensagem de cada conversa já vista, para o envio não precisar buscar a conversa antes. */
    private val messageTypes = ConcurrentHashMap<String, String>()

    override fun rememberConversationType(conversationId: String, conversationType: String) {
        if (conversationType in setOf("DIRECT", "GROUP", "CHAT_BOT")) messageTypes[conversationId] = messageTypeOf(conversationType)
    }

    override suspend fun conversations(): ApiResult<List<Conversation>> {
        val me = currentUserId()
        return when (val result = executor.execute { api.conversations() }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                result.data.forEach { messageTypes[it.id] = it.messageType() }
                // Na lista só a conversa direta precisa de nome de pessoa: o título do grupo já vem do servidor.
                val resolved = names.resolve(result.data.mapNotNull { it.otherParticipant(me) })
                ApiResult.Success(withPreviews(result.data, me, resolved))
            }
        }
    }

    /**
     * A versão atual do messenger já manda a última mensagem e as não lidas na lista. A versão anterior não manda: nela, busca
     * só a mais recente de cada conversa, com poucas chamadas ao mesmo tempo. `unreadCount` presente indica o contrato novo,
     * em que conversa sem `lastMessage` realmente não tem mensagens.
     */
    private suspend fun withPreviews(items: List<ConversationDto>, me: String?, resolved: Map<String, String>): List<Conversation> = coroutineScope {
        val gate = Semaphore(PREVIEW_CONCURRENCY)
        items.map { dto ->
            val conversation = dto.toConversation(me, resolved)
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

    override suspend fun conversation(id: String): ApiResult<Conversation> {
        val me = currentUserId()
        return when (val result = executor.execute { api.conversation(id) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val dto = result.data
                messageTypes[dto.id] = dto.messageType()
                val summaries = names.summaries(dto.participantIds)
                // A leitura por id não traz communityStatus; confirma na comunidade ao abrir o chat diretamente.
                val status = dto.communityStatus ?: if (dto.communityId != null)
                    (executor.execute { api.community(dto.communityId) } as? ApiResult.Success)?.data?.status else null
                ApiResult.Success(dto.copy(communityStatus = status).toConversation(me,
                    summaries.filterKeys { !it.equals(me, ignoreCase = true) }.mapNotNull { (id, summary) -> summary.username?.let { id to it } }.toMap())
                    .copy(participantAvatars = summaries.mapNotNull { (id, summary) -> summary.avatar?.let { id to it } }.toMap()))
            }
        }
    }

    /** O tipo guardado, ou, só quando a conversa ainda não foi vista, o que o servidor informa. */
    private suspend fun messageType(conversationId: String): ApiResult<String> {
        messageTypes[conversationId]?.let { return ApiResult.Success(it) }
        return executor.execute { api.conversation(conversationId) }.map { dto ->
            dto.messageType().also { messageTypes[conversationId] = it }
        }
    }

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

    override suspend fun sendAttachment(conversationId: String, attachment: MessageAttachment, caption: String, clientMessageId: String?): ApiResult<ChatMessage> {
        val me = currentUserId()
        val type = when (val result = messageType(conversationId)) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.data
        }
        val dto = when (attachment) {
            is MessageAttachment.Location -> attachment.toRequestDto(null)
            is MessageAttachment.Image -> when (val sent = upload(conversationId, "IMAGE", attachment.url, attachment.mimeType, attachment.sizeBytes)) {
                is ApiResult.Failure -> return sent
                is ApiResult.Success -> attachment.toRequestDto(sent.data)
            }
            is MessageAttachment.File -> when (val sent = upload(conversationId, attachment.mediaKind(), attachment.url, attachment.mimeType, attachment.sizeBytes)) {
                is ApiResult.Failure -> return sent
                is ApiResult.Success -> attachment.toRequestDto(sent.data)
            }
        }
        val request = SendMessageRequest(conversationId, type, caption, clientMessageId = clientMessageId, attachment = dto)
        return executor.execute { api.send(request) }.map { it.toMessage(me).keepingLocalFile(attachment) }
    }

    /** Pede a permissão de envio, manda os bytes ao bucket e devolve o `mediaId` para a mensagem citar. */
    private suspend fun upload(conversationId: String, kind: String, uri: String, mimeType: String, sizeBytes: Long): ApiResult<String> {
        // O servidor confere o tamanho exato e o teto de cada tipo; recusar aqui poupa a ida ao bucket.
        if (sizeBytes <= 0 || sizeBytes > mediaLimit(kind) || mimeType.isBlank()) return ApiResult.Failure(ErrorKind.VALIDATION)
        val granted = when (val result = executor.execute { api.mediaUpload(MediaUploadRequest(conversationId, kind, mimeType, sizeBytes)) }) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.data
        }
        val target = MediaUploadTarget(granted.upload.url, granted.upload.method, granted.upload.headers)
        return if (media.upload(target, uri, mimeType, sizeBytes)) ApiResult.Success(granted.mediaId) else ApiResult.Failure(ErrorKind.CONNECTIVITY)
    }

    override suspend fun downloadMedia(mediaId: String, name: String?): ApiResult<String> =
        when (val result = executor.execute { api.mediaContent(mediaId) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> media.save(mediaId, name, result.data)?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ErrorKind.UNKNOWN)
        }

    override suspend fun availableMedia(conversationId: String, limit: Int, cursor: String?): ApiResult<com.project.solaria_mobile.feature.communication.domain.AvailableMediaPage> =
        executor.execute { api.availableMedia(conversationId, limit, cursor) }.map { page ->
            com.project.solaria_mobile.feature.communication.domain.AvailableMediaPage(
                page.items.mapNotNull { item -> item.attachment.toAttachment()?.let { com.project.solaria_mobile.feature.communication.domain.AvailableMedia(item.messageId, item.conversationId, it) } }, page.nextCursor,
            )
        }

    override suspend fun archivedConversations(): ApiResult<List<Conversation>> {
        val me = currentUserId()
        return when (val result = executor.execute { api.conversations(archived = true) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val resolved = names.resolve(result.data.mapNotNull { it.otherParticipant(me) })
                ApiResult.Success(result.data.map { it.toListConversation(me, resolved) })
            }
        }
    }

    override suspend fun createGroup(title: String, participantIds: Set<String>): ApiResult<GroupCreated> =
        executor.execute { api.createGroup(GroupConversationRequest(title.trim(), participantIds)) }
            .map { GroupCreated(it.conversation.id, it.rejectedParticipants.size, it.rejectedParticipants.map { rejected -> com.project.solaria_mobile.feature.communication.domain.RejectedParticipant(rejected.userId, rejected.reasonCode) }) }

    override suspend fun addParticipants(id: String, userIds: Set<String>): ApiResult<List<com.project.solaria_mobile.feature.communication.domain.RejectedParticipant>> =
        executor.execute { api.addParticipants(id, ParticipantsRequest(userIds)) }.map { dto -> dto.rejectedParticipants.map { com.project.solaria_mobile.feature.communication.domain.RejectedParticipant(it.userId, it.reasonCode) } }

    override suspend fun removeParticipant(id: String, userId: String, successorId: String?): ApiResult<Unit> =
        executor.execute { api.removeParticipant(id, userId, successorId) }

    override suspend fun transferManager(id: String, successorId: String): ApiResult<Unit> =
        executor.execute { api.transferManager(id, TransferManagerRequest(successorId)); Unit }

    override suspend fun participantNames(authIds: Collection<String>): Map<String, String> = names.resolve(authIds)

    override suspend fun submitNps(conversationId: String, score: Int): ApiResult<Unit> =
        executor.execute { api.nps(NpsRequest(conversationId, score)); Unit }

    override suspend fun sendMessage(conversationId: String, text: String, clientMessageId: String?): ApiResult<ChatMessage> {
        val me = currentUserId()
        val type = when (val result = messageType(conversationId)) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.data
        }
        val request = SendMessageRequest(conversationId, type, text, clientMessageId = clientMessageId)
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

private fun ConversationDto.messageType(): String = messageTypeOf(conversationType)

private fun messageTypeOf(conversationType: String?): String = when (conversationType) {
    "GROUP" -> "USER_TO_GROUP"
    "CHAT_BOT" -> "USER_TO_CHATBOT"
    else -> "USER_TO_USER"
}

/** A conversa como linha da lista, com a prévia da última mensagem que o servidor já manda na projeção. */
internal fun ConversationDto.toListConversation(currentUserId: String?, names: Map<String, String> = emptyMap()): Conversation {
    val conversation = toConversation(currentUserId, names)
    val last = lastMessage ?: return conversation
    return conversation.copy(preview = previewText(last.content.orEmpty(), last.deleted, last.senderId, currentUserId))
}

/** Na conversa direta, a outra pessoa: quem participa e não sou eu. */
internal fun ConversationDto.otherParticipant(me: String?): String? =
    if (conversationType == "DIRECT") participantIds.firstOrNull { !it.equals(me, ignoreCase = true) }?.lowercase() else null

/** [names] são os nomes já resolvidos por `authId`; sem o nome do outro participante, a conversa direta fica como "Conversa". */
internal fun ConversationDto.toConversation(me: String? = null, names: Map<String, String> = emptyMap()): Conversation {
    val name = title?.takeIf { it.isNotBlank() } ?: otherParticipant(me)?.let(names::get) ?: if (conversationType == "CHAT_BOT") "" else "Conversa"
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
        managerId = managerUserId?.lowercase(),
        participants = participantIds.map(String::lowercase).toSet(),
        activityAt = lastInteractionAt,
        participantNames = names.filterKeys { nameId -> participantIds.any { it.equals(nameId, ignoreCase = true) } },
        communityId = communityId,
        communityStatus = communityStatus,
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

internal fun AttachmentDto.toAttachment(): MessageAttachment? {
    // Sem `mediaId` nem `url` não há o que baixar nem mostrar.
    val reference = mediaId ?: url
    val expires = expiresAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
    return when (type) {
        "IMAGE" -> reference?.let { MessageAttachment.Image(url.orEmpty(), mediaId, sizeBytes ?: 0L, mimeType ?: "image/jpeg", expires, width, height) }
        "FILE", "AUDIO", "VIDEO" -> reference?.let { MessageAttachment.File(name.orEmpty(), sizeBytes ?: 0L, mimeType.orEmpty(), url.orEmpty(), mediaId, expires) }
        "LOCATION" -> if (latitude != null && longitude != null) MessageAttachment.Location(latitude, longitude, label.orEmpty()) else null
        else -> null
    }
}

/** O anexo como o `POST /messages` o espera: o arquivo é citado por [mediaId], nunca pelo caminho no aparelho. */
internal fun MessageAttachment.toRequestDto(mediaId: String?): AttachmentDto = when (this) {
    is MessageAttachment.Image -> AttachmentDto("IMAGE", mediaId = mediaId, mimeType = mimeType, sizeBytes = sizeBytes, width = width, height = height)
    is MessageAttachment.File -> AttachmentDto(mediaKind(), mediaId = mediaId, mimeType = mimeType, sizeBytes = sizeBytes, name = name)
    is MessageAttachment.Location -> AttachmentDto("LOCATION", latitude = latitude, longitude = longitude, label = label)
}

/** O tipo do envio sai do mime: áudio e vídeo viajam como o tipo certo, o resto como arquivo. */
internal fun MessageAttachment.File.mediaKind(): String = when {
    mimeType.startsWith("audio/") -> "AUDIO"
    mimeType.startsWith("video/") -> "VIDEO"
    mimeType.startsWith("image/") -> "IMAGE"
    else -> "FILE"
}

/** Teto do servidor por tipo: imagem 10 MB, áudio 16, vídeo 64 e arquivo 25. */
internal fun mediaLimit(kind: String): Long = when (kind) {
    "IMAGE" -> 10L
    "AUDIO" -> 16L
    "VIDEO" -> 64L
    else -> 25L
} * 1024 * 1024

/** A resposta do envio só traz o `mediaId`; quem enviou continua vendo o arquivo que tem no aparelho, sem baixar de volta. */
private fun ChatMessage.keepingLocalFile(sent: MessageAttachment): ChatMessage = when (val received = attachment) {
    is MessageAttachment.Image -> if (sent is MessageAttachment.Image) copy(attachment = received.copy(url = sent.url)) else this
    is MessageAttachment.File -> if (sent is MessageAttachment.File) copy(attachment = received.copy(url = sent.url)) else this
    else -> this
}
