package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult

interface CommunicationRepository {
    suspend fun conversations(): ApiResult<List<Conversation>>
    suspend fun conversation(id: String): ApiResult<Conversation>
    suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>>
    suspend fun sendMessage(conversationId: String, text: String): ApiResult<ChatMessage>
    suspend fun calls(): ApiResult<List<ServiceCall>>
    suspend fun call(id: String): ApiResult<ServiceCall>
    suspend fun notifications(): ApiResult<List<NotificationGroup>>
}
