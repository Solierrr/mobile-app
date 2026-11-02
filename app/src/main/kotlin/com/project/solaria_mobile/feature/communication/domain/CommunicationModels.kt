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
    /** Nome de quem participa, por `authId`, quando foi possível resolver; usado nos remetentes de grupo e em "digitando". */
    val participantNames: Map<String, String> = emptyMap())

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
 * Conteúdo além do texto. O api-messenger ainda não tem contrato para anexos (a proposta está no VersioSpec), então só o
 * modo de demonstração produz estes valores; o servidor real devolve apenas texto.
 */
sealed interface MessageAttachment {
    data class Image(val url: String) : MessageAttachment
    data class File(val name: String, val sizeBytes: Long, val mimeType: String, val url: String) : MessageAttachment
    data class Location(val latitude: Double, val longitude: Double, val label: String = "") : MessageAttachment
}

/** Estado pessoal da conversa: silenciar e arquivar valem só para a própria conta. */
data class ConversationState(val muted: Boolean, val archived: Boolean)

data class ServiceCall(
    val id: String, val title: String, val detail: String, val icon: ServiceCallIcon, val badge: String? = null,
    val projectId: String? = null, val projectName: String = "", val status: String? = null,
    val scheduledAt: java.time.OffsetDateTime? = null, val acceptedAt: java.time.OffsetDateTime? = null,
    val endedAt: java.time.OffsetDateTime? = null, val address: String = "", val canManage: Boolean = false,
    val hasExecutors: Boolean = false, val managedByAgenda: Boolean = false,
)

