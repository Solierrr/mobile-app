package com.project.solaria_mobile.feature.communication.domain

enum class AvatarTone { ORANGE, BLUE, GREEN, PINK }

enum class ServiceCallIcon { HASH, PHONE, SHIELD_ALERT }

enum class MessageStatus { SENT, PENDING, FAILED }

data class Conversation(
    val id: String,
    val initials: String,
    val name: String,
    val preview: String,
    val time: String,
    val tone: AvatarTone,
    val accessLost: Boolean = false,
    val unreadCount: Int = 0,
    val muted: Boolean = false,
    val archived: Boolean = false,
    val group: Boolean = false,
    val chatbot: Boolean = false,
)

data class ChatMessage(
    val id: String,
    val text: String,
    val mine: Boolean,
    val time: String,
    val sequence: Int? = null,
    val clientMessageId: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val sender: String? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val sentAt: Long? = null,
    val changeable: Boolean = false,
)

data class ServiceCall(val id: String, val title: String, val detail: String, val icon: ServiceCallIcon, val badge: String? = null)

data class NotificationGroup(val title: String, val calls: List<ServiceCall>)

/**
 * O que a tela mostra de ao vivo numa conversa.
 *
 * @property known `false` enquanto não há conexão em tempo real ou o servidor ainda não mandou o retrato da presença.
 *   A tela não deve mostrar "offline" nesse caso, só esconder a informação.
 * @property peerOnline na conversa direta, se o outro participante está online. `null` fora da conversa direta ou com
 *   [known] falso.
 * @property onlineCount no grupo, quantos participantes além do usuário estão online.
 * @property typingNames nomes de quem está digitando agora, sem o usuário.
 */
data class ChatPresence(
    val known: Boolean = false,
    val peerOnline: Boolean? = null,
    val onlineCount: Int = 0,
    val typingNames: List<String> = emptyList(),
)
