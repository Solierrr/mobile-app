package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.network.ApiCallExecutor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recupera o que mudou nas mensagens da conta enquanto o tempo real esteve fora, em todas as conversas de uma vez.
 */
interface MessageRecovery {
    /**
     * Entrega, em ordem, as mensagens que mudaram desde a última recuperação. Na primeira chamada só marca o ponto de partida.
     * Chamar com a assinatura do canal da conta já ativa: o que mudar durante a chamada chega pelo canal.
     */
    suspend fun recover(deliver: suspend (ChatEvent) -> Unit)
}

/** Sem recuperação: o chat só vê o que chega ao vivo ou o que o REST devolve ao abrir. */
object NoMessageRecovery : MessageRecovery {
    override suspend fun recover(deliver: suspend (ChatEvent) -> Unit) = Unit
}

/**
 * Recuperação por `GET /messaging/updates`. O cursor fica na memória do processo: sem cache de mensagens no aparelho não há o que
 * aplicar a um cursor de uma sessão anterior, e a tela já lê o REST ao abrir. Só a recuperação move o cursor; o `changeVersion` de um
 * evento ao vivo não o move, porque entre instâncias uma mensagem de versão menor pode ficar visível depois de uma de versão maior.
 * Repetir mensagens já conhecidas não faz mal: quem recebe troca pelo identificador.
 */
@Singleton
class UpdatesMessageRecovery internal constructor(
    private val fetch: suspend (since: Long?) -> ApiResult<MessageUpdatesDto>,
) : MessageRecovery {
    @Inject constructor(api: MessengerApi, executor: ApiCallExecutor) : this({ since -> executor.execute { api.updates(since) } })

    private var epoch: String? = null
    private var cursor: Long? = null

    override suspend fun recover(deliver: suspend (ChatEvent) -> Unit) {
        var since = cursor
        if (since == null) {
            baseline()
            return
        }
        while (true) {
            // Falhou: o cursor fica onde estava, e a próxima conexão tenta de novo.
            val page = (fetch(since) as? ApiResult.Success)?.data ?: return
            if (page.syncEpoch != epoch) {
                // O servidor recomeçou a contagem: as versões guardadas não valem mais e o que mudou não dá para saber por elas.
                deliver(ChatEvent.Resynced)
                baseline()
                return
            }
            page.messages.forEach { deliver(ChatEvent.MessageRecovered(it)) }
            cursor = page.cursor
            since = page.cursor
            if (!page.hasMore) return
        }
    }

    /** Sem `since` o servidor só diz onde está agora. */
    private suspend fun baseline() {
        val start = (fetch(null) as? ApiResult.Success)?.data
        epoch = start?.syncEpoch
        cursor = start?.cursor
    }
}
