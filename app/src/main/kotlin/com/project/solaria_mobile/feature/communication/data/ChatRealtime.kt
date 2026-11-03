package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.mock.MockChatRealtime
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiJson
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** O que o messenger publica em tempo real para a conta, de qualquer uma das conversas dela. */
sealed interface ChatEvent {
    data class MessageCreated(val message: MessageDto) : ChatEvent
    /** Edição ou exclusão de uma mensagem já existente. */
    data class MessageChanged(val message: MessageDto) : ChatEvent
    /** [userId] começou ou parou de digitar na conversa [conversationId]. */
    data class Typing(val conversationId: String, val userId: String, val started: Boolean) : ChatEvent
    /** Uma pessoa ficou online ou offline; [epoch] e [version] ordenam os avisos dela. */
    data class Presence(val userId: String, val online: Boolean, val epoch: String = "", val version: Long = 0) : ChatEvent
    /**
     * Retrato de presença: o inicial, ao assinar o canal da conta, ou o parcial, quando uma conversa ganha gente. Só soma ao
     * que já se sabe.
     */
    data class PresenceSnapshot(val users: Map<String, Boolean>, val epoch: String = "", val version: Long = 0) : ChatEvent
    /**
     * A conversa mudou para esta conta: mensagem nova, edição, participantes ou estado pessoal. Traz a mesma projeção da lista
     * (`GET /conversations/me`), já com a prévia e as não lidas desta conta.
     */
    data class ConversationUpdated(val conversation: ConversationDto) : ChatEvent
    /** A conversa saiu desta conta, por exemplo por remoção do grupo. */
    data class ConversationRemoved(val id: String) : ChatEvent
    /**
     * Mensagem que mudou enquanto a conexão esteve fora, vinda da recuperação por versão: criada, editada ou excluída, no estado
     * atual. Quem a recebe troca pelo identificador ou acrescenta.
     */
    data class MessageRecovered(val message: MessageDto) : ChatEvent
    /** A recuperação não pode saber o que mudou (o servidor recomeçou a contagem de versões): quem mostra dados deve lê-los de novo. */
    data object Resynced : ChatEvent
}

/** Conversa a que o evento de mensagem, de atualização ou de remoção pertence; os outros tipos não são de uma conversa só ou nunca saem de [ChatRealtime.events]. */
internal fun ChatEvent.conversationId(): String? = when (this) {
    is ChatEvent.MessageCreated -> message.conversationId
    is ChatEvent.MessageChanged -> message.conversationId
    is ChatEvent.ConversationUpdated -> conversation.id
    is ChatEvent.ConversationRemoved -> id
    is ChatEvent.MessageRecovered -> message.conversationId
    else -> null
}

/**
 * Quem está online entre os contatos da conta, em identificadores minúsculos. [known] é falso sem conexão ou antes do retrato
 * inicial: nesse caso "ninguém online" não quer dizer que todos estão offline.
 */
data class PresenceState(val known: Boolean = false, val online: Set<String> = emptySet())

/**
 * Tempo real do chat: um canal por conta, assinado uma vez por conexão, que traz os eventos de todas as conversas dela, em
 * qualquer tela. Em produção vem do STOMP do api-messenger; no modo de demonstração, de um cliente falso com o mesmo contrato.
 */
interface ChatRealtime {
    /** Verdadeiro enquanto a conexão está ativa; uma volta a verdadeiro depois de uma queda pede recuperar o que foi perdido. */
    val connected: Flow<Boolean>
    val connection: Flow<RealtimeState> get() = connected.map { if (it) RealtimeState.Connected else RealtimeState.Disconnected }
    /** Eventos de mensagem e de conversa (atualizada ou removida), de todas as conversas da conta. Coletar pede a conexão. */
    val events: Flow<ChatEvent>
    /** Quem está online entre os contatos da conta; vale para qualquer conversa e continua entre as telas. */
    val presence: Flow<PresenceState>
    /** Quem está digitando em cada conversa, por identificador de conversa. */
    val typingByConversation: Flow<Map<String, Set<String>>>
    suspend fun typing(conversationId: String, started: Boolean)
}

/** Sem conexão nenhuma: o chat continua funcionando só por REST. */
object NoChatRealtime : ChatRealtime {
    override val connected: Flow<Boolean> = flowOf(false)
    override val events: Flow<ChatEvent> = emptyFlow()
    override val presence: Flow<PresenceState> = flowOf(PresenceState())
    override val typingByConversation: Flow<Map<String, Set<String>>> = flowOf(emptyMap())
    override suspend fun typing(conversationId: String, started: Boolean) = Unit
}

@Serializable
private data class TypingPayload(val userId: String)

/** Canais e destinos do messenger (`/topic/users.{uid}.events`, `/app/typing.*`). Não há tópico por conversa. */
internal object MessengerDestinations {
    fun userEvents(userId: String) = "/topic/users.$userId.events"
    const val TYPING_STARTED = "/app/typing.started"
    const val TYPING_STOPPED = "/app/typing.stopped"
}

/**
 * Converte o envelope `{type, conversationId, eventId, serverTimestamp, payload}` do messenger num [ChatEvent]. Tipos que o
 * app não usa, ou corpos que não são do formato esperado, viram `null`. Presença e retrato vêm sem `conversationId`.
 */
internal fun parseChatEvent(body: String, json: Json = ApiJson.instance): ChatEvent? = runCatching {
    val envelope = json.parseToJsonElement(body).jsonObject
    val type = envelope["type"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
    val payload = envelope["payload"]
    when (type) {
        "MESSAGE_CREATED" -> payload?.let { ChatEvent.MessageCreated(json.decodeFromJsonElement(MessageDto.serializer(), it)) }
        "MESSAGE_UPDATED", "MESSAGE_EDITED", "MESSAGE_DELETED" ->
            payload?.let { ChatEvent.MessageChanged(json.decodeFromJsonElement(MessageDto.serializer(), it)) }
        "TYPING_STARTED", "TYPING_STOPPED" -> {
            val conversationId = envelope["conversationId"]?.jsonPrimitive?.contentOrNull
            if (conversationId == null || payload == null) null
            else ChatEvent.Typing(conversationId, json.decodeFromJsonElement(TypingPayload.serializer(), payload).userId, type == "TYPING_STARTED")
        }
        "USER_ONLINE", "USER_OFFLINE" -> payload?.jsonObject?.let {
            ChatEvent.Presence(it.getValue("userId").jsonPrimitive.content, type == "USER_ONLINE", presenceEpoch(it), it["presenceVersion"]?.jsonPrimitive?.longOrNull ?: 0)
        }
        "PRESENCE_SNAPSHOT" -> payload?.jsonObject?.let { ChatEvent.PresenceSnapshot(presenceUsers(it), presenceEpoch(it), it["presenceVersion"]?.jsonPrimitive?.longOrNull ?: 0) }
        "CONVERSATION_UPDATED" -> payload?.let { ChatEvent.ConversationUpdated(json.decodeFromJsonElement(ConversationDto.serializer(), it)) }
        // O envelope desse evento não traz `conversationId`; a conversa removida vem no `id` do payload.
        "CONVERSATION_REMOVED" -> payload?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull?.let { ChatEvent.ConversationRemoved(it) }
        else -> null
    }
}.getOrNull()

private fun presenceEpoch(payload: JsonObject): String = payload["presenceEpoch"]?.jsonPrimitive?.contentOrNull.orEmpty()

private fun presenceUsers(payload: JsonObject): Map<String, Boolean> =
    (payload["users"] as? JsonObject).orEmpty().mapNotNull { (id, value) -> value.jsonPrimitive.booleanOrNull?.let { id to it } }.toMap()

/**
 * Ligação com o STOMP real. Enquanto há conexão, mantém uma assinatura só, no canal de eventos da própria conta, e a reparte:
 * mensagens e mudanças de conversa vão para [events], a presença para [presence] e o "digitando" para [typingByConversation]. Presença e
 * "digitando" ficam aqui, e não nas telas, para existirem mesmo sem nenhum chat aberto. Quem cria o singleton cedo (o `App`)
 * garante que a escuta comece junto com a conexão.
 */
@Singleton
class StompChatRealtime @Inject constructor(
    private val client: RealtimeClient,
    private val credentials: CredentialProvider,
    private val scope: CoroutineScope,
    private val recovery: MessageRecovery,
) : ChatRealtime {
    private val tracker = PresenceTracker()
    private val messageEvents = MutableSharedFlow<ChatEvent>(extraBufferCapacity = EVENT_BUFFER)
    private val presenceState = MutableStateFlow(PresenceState())
    private val typingState = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val typingExpiry = ConcurrentHashMap<Pair<String, String>, Job>()
    // Só a coroutine de escuta mexe nestes dois.
    private var snapshotSeen = false
    private var recoveryJob: Job? = null

    override val connected: Flow<Boolean> = client.state.map { it is RealtimeState.Connected }.distinctUntilChanged()
    override val connection: Flow<RealtimeState> = client.state
    override val events: Flow<ChatEvent> = messageEvents.onStart { client.connect() }
    override val presence: Flow<PresenceState> = presenceState
    override val typingByConversation: Flow<Map<String, Set<String>>> = typingState

    init {
        scope.launch { listen() }
    }

    override suspend fun typing(conversationId: String, started: Boolean) {
        val destination = if (started) MessengerDestinations.TYPING_STARTED else MessengerDestinations.TYPING_STOPPED
        client.send(destination, ApiJson.instance.encodeToString(ConversationIdPayload.serializer(), ConversationIdPayload(conversationId)))
    }

    /** A cada conexão assina o canal da conta; ao cair, presença e "digitando" voltam a ser desconhecidos. */
    private suspend fun listen() {
        client.state.map { it is RealtimeState.Connected }.distinctUntilChanged().collectLatest { up ->
            reset()
            if (!up) return@collectLatest
            val me = credentials.accessToken()?.let(::accessTokenSubject) ?: return@collectLatest
            client.subscribe(MessengerDestinations.userEvents(me)).collect { body -> parseChatEvent(body)?.let(::handle) }
        }
    }

    private fun reset() {
        recoveryJob?.cancel()
        recoveryJob = null
        tracker.clear()
        snapshotSeen = false
        presenceState.value = PresenceState()
        typingExpiry.values.forEach(Job::cancel)
        typingExpiry.clear()
        typingState.value = emptyMap()
    }

    private fun handle(event: ChatEvent) {
        when (event) {
            is ChatEvent.PresenceSnapshot -> {
                tracker.snapshot(event.epoch, event.version, event.users)
                val first = !snapshotSeen
                snapshotSeen = true
                publishPresence()
                // O servidor manda o retrato logo que registra a assinatura: é o sinal de que o canal já está ativo, e só
                // então vale buscar o que passou, para nada cair entre a busca e a assinatura. Retratos parciais não repetem.
                if (first) recoveryJob = scope.launch { recovery.recover(messageEvents::emit) }
            }
            is ChatEvent.Presence -> {
                tracker.change(event.userId, event.online, event.epoch, event.version)
                publishPresence()
            }
            is ChatEvent.Typing -> onTyping(event)
            else -> messageEvents.tryEmit(event)
        }
    }

    // Antes do retrato inicial a presença segue desconhecida, mesmo que um aviso avulso já tenha chegado.
    private fun publishPresence() {
        presenceState.value = PresenceState(known = snapshotSeen, online = tracker.online())
    }

    private fun onTyping(event: ChatEvent.Typing) {
        val user = event.userId.lowercase()
        val key = event.conversationId to user
        typingExpiry.remove(key)?.cancel()
        typingState.update { it.withTyping(event.conversationId, user, event.started) }
        if (event.started) typingExpiry[key] = scope.launch {
            // Sem renovação, o aviso some sozinho, mesmo que o "parou de digitar" se perca.
            delay(TYPING_EXPIRE_MILLIS)
            typingExpiry.remove(key)
            typingState.update { it.withTyping(event.conversationId, user, started = false) }
        }
    }

    private companion object {
        const val EVENT_BUFFER = 64
        const val TYPING_EXPIRE_MILLIS = 8_000L
    }
}

private fun Map<String, Set<String>>.withTyping(conversationId: String, user: String, started: Boolean): Map<String, Set<String>> {
    val users = this[conversationId].orEmpty().let { if (started) it + user else it - user }
    return if (users.isEmpty()) this - conversationId else this + (conversationId to users)
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
    override val connection: Flow<RealtimeState> = mockMode.enabled.flatMapLatest { current().connection }
    override val events: Flow<ChatEvent> = mockMode.enabled.flatMapLatest { current().events }
    override val presence: Flow<PresenceState> = mockMode.enabled.flatMapLatest { current().presence }
    override val typingByConversation: Flow<Map<String, Set<String>>> = mockMode.enabled.flatMapLatest { current().typingByConversation }
    override suspend fun typing(conversationId: String, started: Boolean) = current().typing(conversationId, started)
}

/** O que o chat oferece conforme o ambiente. */
fun interface ChatCapabilities {
    /** Foto, documento e localização no compositor. */
    fun attachments(): Boolean
}

/** O messenger aceita anexos (mídia pelo bucket e localização): o compositor os oferece em qualquer ambiente. */
@Singleton
class AppChatCapabilities @Inject constructor() : ChatCapabilities {
    override fun attachments(): Boolean = true
}
