package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.domain.ChatPresence

internal fun ChatPresence.label(): String? = when {
    !known -> null
    typingNames.isNotEmpty() -> typingNames.joinToString(", ") + " digitando…"
    peerOnline == true -> "Online"
    peerOnline == false -> "Offline"
    onlineCount > 0 -> "$onlineCount online"
    else -> null
}
