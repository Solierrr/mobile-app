package com.project.solaria_mobile.feature.communication.data

enum class AvatarTone { ORANGE, BLUE, GREEN, PINK }
enum class ServiceCallIcon { HASH, PHONE, SHIELD_ALERT }

data class Conversation(val initials: String, val name: String, val preview: String, val time: String, val tone: AvatarTone)
data class ChatMessage(val text: String, val mine: Boolean, val time: String)
data class ServiceCall(val title: String, val detail: String, val icon: ServiceCallIcon, val badge: String? = null)
