package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.communication.domain.AssistantReply
import com.project.solaria_mobile.feature.communication.domain.AssistantRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAssistantRepository @Inject constructor(
    private val api: AssistantApi,
    private val executor: ApiCallExecutor,
) : AssistantRepository {
    override suspend fun send(threadId: String, text: String): ApiResult<AssistantReply> =
        executor.execute { api.chat(AssistantChatRequest(threadId, text)) }
            .map { AssistantReply(it.response, it.outcome, it.messengerConversationId) }
}
