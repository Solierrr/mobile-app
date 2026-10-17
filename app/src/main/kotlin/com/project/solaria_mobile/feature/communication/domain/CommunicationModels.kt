package com.project.solaria_mobile.feature.communication.domain

enum class AvatarTone { ORANGE, BLUE, GREEN, PINK }

enum class ServiceCallIcon { HASH, PHONE, SHIELD_ALERT }

data class Conversation(val id: String, val initials: String, val name: String, val preview: String, val time: String, val tone: AvatarTone)

data class ChatMessage(val text: String, val mine: Boolean, val time: String)

data class ServiceCall(val id: String, val title: String, val detail: String, val icon: ServiceCallIcon, val badge: String? = null)

data class NotificationGroup(val title: String, val calls: List<ServiceCall>)
