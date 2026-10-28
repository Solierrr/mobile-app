package com.project.solaria_mobile

import android.app.Application
import com.project.solaria_mobile.core.network.realtime.RealtimeLifecycleBinder
import com.project.solaria_mobile.feature.communication.data.MessageOutbox
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Raiz para montagem de dependencias do hilt
 */
@HiltAndroidApp
class App : Application() {
    @Inject lateinit var realtimeLifecycleBinder: RealtimeLifecycleBinder
    @Inject lateinit var messageOutbox: MessageOutbox

    override fun onCreate() {
        super.onCreate()
        realtimeLifecycleBinder.bind()
        messageOutbox.start()
    }
}
