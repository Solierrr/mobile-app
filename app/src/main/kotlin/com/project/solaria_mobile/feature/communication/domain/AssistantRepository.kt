package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult

/** Resposta de um turno do assistente; [messengerConversationId] aponta a conversa onde o ai-assistant gravou as duas pontas. */
data class AssistantReply(val text: String, val outcome: String, val messengerConversationId: String?)

interface AssistantRepository {
    /** Envia [text] à sessão [threadId]; o ai-assistant grava a mensagem e a resposta no api-messenger. */
    suspend fun send(threadId: String, text: String): ApiResult<AssistantReply>
}

/** Prefixo do identificador de uma sessão que ainda não tem conversa no messenger; o resto é o identificador da sessão. */
const val NEW_SESSION_PREFIX = "assistant-new:"
