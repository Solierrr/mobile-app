package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.currentUserId
import com.project.solaria_mobile.core.common.ApplicationScope
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class TypingCommand(val conversationId: String)

@Serializable
internal data class PresenceSnapshotDto(
    val presenceEpoch: String = "",
    val presenceVersion: Long = 0,
    val users: Map<String, Boolean> = emptyMap(),
)

@Serializable
internal data class PresenceChangeDto(
    val userId: String,
    val presenceEpoch: String = "",
    val presenceVersion: Long = 0,
)

@Serializable
internal data class TypingEventDto(val userId: String)

/**
 * O que o servidor disse da conversa ao vivo, em ids de usuário.
 *
 * @property known `false` sem conexão em tempo real ou antes do retrato inicial da presença.
 * @property online `authId` em minúsculas de quem está online, sem o usuário logado.
 * @property typing `authId` em minúsculas de quem está digitando, sem o usuário logado.
 */
data class LiveState(
    val known: Boolean = false,
    val online: Set<String> = emptySet(),
    val typing: Set<String> = emptySet(),
)

/**
 * Estado interno do acompanhamento: o [LiveState] mais a versão da presença, que ordena os eventos.
 *
 * @property epoch época da presença do último evento aceito.
 * @property version versão da presença do último evento aceito. Dentro da mesma época, versão menor é ignorada.
 */
internal data class PresenceState(
    val epoch: String = "",
    val version: Long = -1,
    val live: LiveState = LiveState(),
)

internal const val EVENT_PRESENCE_SNAPSHOT = "PRESENCE_SNAPSHOT"
internal const val EVENT_USER_ONLINE = "USER_ONLINE"
internal const val EVENT_USER_OFFLINE = "USER_OFFLINE"
internal const val EVENT_TYPING_STARTED = "TYPING_STARTED"
internal const val EVENT_TYPING_STOPPED = "TYPING_STOPPED"

/**
 * Aplica um evento do tópico `/topic/conversations.{id}` ao estado.
 *
 * - `PRESENCE_SNAPSHOT` é o retrato completo e sempre vale: o servidor o manda de novo a cada assinatura.
 * - `USER_ONLINE` e `USER_OFFLINE` só valem em época diferente da guardada ou com versão maior. Antes do retrato
 *   inicial são ignorados, porque não há base para aplicá-los.
 * - `TYPING_*` não têm versão. O usuário logado nunca entra nos conjuntos.
 *
 * @param event envelope do evento.
 * @param json conversor dos payloads.
 * @param me `sub` do usuário logado em minúsculas, ou `null` sem sessão.
 * @return o novo estado, que é o mesmo objeto se o evento não muda nada.
 */
internal fun PresenceState.reduce(event: RealtimeEventDto, json: Json, me: String?): PresenceState {
    val payload = event.payload ?: return this
    return try {
        when (event.type) {
            EVENT_PRESENCE_SNAPSHOT -> {
                val snapshot = json.decodeFromJsonElement(PresenceSnapshotDto.serializer(), payload)
                val online = snapshot.users.filterValues { it }.keys.map { it.lowercase() }.filterNot { it == me }.toSet()
                PresenceState(snapshot.presenceEpoch, snapshot.presenceVersion, live.copy(known = true, online = online))
            }
            EVENT_USER_ONLINE, EVENT_USER_OFFLINE -> {
                val change = json.decodeFromJsonElement(PresenceChangeDto.serializer(), payload)
                val user = change.userId.lowercase()
                val newer = change.presenceEpoch != epoch || change.presenceVersion > version
                if (!live.known || !newer || user == me) {
                    this
                } else {
                    val online = if (event.type == EVENT_USER_ONLINE) live.online + user else live.online - user
                    PresenceState(change.presenceEpoch, change.presenceVersion, live.copy(online = online))
                }
            }
            EVENT_TYPING_STARTED -> typing(json.decodeFromJsonElement(TypingEventDto.serializer(), payload), me, started = true)
            EVENT_TYPING_STOPPED -> typing(json.decodeFromJsonElement(TypingEventDto.serializer(), payload), me, started = false)
            else -> this
        }
    } catch (e: kotlinx.serialization.SerializationException) {
        this
    } catch (e: IllegalArgumentException) {
        this
    }
}

private fun PresenceState.typing(event: TypingEventDto, me: String?, started: Boolean): PresenceState {
    val user = event.userId.lowercase()
    if (user == me) return this
    val typing = if (started) live.typing + user else live.typing - user
    return copy(live = live.copy(typing = typing))
}

/** Tira [userId] de quem está digitando. Chamado quando o prazo local de digitação acaba. */
internal fun PresenceState.withoutTyping(userId: String): PresenceState =
    if (userId in live.typing) copy(live = live.copy(typing = live.typing - userId)) else this

/**
 * Presença, "digitando" e envio do "digitando" de uma conversa, pelo WebSocket.
 *
 * **Receber.** [observe] assina `/topic/conversations.{id}`. O servidor manda um retrato da presença logo depois da
 * assinatura e depois `USER_ONLINE`/`USER_OFFLINE`. O "digitando" chega como `TYPING_STARTED` e `TYPING_STOPPED`, mas o
 * servidor deixa o estado expirar em 5 s sem avisar, então cada `TYPING_STARTED` vale por 6 s aqui e some sozinho. Sem
 * conexão em tempo real o estado volta a "desconhecido".
 *
 * **Enviar.** [typing] manda `/app/typing.started` no máximo a cada 3 s (o servidor descarta repetições em 2 s e
 * esquece em 5 s) e agenda um `/app/typing.stopped` para 4 s sem digitação. [stopTyping] manda o `stopped` na hora.
 * Falha de envio sem conexão é ignorada: o "digitando" é descartável.
 *
 * Não envia `/app/presence.ping`: o servidor o ignora, e a presença depende da sessão, que o heartbeat do STOMP mantém.
 *
 * Depende de:
 * - [RealtimeClient] -> assinatura e envio STOMP.
 * - [Json] -> conversão dos payloads.
 * - [CredentialProvider] -> `sub` do usuário logado, para ele não aparecer como "digitando" nem como online.
 * - [ApplicationScope] -> escopo dos envios.
 *
 * Principais operações:
 * - [observe] -> acompanha a presença e o "digitando" de uma conversa.
 * - [typing] -> avisa que o usuário está digitando.
 * - [stopTyping] -> avisa que o usuário parou de digitar.
 */
@Singleton
class ConversationPresence @Inject constructor(
    private val realtime: RealtimeClient,
    private val json: Json,
    private val credentials: CredentialProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val startedAt = ConcurrentHashMap<String, Long>()
    private val stopJobs = ConcurrentHashMap<String, Job>()

    // Relógio da regra dos 3 s. Só os testes trocam.
    internal var now: () -> Long = System::currentTimeMillis

    /**
     * Acompanha a presença e o "digitando" da conversa.
     *
     * Emite [LiveState] desconhecido enquanto o WebSocket não está conectado. A cada reconexão assina de novo e recebe
     * outro retrato.
     *
     * @param conversationId conversa aberta na tela.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(conversationId: String): Flow<LiveState> {
        val me = credentials.currentUserId()
        return realtime.state
            .map { it == RealtimeState.Connected }
            .distinctUntilChanged()
            .flatMapLatest { connected ->
                if (connected) liveOf(conversationId, me) else flowOf(LiveState())
            }
    }

    private fun liveOf(conversationId: String, me: String?): Flow<LiveState> = channelFlow {
        val lock = Mutex()
        var state = PresenceState()
        val expiry = HashMap<String, Job>()

        suspend fun update(change: (PresenceState) -> PresenceState) = lock.withLock {
            val next = change(state)
            if (next != state) {
                state = next
                send(next.live)
            }
        }

        send(state.live)
        realtime.subscribe(conversationTopic(conversationId)).collect { body ->
            val event = runCatching { json.decodeFromString(RealtimeEventDto.serializer(), body) }.getOrNull() ?: return@collect
            update { it.reduce(event, json, me) }
            if (event.type == EVENT_TYPING_STARTED) {
                val user = typingUser(event.payload) ?: return@collect
                expiry.remove(user)?.cancel()
                expiry[user] = launch {
                    delay(TYPING_EXPIRY_MILLIS)
                    update { it.withoutTyping(user) }
                }
            }
        }
    }

    private fun typingUser(payload: JsonElement?): String? =
        payload?.let { runCatching { json.decodeFromJsonElement(TypingEventDto.serializer(), it).userId.lowercase() }.getOrNull() }

    /**
     * Avisa que o usuário está digitando na conversa.
     *
     * Chamado a cada alteração do texto. Envia o `started` na primeira vez e depois só a cada 3 s, e reinicia o prazo de
     * 4 s para o `stopped` automático.
     *
     * @param conversationId conversa em que o usuário digita.
     */
    fun typing(conversationId: String) {
        val current = now()
        val last = startedAt[conversationId]
        if (last == null || current - last >= START_INTERVAL_MILLIS) {
            startedAt[conversationId] = current
            scope.launch { realtime.send(TYPING_STARTED_DESTINATION, typingBody(conversationId)) }
        }
        stopJobs.remove(conversationId)?.cancel()
        stopJobs[conversationId] = scope.launch {
            delay(IDLE_STOP_MILLIS)
            stopTyping(conversationId)
        }
    }

    /**
     * Avisa que o usuário parou de digitar. Não faz nada se nenhum `started` foi enviado.
     *
     * @param conversationId conversa em que o usuário digitava.
     */
    fun stopTyping(conversationId: String) {
        stopJobs.remove(conversationId)?.cancel()
        if (startedAt.remove(conversationId) == null) return
        scope.launch { realtime.send(TYPING_STOPPED_DESTINATION, typingBody(conversationId)) }
    }

    private fun typingBody(conversationId: String): String =
        json.encodeToString(TypingCommand.serializer(), TypingCommand(conversationId))

    private companion object {
        const val TYPING_STARTED_DESTINATION = "/app/typing.started"
        const val TYPING_STOPPED_DESTINATION = "/app/typing.stopped"

        /** Intervalo entre dois `started`: 3 s, acima dos 2 s de deduplicação e abaixo dos 5 s de validade do servidor. */
        const val START_INTERVAL_MILLIS = 3_000L

        /** Silêncio de digitação que dispara o `stopped` automático: 4 s. */
        const val IDLE_STOP_MILLIS = 4_000L

        /** Validade local de um `TYPING_STARTED` recebido: 6 s, porque o servidor expira em 5 s sem avisar. */
        const val TYPING_EXPIRY_MILLIS = 6_000L
    }
}

internal fun conversationTopic(conversationId: String): String = "/topic/conversations.$conversationId"
