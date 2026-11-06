package com.project.solaria_mobile.core.network.realtime

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.mock.MockModeStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * De onde vem a URL do WebSocket do messenger
 */
fun interface RealtimeEndpoint {

    fun url(): String?
}

/** ainda não fiz */
@Singleton
class NoRealtimeEndpoint @Inject constructor() : RealtimeEndpoint {
    override fun url(): String? = null
}

/**
 * WebSocket do api-messenger no ambiente do app, derivado do endereço REST: `https://host/` vira `wss://host/messaging/ws`
 * (o mesmo caminho em QA e produção). Sem URL no modo de demonstração, que tem o próprio cliente falso.
 */
@Singleton
class MessengerRealtimeEndpoint @Inject constructor(private val mockMode: MockModeStore) : RealtimeEndpoint {
    override fun url(): String? = if (mockMode.isEnabled()) null else messengerWebSocketUrl(BuildConfig.MESSENGER_BASE_URL)
}

internal fun messengerWebSocketUrl(baseUrl: String): String? {
    val host = baseUrl.trim().trimEnd('/').takeIf { it.isNotEmpty() } ?: return null
    val secure = host.replaceFirst(Regex("^http://"), "ws://").replaceFirst(Regex("^https://"), "wss://")
    return "$secure/messaging/ws"
}
