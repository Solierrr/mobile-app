package com.project.solaria_mobile.feature.communication.data

/** Um aviso de mensagem pode abrir o chat mesmo quando o provedor não incluiu notificationId. */
internal fun chatPushTarget(data: Map<String, String>): String? =
    if (data["type"] == "NEW_MESSAGE") (data["conversationId"] ?: data["chatId"])?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) } else null
