package com.project.solaria_mobile.feature.communication.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.project.solaria_mobile.MainActivity
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.mock.MockModeStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

object VisibleConversation {
    @Volatile var id: String? = null
}

@AndroidEntryPoint
class SolariaMessagingService : FirebaseMessagingService() {
    @Inject lateinit var registration: PushRegistration
    @Inject lateinit var updates: NotificationUpdates
    @Inject lateinit var tokens: TokenStore
    @Inject lateinit var mockMode: MockModeStore

    override fun onNewToken(token: String) {
        registration.start()
        registration.tokenChanged()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (mockMode.isEnabled() || tokens.currentTokens() == null) return
        updates.changed()
        val conversationId = chatPushTarget(message.data)
        if (message.data["type"] == "NEW_MESSAGE" && conversationId != null && conversationId == VisibleConversation.id &&
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val notificationId = message.data["notificationId"]?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }
        val id = notificationId ?: conversationId?.let { "chat-$it" } ?: return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.push_canal), NotificationManager.IMPORTANCE_DEFAULT))
        val open = Intent(this, MainActivity::class.java).putExtra(OPEN_INBOX, true)
            .putExtra("notificationId", notificationId)
            .putExtra(OPEN_CONVERSATION, conversationId)
            .setData(android.net.Uri.parse("solaria-notify://notification/$id"))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id, 0, NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.solaria_ic_bell)
            .setContentTitle("Solaria")
            .setContentText(getString(R.string.push_nova_notificacao))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build())
    }

    companion object {
        const val CHANNEL = "solaria_updates"
        const val OPEN_INBOX = "solaria_open_notifications"
        const val OPEN_CONVERSATION = "solaria_open_conversation"
    }
}
