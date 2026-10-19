package com.project.solaria_mobile.core.network.realtime

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
