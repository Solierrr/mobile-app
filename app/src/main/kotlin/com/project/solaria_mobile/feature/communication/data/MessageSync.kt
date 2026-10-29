package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.auth.accessTokenUserId
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ApplicationScope
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.database.CHATBOT_TYPE
import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.LocalDataWiper
import com.project.solaria_mobile.core.database.MessageDao
import com.project.solaria_mobile.core.database.MessageStats
import com.project.solaria_mobile.core.database.SyncStateDao
import com.project.solaria_mobile.core.database.SyncStateEntity
import com.project.solaria_mobile.core.database.hasHole
import com.project.solaria_mobile.core.database.isChatbot
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/**
 * Motor que mantém o banco igual ao servidor. O chat e a lista só leem o Room, e esta classe é quem escreve nele.
 *
 * Roda enquanto há usuário logado. Quando a sessão acaba, para tudo e apaga os dados locais. A sincronização segue o
 * fluxo da especificação do histórico offline:
 * 1. Assina `/topic/users.{me}.events` antes de qualquer catch-up. Cada evento `MESSAGE_*`, `CONVERSATION_UPDATED` e
 *    `CONVERSATION_REMOVED` vai direto para o banco.
 * 2. Lê `GET /messaging/conversations/me`, arquivadas ou não, e marca como sem acesso (`accessLost`) as conversas
 *    guardadas que sumiram da lista.
 * 3. Sem cursor guardado, lê `GET /messaging/updates` sem `since` para guardar a época e o cursor. Com cursor, lê
 *    `GET /messaging/updates?since=` enquanto houver mais páginas. Época diferente apaga as mensagens e o cursor e
 *    recomeça do passo anterior. A fila de envio fica.
 * 4. Completa cada conversa humana: preenche buracos de `sequence`, traz o que veio depois da última mensagem e baixa
 *    o histórico para trás até a `sequence` 1.
 *
 * O cursor só anda pelo catch-up, nunca por evento ao vivo. Um evento perdido numa queda volta no próximo catch-up, e
 * reaplicar o que já chegou ao vivo não muda nada, porque todo caminho grava pela regra de versão maior vence.
 *
 * Uma sincronização completa roda quando o app abre, quando o WebSocket conecta, quando a internet volta e quando
 * [refresh] é chamado. Pedidos repetidos se fundem e nunca há duas sincronizações ao mesmo tempo. Se uma falhar com
 * internet, repete depois de 30 s. Conversas com o chatbot não têm mensagens no banco.
 *
 * Depende de:
 * - [MessengerApi] -> lista de conversas, histórico e catch-up.
 * - [ApiCallExecutor] -> executa as chamadas e devolve o [ApiResult], com o [ErrorKind] na falha.
 * - [TokenStore] -> diz quando o usuário entra e sai.
 * - [RealtimeClient] -> entrega os eventos do tópico do usuário e avisa quando o WebSocket conecta.
 * - [NetworkMonitor] -> avisa quando a internet volta.
 * - [MessageStore] -> grava as mensagens pela regra de versão.
 * - [ConversationDao], [MessageDao] e [SyncStateDao] -> tabelas lidas e escritas.
 * - [UserDirectory] -> nomes das conversas diretas.
 * - [LocalDataWiper] -> apaga os dados locais no logout.
 * - [ApplicationScope] -> escopo do motor.
 *
 * Principais operações:
 * - [start] -> liga o motor. Chamadas seguintes não têm efeito.
 * - [refresh] -> pede uma sincronização completa agora.
 * - [result] -> resultado da última sincronização, para as telas saberem se uma lista vazia é vazia mesmo.
 */
@Singleton
class MessageSync @Inject constructor(
    private val api: MessengerApi,
    private val executor: ApiCallExecutor,
    private val tokens: TokenStore,
    private val realtime: RealtimeClient,
    private val networkMonitor: NetworkMonitor,
    private val json: Json,
    private val store: MessageStore,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val syncState: SyncStateDao,
    private val users: UserDirectory,
    private val wiper: LocalDataWiper,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _result = MutableStateFlow<ApiResult<Unit>?>(null)

    /**
     * Resultado da última sincronização completa, ou `null` enquanto não houve nenhuma desde o início do app, o
     * logout ou o último [refresh]. Sem internet, vira falha de conectividade.
     */
    val result: StateFlow<ApiResult<Unit>?> = _result.asStateFlow()

    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val started = AtomicBoolean(false)
    private val repairLocks = ConcurrentHashMap<String, Mutex>()

    /**
     * Liga o motor. Só a primeira chamada tem efeito.
     *
     * Chamado na abertura do app. A cada mudança de usuário logado, para a sessão anterior e começa a nova. Sem
     * usuário, ou ao trocar direto de uma conta para outra, apaga os dados locais, inclusive a fila de envio e as
     * mensagens do chatbot em memória.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            var previous: String? = null
            tokens.tokens
                .map { it?.accessToken?.let(::accessTokenUserId) }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    // Um novo login sobre outra conta, sem passar pelo logout, também não pode herdar os dados antigos
                    val switched = userId != null && previous != null && previous != userId
                    previous = userId
                    if (userId == null || switched) {
                        withContext(NonCancellable) {
                            store.forget()
                            users.forget()
                            wiper.wipe()
                        }
                        _result.value = null
                    }
                    if (userId != null) runSession(userId)
                }
        }
    }

    /**
     * Pede uma sincronização completa. Serve para o botão "Tentar novamente" da lista.
     *
     * Zera o [result] até a sincronização terminar.
     */
    fun refresh() {
        _result.value = null
        requests.trySend(Unit)
    }

    // Vive enquanto o mesmo usuário está logado. Os eventos entram numa corrotina própria e a fila de pedidos alimenta o
    // único worker. O monitor de rede emite o estado atual ao começar, e é isso que dispara a primeira sincronização;
    // mudar de estado dispara outra, e sem internet o worker só registra a falha de conectividade.
    private suspend fun runSession(userId: String): Unit = coroutineScope {
        launch { realtime.subscribe(eventsTopic(userId)).collect { handleFrame(this@coroutineScope, it) } }
        launch { realtime.state.filter { it == RealtimeState.Connected }.collect { requests.trySend(Unit) } }
        launch { networkMonitor.isOnline.collect { requests.trySend(Unit) } }
        runWorker(userId)
    }

    // Único consumidor da fila: pedidos repetidos se fundem (CONFLATED) e nunca há duas sincronizações ao mesmo tempo
    private suspend fun runWorker(userId: String) {
        var failures = 0
        while (true) {
            if (failures == 0) requests.receive() else withTimeoutOrNull(RETRY_MILLIS) { requests.receive() }
            // Sem internet não há tentativa agendada: a sincronização espera o próximo gatilho, em geral a volta da conexão
            if (!networkMonitor.isOnline.first()) {
                _result.value = ApiResult.Failure(ErrorKind.CONNECTIVITY)
                failures = 0
                continue
            }
            val outcome = syncSafely(userId)
            _result.value = outcome
            failures = if (outcome is ApiResult.Failure) failures + 1 else 0
        }
    }

    // Uma falha do banco não pode matar o worker, senão a sincronização para até o app reiniciar
    private suspend fun syncSafely(userId: String): ApiResult<Unit> =
        try {
            syncOnce(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Failure(ErrorKind.UNKNOWN)
        }

    private suspend fun syncOnce(userId: String): ApiResult<Unit> {
        val listed = refreshConversations(userId)
        if (listed is ApiResult.Failure) return listed
        val caughtUp = catchUp()
        if (caughtUp is ApiResult.Failure) return caughtUp
        return downloadHistories()
    }

    // Duas leituras porque a lista padrão do servidor esconde as arquivadas, e uma arquivada não é uma conversa perdida
    private suspend fun refreshConversations(userId: String): ApiResult<Unit> {
        val active = executor.execute { api.conversations(archived = false) }.orFail { return it }
        val archived = executor.execute { api.conversations(archived = true) }.orFail { return it }
        conversations.applyList((active + archived).map { it.toEntity() })
        messages.purgeChatbots()
        users.refreshFor(null, userId)
        return ApiResult.Success(Unit)
    }

    private suspend fun catchUp(): ApiResult<Unit> {
        var state = syncState.get() ?: return bootstrap()
        while (true) {
            val page = executor.execute { api.updates(since = state.cursor, limit = PAGE_SIZE) }.orFail { return it }
            if (page.syncEpoch != state.syncEpoch) {
                // O relógio do servidor recomeçou: as versões guardadas não se comparam mais. A fila de envio fica.
                messages.clear()
                syncState.clear()
                return bootstrap()
            }
            // Aplica antes de avançar o cursor: se o app morrer no meio, a página é reaplicada sem efeito
            store.apply(page.messages)
            state = SyncStateEntity(syncEpoch = state.syncEpoch, cursor = page.cursor)
            syncState.put(state)
            if (!page.hasMore) return ApiResult.Success(Unit)
        }
    }

    // O cursor é lido antes de baixar os históricos: o que mudar durante o download volta no próximo catch-up
    private suspend fun bootstrap(): ApiResult<Unit> {
        val start = executor.execute { api.updates() }.orFail { return it }
        syncState.put(SyncStateEntity(syncEpoch = start.syncEpoch, cursor = start.cursor))
        return ApiResult.Success(Unit)
    }

    // As conversas mais recentes primeiro, para a lista que o usuário vê se completar antes
    private suspend fun downloadHistories(): ApiResult<Unit> {
        for (conversation in conversations.all().sortedByDescending { it.lastInteractionAt ?: 0L }) {
            val result = repair(conversation.id)
            if (result is ApiResult.Failure && result.kind !in SKIPPED_KINDS) return result
        }
        return ApiResult.Success(Unit)
    }

    // Repete o pedido que [nextHistoryStep] indica até a conversa ficar completa. Para também quando uma página não
    // muda o resumo do banco, por exemplo um buraco que o servidor nunca preencheu. Uma trava por conversa serializa o
    // reparo da sincronização e os dos eventos, e um evento que chega durante um reparo espera a vez e confere de novo.
    private suspend fun repair(conversationId: String): ApiResult<Unit> =
        repairLocks.getOrPut(conversationId) { Mutex() }.withLock { repairLocked(conversationId) }

    private suspend fun repairLocked(conversationId: String): ApiResult<Unit> {
        var previous: MessageStats? = null
        while (true) {
            val conversation = conversations.find(conversationId)
            if (conversation == null || conversation.isChatbot || conversation.accessLost) return ApiResult.Success(Unit)
            val stats = messages.stats(conversationId)
            if (stats == previous) return ApiResult.Success(Unit)
            previous = stats
            val hole = if (stats.hasHole) messages.holeAfter(conversationId) else null
            val page = when (val step = nextHistoryStep(stats, hole, conversation.lastMessageSequence)) {
                HistoryStep.Done -> return ApiResult.Success(Unit)
                HistoryStep.Latest -> executor.execute { api.messages(conversationId, limit = PAGE_SIZE) }
                is HistoryStep.Since -> executor.execute { api.messages(conversationId, sinceSequence = step.sequence, limit = PAGE_SIZE) }
                is HistoryStep.Before -> executor.execute { api.messages(conversationId, beforeSequence = step.sequence, limit = PAGE_SIZE) }
            }.orFail { return it }
            store.apply(page)
        }
    }

    private suspend fun handleFrame(session: CoroutineScope, body: String) {
        val event = runCatching { json.decodeFromString(RealtimeEventDto.serializer(), body) }.getOrNull() ?: return
        val payload = event.payload ?: return
        try {
            when (event.type) {
                in MESSAGE_EVENTS -> {
                    val message = json.decodeFromJsonElement(MessageDto.serializer(), payload)
                    store.apply(listOf(message))
                    repairInBackground(session, message.conversationId)
                }
                CONVERSATION_UPDATED -> {
                    val item = json.decodeFromJsonElement(ConversationDto.serializer(), payload)
                    conversations.upsert(listOf(item.toEntity()))
                    // Mensagens do chatbot que chegaram antes da conversa foram para o banco sem o tipo saber
                    if (item.conversationType == CHATBOT_TYPE) messages.purgeChatbots()
                    repairInBackground(session, item.id)
                }
                CONVERSATION_REMOVED -> {
                    val removed = json.decodeFromJsonElement(ConversationRemovedDto.serializer(), payload)
                    conversations.remove(removed.id, removed.syncEpoch, removed.syncVersion)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Frame malformado ou falha do banco: o cursor não andou, então o próximo catch-up traz o que faltou
        }
    }

    // Um evento fora de ordem deixa buraco, e uma conversa nova ou antiga não tem histórico. Busca só o que falta, sem
    // travar a leitura dos próximos eventos.
    private fun repairInBackground(session: CoroutineScope, conversationId: String) {
        session.launch {
            try {
                repair(conversationId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // O próximo catch-up refaz
            }
        }
    }

    // Devolve o valor de um sucesso, ou deixa o chamador sair com a falha
    private inline fun <T> ApiResult<T>.orFail(onFailure: (ApiResult.Failure) -> Nothing): T =
        when (this) {
            is ApiResult.Success -> data
            is ApiResult.Failure -> onFailure(this)
        }

    private companion object {
        /** Mensagens por página, o máximo que o servidor aceita. */
        const val PAGE_SIZE = 500
        /** Espera antes de repetir uma sincronização que falhou com internet: 30 s. */
        const val RETRY_MILLIS = 30_000L
        val MESSAGE_EVENTS = setOf("MESSAGE_CREATED", "MESSAGE_EDITED", "MESSAGE_DELETED")
        const val CONVERSATION_UPDATED = "CONVERSATION_UPDATED"
        const val CONVERSATION_REMOVED = "CONVERSATION_REMOVED"
        /** Erros que só afetam uma conversa, como a perda de acesso no meio da sincronização. */
        val SKIPPED_KINDS = setOf(ErrorKind.FORBIDDEN, ErrorKind.NOT_FOUND)
    }
}

/**
 * Destino STOMP dos eventos privados do usuário.
 *
 * @param userId `sub` do JWT em minúsculas, o mesmo usado no tópico `.errors`.
 */
internal fun eventsTopic(userId: String): String = "/topic/users.$userId.events"
