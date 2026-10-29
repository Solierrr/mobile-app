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
)

data class ServiceCall(val id: String, val title: String, val detail: String, val icon: ServiceCallIcon, val badge: String? = null)

data class NotificationGroup(val title: String, val calls: List<ServiceCall>)
