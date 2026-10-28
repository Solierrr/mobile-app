package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.currentUserId
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ApplicationScope
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.database.PendingMessageDao
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * Fila de envio das mensagens do chat: grava no banco antes e envia por REST depois.
 *
 * Cada mensagem vai para a tabela `pending_messages` ([PendingMessageDao]) antes do POST. Assim, um envio feito
 * sem internet fica guardado e sai quando a conexão volta. Um único worker envia em ordem de gravação.
 *
 * Regras de reenvio:
 * - Falha temporária (sem rede, timeout, 5xx, 429 ou 409) com internet: nova tentativa com espera de 15 s, 30 s,
 *   60 s, 120 s e 240 s. Um gatilho (internet voltou, mensagem nova ou "Tentar de novo") antecipa a espera.
 * - 401: a fila para até o próximo gatilho.
 * - 400/422, 403 e 404: a mensagem vai para `failed`, sem travar as demais.
 * - Mensagem já tentada antes (`attempts > 0`): confere as últimas 100 mensagens da conversa antes de reenviar,
 *   porque o messenger não deduplica pelo `clientMessageId`.
 *
 * Depende de:
 * - [PendingMessageDao] -> fila local.
 * - [MessengerApi] -> envio e histórico.
 * - [ApiCallExecutor] -> executa a chamada e devolve o [ApiResult], com o [ErrorKind] na falha.
 * - [NetworkMonitor] -> avisa quando a internet volta.
 * - [CredentialProvider] -> `sub` do usuário logado.
 * - [ApplicationScope] -> escopo do worker.
 *
 * Principais operações:
 * - [start] -> liga o monitor de rede e o worker. Chamadas seguintes não têm efeito.
 * - [enqueue] -> grava a mensagem e pede o envio.
 * - [observe] -> acompanha as mensagens pendentes de uma conversa.
 * - [retry] -> libera uma mensagem recusada para novo envio.
 * - [discard] -> apaga uma mensagem da fila sem enviá-la.
 * - [delivered] -> avisa cada mensagem confirmada pelo servidor.
 */
@Singleton
class MessageOutbox @Inject constructor(
    private val dao: PendingMessageDao,
    private val api: MessengerApi,
    private val executor: ApiCallExecutor,
    private val networkMonitor: NetworkMonitor,
    private val credentials: CredentialProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _delivered = MutableSharedFlow<MessageDto>(extraBufferCapacity = DELIVERED_BUFFER)
    /**
     * Mensagens confirmadas pelo servidor. Cada uma é emitida antes de a linha sair da fila, para o chat trocar
     * a bolha pendente pela definitiva sem piscar.
     */
    val delivered: SharedFlow<MessageDto> = _delivered.asSharedFlow()

    private val drainRequests = Channel<Unit>(Channel.CONFLATED)
    private val started = AtomicBoolean(false)

    // Escrito pela coleta de rede e lido pelo worker, em corrotinas diferentes: por isso é @Volatile.
    @Volatile
    private var online = false

    /**
     * Liga o monitor de rede e o worker de envio. Só a primeira chamada tem efeito.
     *
     * Chamado na abertura do app.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                online = isOnline
                if (isOnline) drainRequests.trySend(Unit)
            }
        }
        scope.launch { runWorker() }
    }

    /**
     * Grava a mensagem na fila com o usuário logado como remetente e pede o envio.
     *
     * Sucesso quer dizer gravada, não entregue: o POST acontece depois, em segundo plano.
     *
     * @param conversationId conversa de destino.
     * @param messageType tipo exigido pelo servidor para a conversa.
     * @param content texto da mensagem.
     * @return [ApiResult.Success] quando gravada. Sem sessão, [ErrorKind.UNAUTHORIZED]; se o banco falhar, [ErrorKind.UNKNOWN].
     */
    suspend fun enqueue(conversationId: String, messageType: String, content: String): ApiResult<Unit> {
        val senderId = credentials.currentUserId() ?: return ApiResult.Failure(ErrorKind.UNAUTHORIZED)
        val message = PendingMessageEntity(
            clientMessageId = UUID.randomUUID().toString(),
            senderId = senderId,
            conversationId = conversationId,
            messageType = messageType,
            content = content,
            createdAt = System.currentTimeMillis(),
        )
        try {
            dao.insert(message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ApiResult.Failure(ErrorKind.UNKNOWN)
        }
        drainRequests.trySend(Unit)
        return ApiResult.Success(Unit)
    }

    /**
     * Acompanha as mensagens pendentes de uma conversa do usuário logado, em ordem de gravação.
     *
     * Sem sessão, emite uma lista vazia.
     */
    fun observe(conversationId: String): Flow<List<PendingMessageEntity>> {
        val senderId = credentials.currentUserId() ?: return flowOf(emptyList())
        return dao.observe(senderId, conversationId)
    }

    /**
     * Libera uma mensagem recusada (`failed = false`) e pede novo envio.
     *
     * @param clientMessageId id da mensagem na fila.
     */
    suspend fun retry(clientMessageId: String) {
        dao.setFailed(clientMessageId, failed = false)
        drainRequests.trySend(Unit)
    }

    /**
     * Apaga a mensagem da fila sem enviá-la.
     *
     * @param clientMessageId id da mensagem na fila.
     */
    suspend fun discard(clientMessageId: String) {
        dao.delete(clientMessageId)
    }

    // Único consumidor da fila: pedidos repetidos se fundem (CONFLATED) e nunca há dois envios ao mesmo tempo
    private suspend fun runWorker() {
        var failures = 0
        while (true) {
            if (failures == 0) {
                drainRequests.receive()
            } else {
                withTimeoutOrNull(retryDelayMillis(failures)) { drainRequests.receive() }
            }
            // Sem internet não há tentativa agendada: a fila espera o próximo gatilho, em geral a volta da conexão.
            if (!online) {
                failures = 0
                continue
            }
            failures = if (drainSafely() == Outcome.RETRY_LATER) failures + 1 else 0
        }
    }

    // Uma falha do banco não pode matar o worker, senão a fila para de andar até o app reiniciar
    private suspend fun drainSafely(): Outcome =
        try {
            drainOnce()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Outcome.RETRY_LATER
        }

    private suspend fun drainOnce(): Outcome {
        val senderId = credentials.currentUserId() ?: return Outcome.STOP
        for (message in dao.sendable(senderId)) {
            when (val outcome = deliver(message)) {
                Outcome.DELIVERED -> Unit
                Outcome.REJECTED -> dao.setFailed(message.clientMessageId, failed = true)
                Outcome.RETRY_LATER, Outcome.STOP -> return outcome
            }
        }
        return Outcome.DELIVERED
    }

    private suspend fun deliver(message: PendingMessageEntity): Outcome {
        // O messenger não deduplica: se uma tentativa anterior pode ter gravado, confere o histórico antes de repetir
        if (message.attempts > 0) {
            when (val found = findOnServer(message)) {
                is ApiResult.Failure -> return outcomeOf(found.kind)
                is ApiResult.Success -> found.data?.let { return confirm(message, it) }
            }
        }
        dao.countAttempt(message.clientMessageId)
        val request = SendMessageRequest(
            conversationId = message.conversationId,
            messageType = message.messageType,
            content = message.content,
            clientMessageId = message.clientMessageId,
        )
        return when (val result = executor.execute { api.send(request) }) {
            is ApiResult.Success -> confirm(message, result.data)
            is ApiResult.Failure -> outcomeOf(result.kind)
        }
    }

    private suspend fun findOnServer(message: PendingMessageEntity): ApiResult<MessageDto?> =
        executor.execute { api.messages(message.conversationId, limit = HISTORY_CHECK_LIMIT) }
            .map { recent -> recent.firstOrNull { it.clientMessageId.equals(message.clientMessageId, ignoreCase = true) } }

    private suspend fun confirm(message: PendingMessageEntity, sent: MessageDto): Outcome {
        _delivered.emit(sent)
        dao.delete(message.clientMessageId)
        return Outcome.DELIVERED
    }

    /**
     * Resultado de uma tentativa de envio, que decide o próximo passo do worker.
     *
     * - `DELIVERED` -> enviada e confirmada, ou nada mais a enviar.
     * - `REJECTED` -> recusada pelo servidor. A mensagem vai para `failed`.
     * - `RETRY_LATER` -> falha temporária. O worker repete com espera crescente.
     * - `STOP` -> 401. O worker para até o próximo gatilho.
     */
    private enum class Outcome { DELIVERED, REJECTED, RETRY_LATER, STOP }

    /**
     * Decide o que o worker faz com um erro: recusar a mensagem, parar até o próximo gatilho ou tentar de novo depois.
     */
    private fun outcomeOf(kind: ErrorKind): Outcome = when (kind) {
        ErrorKind.VALIDATION, ErrorKind.FORBIDDEN, ErrorKind.NOT_FOUND -> Outcome.REJECTED
        ErrorKind.UNAUTHORIZED -> Outcome.STOP
        else -> Outcome.RETRY_LATER
    }

    // 15 s, 30 s, 60 s, 120 s e 240 s
    private fun retryDelayMillis(failures: Int): Long =
        (RETRY_BASE_MILLIS shl (failures - 1).coerceAtMost(4)).coerceAtMost(RETRY_MAX_MILLIS)

    private companion object {
        /** Confirmações que podem aguardar no buffer de [delivered] sem bloquear o envio. */
        const val DELIVERED_BUFFER = 64
        /** Quantas mensagens recentes da conversa são conferidas antes de um reenvio. */
        const val HISTORY_CHECK_LIMIT = 100
        /** Primeira espera entre tentativas de envio: 15 s. */
        const val RETRY_BASE_MILLIS = 15_000L
        /** Teto da espera entre tentativas de envio: 240 s. */
        const val RETRY_MAX_MILLIS = 240_000L
    }
}
