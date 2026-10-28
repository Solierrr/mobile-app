package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.database.PendingMessageDao
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.MessengerApi
import com.project.solaria_mobile.feature.communication.data.SendMessageRequest
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Monta um JWT de teste com o `sub` informado. A assinatura é um texto fictício.
 */
fun tokenFor(subject: String): String {
    val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"$subject"}""".toByteArray())
    return "e30.$payload.signature"
}

/**
 * Dublê em memória de [PendingMessageDao], com as mesmas regras das consultas SQL.
 *
 * Filtra por remetente, deixa de fora as mensagens `failed` no envio e ordena o envio por `localId`.
 */
class InMemoryPendingMessageDao : PendingMessageDao {
    private val table = MutableStateFlow<List<PendingMessageEntity>>(emptyList())
    private var nextId = 1L

    val rows: List<PendingMessageEntity> get() = table.value

    override suspend fun insert(message: PendingMessageEntity) {
        table.update { it + message.copy(localId = nextId++) }
    }

    override fun observe(senderId: String, conversationId: String): Flow<List<PendingMessageEntity>> =
        table.map { rows -> rows.filter { it.senderId == senderId && it.conversationId == conversationId } }

    override suspend fun sendable(senderId: String): List<PendingMessageEntity> =
        table.value.filter { it.senderId == senderId && !it.failed }.sortedBy { it.localId }

    override suspend fun countAttempt(clientMessageId: String) = change(clientMessageId) { it.copy(attempts = it.attempts + 1) }

    override suspend fun setFailed(clientMessageId: String, failed: Boolean) = change(clientMessageId) { it.copy(failed = failed) }

    override suspend fun delete(clientMessageId: String) {
        table.update { rows -> rows.filterNot { it.clientMessageId == clientMessageId } }
    }

    private fun change(clientMessageId: String, transform: (PendingMessageEntity) -> PendingMessageEntity) {
        table.update { rows -> rows.map { if (it.clientMessageId == clientMessageId) transform(it) else it } }
    }
}

/** Messenger em memória: cada falha programada vale para um envio; `persistBeforeFailing` simula resposta perdida. */
class ScriptedMessengerApi : MessengerApi {
    val history = mutableListOf<MessageDto>()
    val sendRequests = mutableListOf<SendMessageRequest>()
    val sendFailures = ArrayDeque<Throwable>()
    val conversationTypes = mutableMapOf<String, String>()
    var persistBeforeFailing = false
    var historyCalls = 0
    var conversationFailure: Throwable? = null

    override suspend fun conversations(): List<ConversationDto> =
        conversationTypes.map { (id, type) -> ConversationDto(id, type) }

    override suspend fun conversation(id: String): ConversationDto {
        conversationFailure?.let { throw it }
        return ConversationDto(id, conversationTypes[id] ?: "DIRECT")
    }

    override suspend fun messages(conversationId: String, sinceSequence: Int?, limit: Int): List<MessageDto> {
        historyCalls++
        val matching = history.filter { it.conversationId == conversationId && it.sequence > (sinceSequence ?: 0) }
        // Como o messenger: sem cursor devolve as últimas, com cursor as primeiras depois dele
        return if (sinceSequence == null) matching.takeLast(limit) else matching.take(limit)
    }

    override suspend fun send(body: SendMessageRequest): MessageDto {
        sendRequests += body
        val failure = sendFailures.removeFirstOrNull()
        if (failure != null && !persistBeforeFailing) throw failure
        val stored = MessageDto(
            id = "m${history.size + 1}",
            conversationId = body.conversationId,
            senderId = "user-1",
            clientMessageId = body.clientMessageId,
            content = body.content,
            sequence = history.size + 1,
        )
        history += stored
        if (failure != null) throw failure
        return stored
    }
}

/**
 * Dublê de [RealtimeClient]. O teste publica pares (destino, corpo) em [frames]; o estado fica sempre `Disconnected`.
 *
 * `connect` e `disconnect` não fazem nada.
 */
class FakeRealtimeClient : RealtimeClient {
    val frames = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 16)
    override val state: StateFlow<RealtimeState> = MutableStateFlow(RealtimeState.Disconnected)

    override fun subscribe(destination: String): Flow<String> =
        frames.filter { it.first == destination }.map { it.second }

    override suspend fun send(destination: String, body: String): ApiResult<Unit> = ApiResult.Success(Unit)

    override fun connect() = Unit

    override fun disconnect() = Unit
}
