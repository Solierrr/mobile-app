package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.currentUserId
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.isChatbot
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.NotificationGroup
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class RemoteCommunicationRepository @Inject constructor(
    private val api: MessengerApi,
    private val executor: ApiCallExecutor,
    private val credentials: CredentialProvider,
    private val outbox: MessageOutbox,
    private val networkMonitor: NetworkMonitor,
    private val store: MessageStore,
    private val conversationDao: ConversationDao,
    private val users: UserDirectory,
    private val sync: MessageSync,
    private val serviceCalls: FakeCommunicationRepository,
    private val stateSync: ConversationStateSync,
) : CommunicationRepository {
    override val online: Flow<Boolean> get() = networkMonitor.isOnline

    override val syncResult: Flow<ApiResult<Unit>?> get() = sync.result

    override fun conversations(archived: Boolean): Flow<List<Conversation>> =
        combine(if (archived) conversationDao.observeArchivedList() else conversationDao.observeList(), users.observe()) { rows, names ->
            val me = credentials.currentUserId()
            rows.map { it.toConversation(me, names) }
        }

    override fun conversation(id: String): Flow<Conversation?> =
        combine(conversationDao.observe(id), users.observe()) { row, names ->
            row?.toConversation(credentials.currentUserId(), names)
        }

    override fun messages(conversationId: String): Flow<List<ChatMessage>> =
        combine(
            store.observe(conversationId),
            outbox.observe(conversationId),
            conversationDao.observe(conversationId),
            users.observe(),
        ) { stored, pending, conversation, names ->
            val me = credentials.currentUserId()
            val group = conversation?.conversationType == GROUP_TYPE
            timelineOf(stored.map { it.toMessage(me, names, group) }, pending.map { it.toMessage() })
        }

    override suspend fun open(conversationId: String): ApiResult<Unit> {
        val conversation = conversationDao.find(conversationId)
            ?: when (val fetched = executor.execute { api.conversation(conversationId) }) {
                is ApiResult.Failure -> return fetched
                is ApiResult.Success -> fetched.data.toEntity().also { conversationDao.upsert(listOf(it)) }
            }
        if (!conversation.isChatbot) return ApiResult.Success(Unit)
        return executor.execute { api.messages(conversationId, limit = CHATBOT_HISTORY) }.map { store.apply(it) }
    }

    override fun refresh() = sync.refresh()

    override suspend fun refreshNames(conversationId: String?) = users.refreshFor(conversationId, credentials.currentUserId())

    override suspend fun sendMessage(conversationId: String, text: String): ApiResult<Unit> {
        val conversation = conversationDao.find(conversationId) ?: return ApiResult.Failure(ErrorKind.NOT_FOUND)
        if (conversation.accessLost) return ApiResult.Failure(ErrorKind.FORBIDDEN)
        return outbox.enqueue(conversationId, conversation.messageType(), text)
    }

    override suspend fun retryMessage(messageId: String) = outbox.retry(messageId)

    override suspend fun discardMessage(messageId: String) = outbox.discard(messageId)

    override suspend fun markRead(conversationId: String) = stateSync.markRead(conversationId)

    override suspend fun setMuted(conversationId: String, muted: Boolean) = stateSync.setMuted(conversationId, muted)

    override suspend fun setArchived(conversationId: String, archived: Boolean) = stateSync.setArchived(conversationId, archived)

    override suspend fun calls(): ApiResult<List<ServiceCall>> = serviceCalls.calls()

    override suspend fun call(id: String): ApiResult<ServiceCall> = serviceCalls.call(id)

    override suspend fun notifications(): ApiResult<List<NotificationGroup>> = serviceCalls.notifications()

    private companion object {
        const val CHATBOT_HISTORY = 100
    }
}
