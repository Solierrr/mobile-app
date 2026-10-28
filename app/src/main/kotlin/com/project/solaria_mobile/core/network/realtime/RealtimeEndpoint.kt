package com.project.solaria_mobile.core.network.realtime

import com.project.solaria_mobile.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * De onde vem a URL do WebSocket do messenger
 */
fun interface RealtimeEndpoint {

    fun url(): String?
}

@Singleton
class MessengerRealtimeEndpoint @Inject constructor() : RealtimeEndpoint {
    override fun url(): String? = websocketUrlOf(BuildConfig.MESSENGER_BASE_URL)
}

internal fun websocketUrlOf(baseUrl: String): String? {
    val root = baseUrl.trim().removeSuffix("/")
    val socketRoot = when {
        root.startsWith("https://") -> "wss://" + root.removePrefix("https://")
        root.startsWith("http://") -> "ws://" + root.removePrefix("http://")
        else -> return null
    }
    return "$socketRoot/$WEBSOCKET_PATH"
}

private const val WEBSOCKET_PATH = "messaging/ws"
