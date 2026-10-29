package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult
import kotlinx.coroutines.flow.Flow

interface CommunicationRepository {
    val online: Flow<Boolean>
    val syncResult: Flow<ApiResult<Unit>?>
    fun conversations(archived: Boolean = false): Flow<List<Conversation>>
    fun conversation(id: String): Flow<Conversation?>
    fun messages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun open(conversationId: String): ApiResult<Unit>
    fun refresh()
    suspend fun refreshNames(conversationId: String? = null)
    suspend fun sendMessage(conversationId: String, text: String): ApiResult<Unit>
    suspend fun retryMessage(messageId: String)
    suspend fun discardMessage(messageId: String)
    suspend fun markRead(conversationId: String)
    suspend fun setMuted(conversationId: String, muted: Boolean): ApiResult<Unit>
    suspend fun setArchived(conversationId: String, archived: Boolean): ApiResult<Unit>
    suspend fun startDirect(recipientAuthId: String): ApiResult<String>
    fun presence(conversationId: String): Flow<ChatPresence>
    fun typing(conversationId: String)
    fun stopTyping(conversationId: String)
    suspend fun editMessage(messageId: String, text: String): ApiResult<Unit>
    suspend fun deleteMessage(messageId: String): ApiResult<Unit>
    suspend fun calls(): ApiResult<List<ServiceCall>>
    suspend fun call(id: String): ApiResult<ServiceCall>
    suspend fun notifications(): ApiResult<List<NotificationGroup>>
}
