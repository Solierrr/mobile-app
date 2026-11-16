package com.project.solaria_mobile.di

import com.project.solaria_mobile.core.database.ConversationEntity
import com.project.solaria_mobile.core.database.MessageEntity
import com.project.solaria_mobile.core.database.MessageStats
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.database.SyncStateEntity
import com.project.solaria_mobile.core.database.UserEntity
import kotlinx.coroutines.flow.Flow
import com.project.solaria_mobile.core.database.ChatDatabaseRouter
import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.LocalDataWiper
import com.project.solaria_mobile.core.database.MessageDao
import com.project.solaria_mobile.core.database.PendingMessageDao
import com.project.solaria_mobile.core.database.SyncStateDao
import com.project.solaria_mobile.core.database.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun pending(router: ChatDatabaseRouter): PendingMessageDao = object : PendingMessageDao {
        override suspend fun insert(message: PendingMessageEntity) = router.forOperation().pendingMessages().insert(message)
        override fun observe(senderId: String, conversationId: String): Flow<List<PendingMessageEntity>> = router.observe { it.pendingMessages().observe(senderId, conversationId) }
        override suspend fun sendable(senderId: String): List<PendingMessageEntity> = router.forOperation().pendingMessages().sendable(senderId)
        override suspend fun countAttempt(clientMessageId: String) = router.forOperation().pendingMessages().countAttempt(clientMessageId)
        override suspend fun setFailed(clientMessageId: String, failed: Boolean) = router.forOperation().pendingMessages().setFailed(clientMessageId, failed)
        override suspend fun delete(clientMessageId: String) = router.forOperation().pendingMessages().delete(clientMessageId)
    }
    @Provides @Singleton
    fun messages(router: ChatDatabaseRouter): MessageDao = object : MessageDao() {
        override fun observe(conversationId: String): Flow<List<MessageEntity>> = router.observe { it.messages().observe(conversationId) }
        override suspend fun find(id: String): MessageEntity? = router.forOperation().messages().find(id)
        override suspend fun put(message: MessageEntity) = router.forOperation().messages().put(message)
        override suspend fun stats(conversationId: String): MessageStats = router.forOperation().messages().stats(conversationId)
        override suspend fun holeAfter(conversationId: String): Int? = router.forOperation().messages().holeAfter(conversationId)
        override suspend fun senders(conversationId: String): List<String> = router.forOperation().messages().senders(conversationId)
        override suspend fun clear() = router.forOperation().messages().clear()
        override suspend fun purgeChatbots() = router.forOperation().messages().purgeChatbots()
        override suspend fun upsert(incoming: List<MessageEntity>): List<MessageEntity> = router.forOperation().messages().upsert(incoming)
    }
    @Provides @Singleton
    fun conversations(router: ChatDatabaseRouter): ConversationDao = object : ConversationDao() {
        override fun observeList(): Flow<List<ConversationEntity>> = router.observe { it.conversations().observeList() }
        override fun observeArchivedList(): Flow<List<ConversationEntity>> = router.observe { it.conversations().observeArchivedList() }
        override fun observe(id: String): Flow<ConversationEntity?> = router.observe { it.conversations().observe(id) }
        override suspend fun find(id: String): ConversationEntity? = router.forOperation().conversations().find(id)
        override suspend fun all(): List<ConversationEntity> = router.forOperation().conversations().all()
        override suspend fun chatbotIds(): List<String> = router.forOperation().conversations().chatbotIds()
        override suspend fun put(conversation: ConversationEntity) = router.forOperation().conversations().put(conversation)
        override suspend fun refreshPreview(conversationId: String, messageId: String, content: String?, deleted: Boolean) = router.forOperation().conversations().refreshPreview(conversationId, messageId, content, deleted)
        override suspend fun markReadLocally(conversationId: String, sequence: Int) = router.forOperation().conversations().markReadLocally(conversationId, sequence)
        override suspend fun setLocalRead(conversationId: String, sequence: Int) = router.forOperation().conversations().setLocalRead(conversationId, sequence)
        override suspend fun pendingReads(): List<ConversationEntity> = router.forOperation().conversations().pendingReads()
        override suspend fun applyState(conversationId: String, lastReadSequence: Int, unreadCount: Long, muted: Boolean, archived: Boolean) = router.forOperation().conversations().applyState(conversationId, lastReadSequence, unreadCount, muted, archived)
        override suspend fun upsert(incoming: List<ConversationEntity>) = router.forOperation().conversations().upsert(incoming)
        override suspend fun applyList(items: List<ConversationEntity>) = router.forOperation().conversations().applyList(items)
        override suspend fun remove(id: String, syncEpoch: String, syncVersion: Long) = router.forOperation().conversations().remove(id, syncEpoch, syncVersion)
    }
    @Provides @Singleton
    fun users(router: ChatDatabaseRouter): UserDao = object : UserDao() {
        override fun observeAll(): Flow<List<UserEntity>> = router.observe { it.users().observeAll() }
        override suspend fun find(authId: String): UserEntity? = router.forOperation().users().find(authId)
        override suspend fun put(user: UserEntity) = router.forOperation().users().put(user)
        override suspend fun save(user: UserEntity) = router.forOperation().users().save(user)
    }
    @Provides @Singleton
    fun syncState(router: ChatDatabaseRouter): SyncStateDao = object : SyncStateDao {
        override suspend fun get(): SyncStateEntity? = router.forOperation().syncState().get()
        override suspend fun put(state: SyncStateEntity) = router.forOperation().syncState().put(state)
        override suspend fun clear() = router.forOperation().syncState().clear()
    }
    @Provides @Singleton
    fun wiper(router: ChatDatabaseRouter): LocalDataWiper = LocalDataWiper { withContext(Dispatchers.IO) { router.clear() } }
}
