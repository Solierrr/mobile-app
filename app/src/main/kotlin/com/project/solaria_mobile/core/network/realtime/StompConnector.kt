package com.project.solaria_mobile.core.network.realtime

import kotlinx.coroutines.flow.Flow

/**
 * Abre uma sessão STOMP com o servidor
 */
interface StompConnector {

    /**
     * Conecta e conclui o STOMP, enviando [token] no header `Authorization` do `CONNECT`
     *
     * @param url endereço do WebSocket (`wss://.../messaging/ws`)
     * @param token token de acesso, sem o prefixo `Bearer`
     * @return a sessão aberta
     * @throws StompAuthException se o servidor recusar por autenticação.
     */
    suspend fun connect(url: String, token: String): StompLink
}

/**
 *  Uma sessão STOMP aberta
 */
interface StompLink {

    /** bodys recebidos em [destination] | Termina quando a sessão é perdida */
    fun subscribe(destination: String): Flow<String>

    /** Publica [body] em [destination] */
    suspend fun send(destination: String, body: String)

    /** Encerra a sessão */
    suspend fun close()

    /**
     * Suspende até a sessão ser perdida
     *
     * @return o motivo se a perda foi uma recusa de autenticação | `null` para qualquer outra perda
     */
    suspend fun awaitLoss(): AuthFailure?
}

/**
 * O servidor recusou a conexão por autenticação (token inválido/expirado ou limite de sessões)
 *
 * @property reason motivo da recusa
 */
class StompAuthException(
    val reason: AuthFailure,
    cause: Throwable? = null,
) : Exception("Conexão em tempo real recusada: $reason", cause)
