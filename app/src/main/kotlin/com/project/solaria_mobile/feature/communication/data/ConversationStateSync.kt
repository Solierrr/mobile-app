package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ApplicationScope
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.ConversationEntity
import com.project.solaria_mobile.core.database.MessageDao
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.NetworkMonitor
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Estado pessoal das conversas: leitura, silenciar e arquivar.
 *
 * A leitura vale offline. [markRead] grava no Room até onde o usuário viu, e o servidor só é avisado quando há internet.
 * Não existe tabela de fila: o que falta enviar é a diferença entre `localReadSequence` e `lastReadSequence` da própria
 * linha da conversa. Silenciar e arquivar, ao contrário, exigem internet, porque uma preferência aplicada só no aparelho
 * ficaria diferente da dos outros dispositivos.
 *
 * Regras de envio da leitura:
 * - O servidor só avança o cursor, então reenviar o mesmo valor é seguro.
 * - 403 e 404 (acesso perdido) e 400/422 (valor acima do que o servidor tem, ou chatbot): a leitura pendente é
 *   descartada, igualando a local à do servidor, para a conversa não ser reenviada para sempre.
 * - Sem internet, timeout ou erro do servidor: a leitura fica pendente e uma nova tentativa sai em 30 s, ou antes se a
 *   internet voltar ou o usuário ler outra mensagem.
 *
 * Depende de:
 * - [MessengerApi] -> `PATCH /messaging/conversations/{id}/state`.
 * - [ApiCallExecutor] -> executa a chamada e devolve o [ApiResult].
 * - [NetworkMonitor] -> avisa quando a internet volta.
 * - [ConversationDao] -> guarda a leitura local e o estado devolvido pelo servidor.
 * - [MessageDao] -> diz a maior `sequence` que o aparelho tem da conversa.
 * - [ApplicationScope] -> escopo do worker.
 *
 * Principais operações:
 * - [start] -> liga o monitor de rede e o worker. Chamadas seguintes não têm efeito.
 * - [markRead] -> marca a conversa como lida até a última mensagem guardada.
 * - [setMuted] -> silencia ou reativa a conversa.
 * - [setArchived] -> arquiva ou desarquiva a conversa.
 */
@Singleton
class ConversationStateSync @Inject constructor(
    private val api: MessengerApi,
    private val executor: ApiCallExecutor,
    private val networkMonitor: NetworkMonitor,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val started = AtomicBoolean(false)

    /**
     * Liga o monitor de rede e o worker de envio da leitura. Só a primeira chamada tem efeito.
     *
     * Chamado na abertura do app. A internet voltando também dispara o envio, o que cobre a leitura feita offline.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch { networkMonitor.isOnline.collect { if (it) requests.trySend(Unit) } }
        scope.launch { runWorker() }
    }

    /**
     * Marca a conversa como lida até a última mensagem que o aparelho tem dela e pede o envio.
     *
     * Funciona offline. Conversa sem mensagens guardadas, como a do chatbot, não muda nada.
     *
     * @param conversationId conversa aberta na tela.
     */
    suspend fun markRead(conversationId: String) {
        val last = messages.stats(conversationId).maxSequence ?: return
        conversations.markReadLocally(conversationId, last)
        requests.trySend(Unit)
    }

    /**
     * Silencia ou reativa a conversa. Exige internet.
     *
     * @param conversationId conversa a alterar.
     * @param muted `true` para silenciar.
     */
    suspend fun setMuted(conversationId: String, muted: Boolean): ApiResult<Unit> =
        patch(conversationId, ConversationStateRequest(muted = muted))

    /**
     * Arquiva ou desarquiva a conversa. Exige internet.
     *
     * @param conversationId conversa a alterar.
     * @param archived `true` para arquivar.
     */
    suspend fun setArchived(conversationId: String, archived: Boolean): ApiResult<Unit> =
        patch(conversationId, ConversationStateRequest(archived = archived))

    private suspend fun patch(conversationId: String, body: ConversationStateRequest): ApiResult<Unit> {
        if (!networkMonitor.isOnline.first()) return ApiResult.Failure(ErrorKind.CONNECTIVITY)
        return executor.execute { api.patchState(conversationId, body) }.map { state -> store(state) }
    }

    private suspend fun store(state: ConversationStateDto) {
        conversations.applyState(state.conversationId, state.lastReadSequence, state.unreadCount, state.muted, state.archived)
    }

    private suspend fun runWorker() {
        var failed = false
        while (true) {
            // Depois de uma falha, tenta de novo em 30 s mesmo sem gatilho novo
            if (failed) withTimeoutOrNull(RETRY_MILLIS) { requests.receive() } else requests.receive()
            if (!networkMonitor.isOnline.first()) {
                failed = false
                continue
            }
            failed = !flush()
        }
    }

    // Devolve false se alguma leitura ficou pendente por falha que vale tentar de novo
    private suspend fun flush(): Boolean {
        for (conversation in conversations.pendingReads()) {
            val body = ConversationStateRequest(lastReadSequence = conversation.localReadSequence)
            when (val result = executor.execute { api.patchState(conversation.id, body) }) {
                is ApiResult.Success -> store(result.data)
                is ApiResult.Failure -> if (result.kind in DROPPED_KINDS) drop(conversation) else return false
            }
        }
        return true
    }

    // A conversa deixa de ter leitura pendente: a local volta a ser igual à do servidor
    private suspend fun drop(conversation: ConversationEntity) {
        conversations.setLocalRead(conversation.id, conversation.lastReadSequence)
    }

    private companion object {
        /** Espera antes de repetir um envio que falhou com internet: 30 s. */
        const val RETRY_MILLIS = 30_000L
        val DROPPED_KINDS = setOf(ErrorKind.FORBIDDEN, ErrorKind.NOT_FOUND, ErrorKind.VALIDATION)
    }
}
