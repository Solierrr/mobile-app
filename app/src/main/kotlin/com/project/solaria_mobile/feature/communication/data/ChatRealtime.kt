package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.mock.MockChatRealtime
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** O que o messenger publica sobre uma conversa em tempo real. */
sealed interface ChatEvent {
    data class MessageCreated(val message: MessageDto) : ChatEvent
    /** Edição ou exclusão de uma mensagem já existente. */
    data class MessageChanged(val message: MessageDto) : ChatEvent
    data class Typing(val userId: String, val started: Boolean) : ChatEvent
    data class Presence(val userId: String, val online: Boolean) : ChatEvent
    /** Retrato inicial de quem está online, enviado ao assinar a conversa. */
    data class PresenceSnapshot(val users: Map<String, Boolean>) : ChatEvent
    /** A conversa saiu desta conta, por exemplo por remoção do grupo. */
    data class ConversationRemoved(val id: String) : ChatEvent
}

/**
 * Tempo real do chat: eventos da conversa aberta e o aviso de digitação. Em produção vem do STOMP do api-messenger; no modo de
 * demonstração, de um cliente falso com o mesmo contrato.
 */
interface ChatRealtime {
    /** Verdadeiro enquanto a conexão está ativa; uma volta a verdadeiro depois de uma queda pede recuperar o que foi perdido. */
    val connected: Flow<Boolean>
    fun conversationEvents(conversationId: String): Flow<ChatEvent>
    suspend fun typing(conversationId: String, started: Boolean)
}

/** Sem conexão nenhuma: o chat continua funcionando só por REST. */
object NoChatRealtime : ChatRealtime {
    override val connected: Flow<Boolean> = flowOf(false)
    override fun conversationEvents(conversationId: String): Flow<ChatEvent> = emptyFlow()
    override suspend fun typing(conversationId: String, started: Boolean) = Unit
}

@Serializable
private data class TypingPayload(val userId: String)

/** Canais e destinos do messenger (`/topic/conversations.{id}`, `/topic/users.{uid}.events`, `/app/typing.*`). */
internal object MessengerDestinations {
    fun conversation(id: String) = "/topic/conversations.$id"
    fun userEvents(userId: String) = "/topic/users.$userId.events"
    const val TYPING_STARTED = "/app/typing.started"
    const val TYPING_STOPPED = "/app/typing.stopped"
}

/**
 * Converte o envelope `{type, conversationId, eventId, serverTimestamp, payload}` do messenger num [ChatEvent]. Tipos que o
 * app não usa, ou corpos que não são do formato esperado, viram `null`.
 */
internal fun parseChatEvent(body: String, json: Json = ApiJson.instance): ChatEvent? = runCatching {
    val envelope = json.parseToJsonElement(body).jsonObject
    val type = envelope["type"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
    val payload = envelope["payload"]
    when (type) {
        "MESSAGE_CREATED" -> payload?.let { ChatEvent.MessageCreated(json.decodeFromJsonElement(MessageDto.serializer(), it)) }
        "MESSAGE_UPDATED", "MESSAGE_EDITED", "MESSAGE_DELETED" ->
            payload?.let { ChatEvent.MessageChanged(json.decodeFromJsonElement(MessageDto.serializer(), it)) }
        "TYPING_STARTED", "TYPING_STOPPED" -> payload?.let {
            ChatEvent.Typing(json.decodeFromJsonElement(TypingPayload.serializer(), it).userId, type == "TYPING_STARTED")
        }
        "USER_ONLINE", "USER_OFFLINE" -> payload?.let {
            ChatEvent.Presence(json.decodeFromJsonElement(TypingPayload.serializer(), it).userId, type == "USER_ONLINE")
        }
        "PRESENCE_SNAPSHOT" -> payload?.let { ChatEvent.PresenceSnapshot(presenceUsers(it)) }
        // O envelope desse evento não traz `conversationId`; a conversa removida vem no `id` do payload.
        "CONVERSATION_REMOVED" -> payload?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull?.let { ChatEvent.ConversationRemoved(it) }
        else -> null
    }
}.getOrNull()

private fun presenceUsers(payload: JsonElement): Map<String, Boolean> =
    (payload.jsonObject["users"] as? JsonObject).orEmpty().mapNotNull { (id, value) -> value.jsonPrimitive.booleanOrNull?.let { id to it } }.toMap()

/** Ligação com o STOMP real: a conversa aberta mais o canal de eventos da própria conta, filtrado para ela. */
@Singleton
class StompChatRealtime @Inject constructor(
    private val client: RealtimeClient,
    private val credentials: CredentialProvider,
) : ChatRealtime {
    override val connected: Flow<Boolean> = client.state.map { it is RealtimeState.Connected }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun conversationEvents(conversationId: String): Flow<ChatEvent> {
        val me = credentials.accessToken()?.let(::accessTokenSubject)
        val conversation = client.subscribe(MessengerDestinations.conversation(conversationId))
        val own = if (me == null) emptyFlow() else client.subscribe(MessengerDestinations.userEvents(me))
        return merge(conversation, own).onStart { client.connect() }.mapNotNull { parseChatEvent(it) }.filter { event ->
            // O canal da conta traz eventos de todas as conversas; as mensagens são filtradas pela conversa aberta.
            when (event) {
                is ChatEvent.MessageCreated -> event.message.conversationId == conversationId
                is ChatEvent.MessageChanged -> event.message.conversationId == conversationId
                is ChatEvent.ConversationRemoved -> event.id == conversationId
                else -> true
            }
        }
    }

    override suspend fun typing(conversationId: String, started: Boolean) {
        val destination = if (started) MessengerDestinations.TYPING_STARTED else MessengerDestinations.TYPING_STOPPED
        client.send(destination, ApiJson.instance.encodeToString(ConversationIdPayload.serializer(), ConversationIdPayload(conversationId)))
    }
}

@Serializable
private data class ConversationIdPayload(val conversationId: String)

/** Escolhe, a cada uso, o cliente falso no modo de demonstração e o STOMP real fora dele. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SwitchingChatRealtime @Inject constructor(
    private val mockMode: MockModeStore,
    private val mock: MockChatRealtime,
    private val stomp: StompChatRealtime,
) : ChatRealtime {
    private fun current(): ChatRealtime = if (mockMode.isEnabled()) mock else stomp

    override val connected: Flow<Boolean> = mockMode.enabled.flatMapLatest { current().connected }
    override fun conversationEvents(conversationId: String): Flow<ChatEvent> = mockMode.enabled.flatMapLatest { current().conversationEvents(conversationId) }
    override suspend fun typing(conversationId: String, started: Boolean) = current().typing(conversationId, started)
}

/** O que o chat oferece conforme o ambiente. */
fun interface ChatCapabilities {
    /** Foto, documento e localização no compositor; o messenger ainda não tem contrato para isso. */
    fun attachments(): Boolean
}

/** Anexos só aparecem no modo de demonstração. */
@Singleton
class MockModeChatCapabilities @Inject constructor(private val mockMode: MockModeStore) : ChatCapabilities {
    override fun attachments(): Boolean = mockMode.isEnabled()
}
