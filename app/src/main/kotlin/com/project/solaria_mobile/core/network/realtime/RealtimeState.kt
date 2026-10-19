package com.project.solaria_mobile.core.network.realtime

/**
 * Estado da conexão em tempo real (WebSocket + STOMP)
 */
sealed interface RealtimeState {

    /** Sem conexão e sem tentativa em andamento */
    data object Disconnected : RealtimeState

    /** sem URL/envs configurada */
    data object NotConfigured : RealtimeState

    /** Primeira tentativa de conexão em andamento */
    data object Connecting : RealtimeState

    /** Conectado */
    data object Connected : RealtimeState

    /**
     * Conexão perdida | tentatica falhou
     *
     * nova tentativa com espera crescente
     *
     * @property attempt número da tentativa de reconexão, começando em 1
     */
    data class Reconnecting(val attempt: Int) : RealtimeState

    /**
     * O servidor recusou a conexão, ou não há token
     *
     * **Sem tentativa automática**
     *
     * @property reason motivo da recusa
     */
    data class AuthFailed(val reason: AuthFailure) : RealtimeState
}

/** Motivos pelos quais o tempo real para de tentar sozinho */
enum class AuthFailure {
    /** Não há token disponível para enviar no `CONNECT` */
    NO_TOKEN,

    /** O servidor recusou o token */
    TOKEN_REJECTED,

    /** O usuário já tem o número máximo de sessões no messenger */
    SESSION_LIMIT_EXCEEDED,
}
