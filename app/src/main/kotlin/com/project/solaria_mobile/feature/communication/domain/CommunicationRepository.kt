package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult
import kotlinx.coroutines.flow.Flow

interface CommunicationRepository {
    val online: Flow<Boolean>
    suspend fun conversations(): ApiResult<List<Conversation>>
    suspend fun conversation(id: String): ApiResult<Conversation>
    suspend fun messages(conversationId: String, afterSequence: Int? = null): ApiResult<List<ChatMessage>>
    fun liveMessages(conversationId: String): Flow<ChatMessage>
    fun pendingMessages(conversationId: String): Flow<List<ChatMessage>>
    fun reconnections(): Flow<Unit>
    suspend fun sendMessage(conversationId: String, text: String): ApiResult<Unit>
    suspend fun retryMessage(messageId: String)
    suspend fun discardMessage(messageId: String)
    suspend fun calls(): ApiResult<List<ServiceCall>>
    suspend fun call(id: String): ApiResult<ServiceCall>
    suspend fun notifications(): ApiResult<List<NotificationGroup>>
}
