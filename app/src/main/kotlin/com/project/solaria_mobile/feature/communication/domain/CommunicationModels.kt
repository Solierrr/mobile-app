package com.project.solaria_mobile.feature.communication.domain

enum class AvatarTone { ORANGE, BLUE, GREEN, PINK }

enum class ServiceCallIcon { HASH, PHONE, SHIELD_ALERT }

data class Conversation(val id: String, val initials: String, val name: String, val preview: String, val time: String, val tone: AvatarTone, val muted: Boolean = false, val unread: Int = 0, val group: Boolean = false,
    /** Sessão do assistente (conversa do tipo CHAT_BOT); só aparece na página do assistente. */
    val assistant: Boolean = false,
    /** Sessão do assistente geográfico, ligada a um projeto do mapa; o `id` é `geo:<projeto>:<sessão>`. */
    val geo: Boolean = false,
    /** Identificador da sessão no ai-assistant, guardado pela conversa em `userDetails.threadId`. */
    val threadId: String? = null,
    /** Participantes pelo identificador de login, em minúsculas; vazio quando o servidor não informa. */
    val participants: Set<String> = emptySet(),
    /** Data ISO da última interação, como o servidor a manda; ordena a lista quando chega uma atualização ao vivo. */
    val activityAt: String? = null,
    /** Gestor do grupo, em minúsculas; só ele adiciona e remove participantes. Nulo em conversa que não é grupo. */
    val managerId: String? = null,
    /** Nome de quem participa, por `authId`, quando foi possível resolver; usado nos remetentes de grupo e em "digitando". */
    val participantNames: Map<String, String> = emptyMap(),
    val participantAvatars: Map<String, String> = emptyMap(),
    val communityId: String? = null,
    val communityStatus: String? = null,
) {
    val readOnly: Boolean get() = communityStatus == "ARCHIVED" || (communityId != null && communityStatus != "ACTIVE")
}

/**
 * `clientMessageId` é o identificador que o app gerou ao enviar, devolvido pelo messenger no histórico. Mensagem excluída
 * continua no histórico como marcador, sem o texto.
 */
data class ChatMessage(
    val text: String,
    val mine: Boolean,
    val time: String,
    val clientMessageId: String? = null,
    val id: String = "",
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val sequence: Int = 0,
    val sentAt: java.time.OffsetDateTime? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val attachment: MessageAttachment? = null,
)

/**
 * Conteúdo além do texto. Foto e arquivo viajam pelo bucket (o messenger só cita o `mediaId`); a localização leva as coordenadas.
 */
sealed interface MessageAttachment {
    /** [url] é o arquivo no aparelho; vazio enquanto só há [mediaId] a baixar. [sizeBytes] e [mimeType] valem para o envio. */
    data class Image(val url: String, val mediaId: String? = null, val sizeBytes: Long = 0, val mimeType: String = "image/jpeg", val expiresAt: java.time.Instant? = null, val width: Int? = null, val height: Int? = null) : MessageAttachment
    /** Documento, áudio ou vídeo: o tipo do envio sai do [mimeType]. */
    data class File(val name: String, val sizeBytes: Long, val mimeType: String, val url: String, val mediaId: String? = null, val expiresAt: java.time.Instant? = null) : MessageAttachment
    data class Location(val latitude: Double, val longitude: Double, val label: String = "") : MessageAttachment
}

/** Página de anexos que o servidor ainda entrega; o cursor é opaco e deve voltar sem alterações. */
data class AvailableMedia(val messageId: String, val conversationId: String, val attachment: MessageAttachment)
data class AvailableMediaPage(val items: List<AvailableMedia>, val nextCursor: String?)

/** Estado pessoal da conversa: silenciar e arquivar valem só para a própria conta. */
data class ConversationState(val muted: Boolean, val archived: Boolean)

data class ServiceCall(
    val id: String, val title: String, val detail: String, val icon: ServiceCallIcon, val badge: String? = null,
    val projectId: String? = null, val projectName: String = "", val status: String? = null,
    val scheduledAt: java.time.OffsetDateTime? = null, val acceptedAt: java.time.OffsetDateTime? = null,
    val endedAt: java.time.OffsetDateTime? = null, val address: String = "", val canManage: Boolean = false,
    val hasExecutors: Boolean = false, val managedByAgenda: Boolean = false,
)

