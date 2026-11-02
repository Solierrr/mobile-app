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

    override fun onCreate() {
        super.onCreate()
        // O tempo real do messenger liga quando o app vai para a frente e desliga quando sai dela.
        realtime.bind()
    }
}
