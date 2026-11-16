package com.project.solaria_mobile.feature.communication.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.currentUserId
import com.project.solaria_mobile.core.database.PendingMessageDao
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.offline.CacheIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class LegacyMessageImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pending: PendingMessageDao,
    private val identity: CacheIdentity,
    private val credentials: CredentialProvider,
    private val mockMode: MockModeStore,
) {
    private val mutex = Mutex()

    suspend fun import(conversationId: String, messageType: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val file = context.getDatabasePath("message-outbox.db")
            if (!file.exists()) return@withContext
            val actor = credentials.currentUserId() ?: return@withContext
            val scope = identity.current() ?: return@withContext
            val mock = mockMode.isEnabled()
            val account = if (mock) "mock:${BuildConfig.ENVIRONMENT}" else identity.current() ?: return@withContext
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { legacy ->
                legacy.query("pending_message", null, "account = ? AND conversationId = ?", arrayOf(account, conversationId), null, null, "createdAt").use { cursor ->
                    while (cursor.moveToNext()) {
                        if (mockMode.isEnabled() != mock || identity.current() != scope || credentials.currentUserId() != actor) return@withContext
                        fun text(name: String) = cursor.getString(cursor.getColumnIndexOrThrow(name))
                        val id = text("id")
                        pending.insert(PendingMessageEntity(
                            clientMessageId = id, senderId = actor, conversationId = conversationId, messageType = messageType,
                            content = text("text"), createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                            attempts = if (cursor.getInt(cursor.getColumnIndexOrThrow("uncertain")) != 0 || text("state") == "SENDING") 1 else 0,
                            failed = text("state") == "FAILED",
                        ))
                        legacy.delete("pending_message", "id = ?", arrayOf(id))
                    }
                }
            }
        }
    }
}
