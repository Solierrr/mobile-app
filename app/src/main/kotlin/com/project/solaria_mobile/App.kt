package com.project.solaria_mobile

import android.app.Application
import com.project.solaria_mobile.core.network.realtime.RealtimeLifecycleBinder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Raiz para montagem de dependencias do hilt
 */
@HiltAndroidApp
class App : Application() {
    @Inject lateinit var realtime: RealtimeLifecycleBinder
    // Só a injeção importa: criar o tempo real do chat agora faz a escuta de presença e digitando começar com a conexão.
    @Inject lateinit var chatRealtime: com.project.solaria_mobile.feature.communication.data.ChatRealtime

    override fun onCreate() {
        super.onCreate()
        // O tempo real do messenger liga quando o app vai para a frente e desliga quando sai dela.
        realtime.bind()
    }
}
