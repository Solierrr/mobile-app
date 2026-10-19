package com.project.solaria_mobile.core.network.realtime

import com.project.solaria_mobile.core.common.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * base da conexão em tempo real de api-messenger com o resto do app
 *
 * payload da request é entregue como JSON
 *
 * Principais operações:
 * - [state] -> estado da conexão
 * - [subscribe] -> mensagens recebidas em um destino
 * - [send] -> publica uma mensagem em um destino
 * - [connect] | [disconnect] -> abre e fecha a conexão
 */
interface RealtimeClient {

    /** Estado atual da conexão */
    val state: StateFlow<RealtimeState>

    /**
     * Recebe os corpos publicados em [destination]
     *
     * @param destination destino STOMP
     */
    fun subscribe(destination: String): Flow<String>

    /**
     * Publica [body] em [destination]
     *
     * @return sucesso, ou falha com `ErrorKind.CONNECTIVITY` se não houver conexão ativa
     */
    suspend fun send(destination: String, body: String): ApiResult<Unit>

    /**
     * Abre a conexão | Não faz nada se já houver uma
     */
    fun connect()

    /** Fecha a conexão | cancela as tentativas em andamento */
    fun disconnect()
}
