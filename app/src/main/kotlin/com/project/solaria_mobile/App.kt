package com.project.solaria_mobile

import android.app.Application
import com.project.solaria_mobile.core.network.realtime.RealtimeLifecycleBinder
import com.project.solaria_mobile.feature.communication.data.ConversationStateSync
import com.project.solaria_mobile.feature.communication.data.MessageOutbox
import com.project.solaria_mobile.feature.communication.data.MessageSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Raiz para montagem de dependencias do hilt
 */
@HiltAndroidApp
class App : Application() {
    @Inject lateinit var realtimeLifecycleBinder: RealtimeLifecycleBinder
    @Inject lateinit var messageOutbox: MessageOutbox
    @Inject lateinit var messageSync: MessageSync
    @Inject lateinit var conversationStateSync: ConversationStateSync

    override fun onCreate() {
        super.onCreate()
        realtimeLifecycleBinder.bind()
        messageOutbox.start()
        messageSync.start()
        conversationStateSync.start()
    }
}
