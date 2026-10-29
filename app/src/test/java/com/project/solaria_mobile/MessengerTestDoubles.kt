package com.project.solaria_mobile

import com.project.solaria_mobile.core.auth.AuthTokens
import com.project.solaria_mobile.feature.communication.data.ConversationStateRequest
import com.project.solaria_mobile.feature.communication.data.ConversationStateDto
import com.project.solaria_mobile.core.auth.PendingLogin
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.ConversationEntity
import com.project.solaria_mobile.core.database.MessageDao
import com.project.solaria_mobile.core.database.MessageEntity
import com.project.solaria_mobile.core.database.MessageStats
import com.project.solaria_mobile.core.database.PendingMessageDao
import com.project.solaria_mobile.core.database.PendingMessageEntity
import com.project.solaria_mobile.core.database.SyncStateDao
import com.project.solaria_mobile.core.database.SyncStateEntity
import com.project.solaria_mobile.core.database.UserDao
import com.project.solaria_mobile.core.database.UserEntity
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeState
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.MessageUpdatesDto
import com.project.solaria_mobile.feature.communication.data.MessengerApi
import com.project.solaria_mobile.feature.communication.data.SendMessageRequest
import com.project.solaria_mobile.feature.profile.data.ConnectionRequest
import com.project.solaria_mobile.feature.profile.data.ProfileApi
import com.project.solaria_mobile.feature.profile.data.SelfProfileDto
import com.project.solaria_mobile.feature.profile.data.UserDto
import com.project.solaria_mobile.feature.profile.data.UserSummaryDto
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * Monta um JWT de teste com o `sub` informado. A assinatura é um texto fictício.
 */
fun tokenFor(subject: String): String {
    val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"$subject"}""".toByteArray())
    return "e30.$payload.signature"
}

/** Erro HTTP como o Retrofit o lança, com corpo JSON vazio. */
fun httpError(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))

/**
 * Dublê em memória de [PendingMessageDao], com as mesmas regras das consultas SQL.
 *
 * Filtra por remetente, deixa de fora as mensagens `failed` no envio e ordena o envio por `localId`.
 */
class InMemoryPendingMessageDao : PendingMessageDao {
    private val table = MutableStateFlow<List<PendingMessageEntity>>(emptyList())
    private var nextId = 1L

    val rows: List<PendingMessageEntity> get() = table.value

    override suspend fun insert(message: PendingMessageEntity) {
        table.update { it + message.copy(localId = nextId++) }
    }

    override fun observe(senderId: String, conversationId: String): Flow<List<PendingMessageEntity>> =
        table.map { rows -> rows.filter { it.senderId == senderId && it.conversationId == conversationId } }

    override suspend fun sendable(senderId: String): List<PendingMessageEntity> =
        table.value.filter { it.senderId == senderId && !it.failed }.sortedBy { it.localId }

    override suspend fun countAttempt(clientMessageId: String) = change(clientMessageId) { it.copy(attempts = it.attempts + 1) }

    override suspend fun setFailed(clientMessageId: String, failed: Boolean) = change(clientMessageId) { it.copy(failed = failed) }

    override suspend fun delete(clientMessageId: String) {
        table.update { rows -> rows.filterNot { it.clientMessageId == clientMessageId } }
    }

    fun clear() {
        table.value = emptyList()
    }

    private fun change(clientMessageId: String, transform: (PendingMessageEntity) -> PendingMessageEntity) {
        table.update { rows -> rows.map { if (it.clientMessageId == clientMessageId) transform(it) else it } }
    }
}

/**
 * Dublê em memória de [MessageDao]. Implementa só as consultas simples, com as mesmas regras do SQL: a regra de
 * versão fica no `upsert` da classe base, que roda igual à de produção (sem a transação do Room).
 */
class InMemoryMessageDao(private val conversations: InMemoryConversationDao) : MessageDao() {
    private val table = MutableStateFlow<List<MessageEntity>>(emptyList())

    val rows: List<MessageEntity> get() = table.value

    override fun observe(conversationId: String): Flow<List<MessageEntity>> =
        table.map { all -> all.filter { it.conversationId == conversationId }.sortedBy { it.sequence } }

    override suspend fun find(id: String): MessageEntity? = table.value.firstOrNull { it.id == id }

    // REPLACE do SQLite também troca a linha que ocupa a mesma (conversationId, sequence)
    override suspend fun put(message: MessageEntity) {
        table.update { all ->
            all.filterNot { it.id == message.id || (it.conversationId == message.conversationId && it.sequence == message.sequence) } + message
        }
    }

    override suspend fun stats(conversationId: String): MessageStats {
        val sequences = sequencesOf(conversationId)
        return MessageStats(sequences.size, sequences.minOrNull(), sequences.maxOrNull())
    }

    override suspend fun holeAfter(conversationId: String): Int? {
        val sequences = sequencesOf(conversationId).toSet()
        val max = sequences.maxOrNull() ?: return null
        return sequences.filter { it < max && it + 1 !in sequences }.minOrNull()
    }

    override suspend fun senders(conversationId: String): List<String> =
        table.value.filter { it.conversationId == conversationId }.mapNotNull { it.senderId }.distinct()

    override suspend fun clear() {
        table.value = emptyList()
    }

    override suspend fun purgeChatbots() {
        val chatbots = conversations.chatbotIds().toSet()
        table.update { all -> all.filterNot { it.conversationId in chatbots } }
    }

    private fun sequencesOf(conversationId: String): List<Int> =
        table.value.filter { it.conversationId == conversationId }.map { it.sequence }
}

/** Dublê em memória de [ConversationDao], com a ordem e o filtro de arquivadas do SQL. */
class InMemoryConversationDao : ConversationDao() {
    private val table = MutableStateFlow<List<ConversationEntity>>(emptyList())

    val rows: List<ConversationEntity> get() = table.value

    override fun observeList(): Flow<List<ConversationEntity>> =
        table.map { all -> all.filter { !it.archived }.sortedByDescending { it.lastInteractionAt ?: Long.MIN_VALUE } }

    override fun observe(id: String): Flow<ConversationEntity?> = table.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun find(id: String): ConversationEntity? = table.value.firstOrNull { it.id == id }

    override suspend fun all(): List<ConversationEntity> = table.value

    override suspend fun chatbotIds(): List<String> = table.value.filter { it.conversationType == "CHAT_BOT" }.map { it.id }

    override suspend fun put(conversation: ConversationEntity) {
        table.update { all -> all.filterNot { it.id == conversation.id } + conversation }
    }

    override suspend fun refreshPreview(conversationId: String, messageId: String, content: String?, deleted: Boolean) {
        table.update { all ->
            all.map {
                if (it.id == conversationId && it.lastMessageId == messageId) it.copy(lastMessageContent = content, lastMessageDeleted = deleted) else it
            }
        }
    }

    override fun observeArchivedList(): Flow<List<ConversationEntity>> =
        table.map { all -> all.filter { it.archived }.sortedByDescending { it.lastInteractionAt ?: Long.MIN_VALUE } }

    override suspend fun markReadLocally(conversationId: String, sequence: Int) {
        table.update { all -> all.map { if (it.id == conversationId) it.copy(localReadSequence = maxOf(it.localReadSequence, sequence)) else it } }
    }

    override suspend fun setLocalRead(conversationId: String, sequence: Int) {
        table.update { all -> all.map { if (it.id == conversationId) it.copy(localReadSequence = sequence) else it } }
    }

    override suspend fun pendingReads(): List<ConversationEntity> =
        table.value.filter { it.localReadSequence > it.lastReadSequence && !it.accessLost && it.conversationType != "CHAT_BOT" }

    override suspend fun applyState(conversationId: String, lastReadSequence: Int, unreadCount: Long, muted: Boolean, archived: Boolean) {
        table.update { all ->
            all.map {
                if (it.id == conversationId) {
                    it.copy(
                        lastReadSequence = lastReadSequence,
                        unreadCount = unreadCount,
                        muted = muted,
                        archived = archived,
                        localReadSequence = maxOf(it.localReadSequence, lastReadSequence),
                    )
                } else {
                    it
                }
            }
        }
    }

    fun clear() {
        table.value = emptyList()
    }
}

/** Dublê em memória de [UserDao]. */
class InMemoryUserDao : UserDao() {
    private val table = MutableStateFlow<List<UserEntity>>(emptyList())

    val rows: List<UserEntity> get() = table.value

    var writes = 0
        private set

    override fun observeAll(): Flow<List<UserEntity>> = table

    override suspend fun find(authId: String): UserEntity? = table.value.firstOrNull { it.authId == authId }

    override suspend fun put(user: UserEntity) {
        writes++
        table.update { all -> all.filterNot { it.authId == user.authId } + user }
    }

    fun clear() {
        table.value = emptyList()
    }
}

/** Dublê em memória de [SyncStateDao]. */
class InMemorySyncStateDao : SyncStateDao {
    var state: SyncStateEntity? = null

    override suspend fun get(): SyncStateEntity? = state

    override suspend fun put(state: SyncStateEntity) {
        this.state = state
    }

    override suspend fun clear() {
        state = null
    }
}

/** [TokenStore] em memória. Trocar [tokens] simula login, renovação de token e logout. */
class FakeTokenStore(initial: AuthTokens? = null) : TokenStore {
    override val tokens = MutableStateFlow(initial)

    override fun currentTokens(): AuthTokens? = tokens.value

    override suspend fun saveTokens(tokens: AuthTokens) {
        this.tokens.value = tokens
    }

    override suspend fun clearTokens() {
        tokens.value = null
    }

    override suspend fun savePendingLogin(pending: PendingLogin) = Unit

    override suspend fun takePendingLogin(): PendingLogin? = null

    fun signIn(subject: String, suffix: String = "") {
        tokens.value = AuthTokens(tokenFor(subject) + suffix, "refresh")
    }
}

/** [ProfileApi] em memória. Só `summary` responde; as demais rotas não são usadas pelo chat. */
class FakeProfileApi : ProfileApi {
    val summaries = mutableMapOf<String, UserSummaryDto>()
    val summaryCalls = mutableListOf<String>()

    override suspend fun summary(authId: String): UserSummaryDto {
        summaryCalls += authId
        return summaries[authId] ?: throw httpError(404)
    }

    override suspend fun me(): SelfProfileDto = throw httpError(404)

    override suspend fun connections(): List<UserDto> = emptyList()

    override suspend fun connection(id: String): UserDto = throw httpError(404)

    override suspend fun addConnection(body: ConnectionRequest): UserDto = throw httpError(404)

    override suspend fun removeConnection(id: String): Response<Unit> = Response.success(Unit)
}

/**
 * Messenger em memória, com o comportamento dos endpoints usados pelo app.
 *
 * - [history] guarda todas as mensagens do servidor, com `changeVersion`. [serverEdit] e [serverDelete] simulam outra
 *   pessoa mudando uma mensagem: sobem a versão do relógio como o servidor.
 * - [conversationItems] é a lista de `/conversations/me`. A rota devolve só as arquivadas ou só as demais, como a real.
 * - Cada falha programada em [sendFailures] vale para um envio; `persistBeforeFailing` simula resposta perdida.
 * - [messageRequests] registra cada pedido de histórico, para o teste conferir qual trecho foi buscado.
 * - [forbidden] lista as conversas a que o usuário não tem acesso: o histórico responde 403 e o catch-up as ignora.
 */
class ScriptedMessengerApi : MessengerApi {
    val history = mutableListOf<MessageDto>()
    val sendRequests = mutableListOf<SendMessageRequest>()
    val sendFailures = ArrayDeque<Throwable>()
    val conversationItems = mutableListOf<ConversationDto>()
    val messageRequests = mutableListOf<MessageRequest>()
    val forbidden = mutableSetOf<String>()
    var persistBeforeFailing = false
    var historyCalls = 0
    var conversationFailure: Throwable? = null
    var historyFailure: Throwable? = null
    var beforeReply: (() -> Unit)? = null
    var listFailure: Throwable? = null
    var epoch = "epoch-1"
    var version = 0L
    var updateCalls = 0

    /** Pedidos de PATCH de estado, na ordem: id da conversa e corpo. */
    val stateRequests = mutableListOf<Pair<String, ConversationStateRequest>>()
    var stateFailure: Throwable? = null
    private val serverRead = mutableMapOf<String, Int>()
    private val mutedIds = mutableSetOf<String>()
    private val archivedIds = mutableSetOf<String>()

    /** Um pedido de histórico, como o app o fez. */
    data class MessageRequest(val conversationId: String, val sinceSequence: Int?, val beforeSequence: Int?, val limit: Int)

    fun nextVersion(): Long = ++version

    /** Acrescenta uma mensagem do servidor com a próxima versão. */
    fun serverMessage(conversationId: String, sequence: Int, content: String = "mensagem $sequence", senderId: String = "user-2"): MessageDto =
        MessageDto(
            id = "$conversationId-m$sequence",
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            sequence = sequence,
            timestamp = "2026-10-28T10:00:00Z",
            changeVersion = nextVersion(),
        ).also { history += it }

    fun serverEdit(id: String, content: String) {
        history.replaceAll { if (it.id == id) it.copy(content = content, edited = true, changeVersion = nextVersion()) else it }
    }

    fun serverDelete(id: String) {
        history.replaceAll { if (it.id == id) it.copy(content = "", deleted = true, changeVersion = nextVersion()) else it }
    }

    override suspend fun conversations(archived: Boolean): List<ConversationDto> {
        listFailure?.let { throw it }
        return conversationItems.filter { (it.archived == true) == archived }
    }

    override suspend fun conversation(id: String): ConversationDto {
        conversationFailure?.let { throw it }
        return conversationItems.firstOrNull { it.id == id } ?: throw httpError(404)
    }

    override suspend fun messages(conversationId: String, sinceSequence: Int?, beforeSequence: Int?, limit: Int): List<MessageDto> {
        historyCalls++
        messageRequests += MessageRequest(conversationId, sinceSequence, beforeSequence, limit)
        historyFailure?.let { throw it }
        if (conversationId in forbidden) throw httpError(403)
        val inConversation = history.filter { it.conversationId == conversationId }.sortedBy { it.sequence }
        // Como o messenger: com sinceSequence as primeiras depois dele; com beforeSequence ou sem cursor, as últimas
        return when {
            sinceSequence != null -> inConversation.filter { it.sequence > sinceSequence }.take(limit)
            beforeSequence != null -> inConversation.filter { it.sequence < beforeSequence }.takeLast(limit)
            else -> inConversation.takeLast(limit)
        }
    }

    override suspend fun updates(since: Long?, limit: Int?): MessageUpdatesDto {
        updateCalls++
        if (since == null) return MessageUpdatesDto(epoch, version, false, emptyList())
        val changed = history.filter { it.changeVersion > since && it.conversationId !in forbidden }.sortedBy { it.changeVersion }
        val size = limit ?: 100
        val page = changed.take(size)
        return MessageUpdatesDto(epoch, page.lastOrNull()?.changeVersion ?: since, changed.size > size, page)
    }

    // Como o messenger: o cursor só avança, valor acima da última mensagem é 400 e conversa sem acesso é 403
    override suspend fun patchState(id: String, body: ConversationStateRequest): ConversationStateDto {
        stateRequests += id to body
        stateFailure?.let { throw it }
        if (id in forbidden) throw httpError(403)
        val head = history.filter { it.conversationId == id }.maxOfOrNull { it.sequence } ?: 0
        body.lastReadSequence?.let {
            if (it > head) throw httpError(400)
            serverRead[id] = maxOf(serverRead[id] ?: 0, it)
        }
        body.muted?.let { if (it) mutedIds += id else mutedIds -= id }
        body.archived?.let { if (it) archivedIds += id else archivedIds -= id }
        val read = serverRead[id] ?: 0
        return ConversationStateDto(id, epoch, nextVersion(), read, (head - read).toLong(), id in mutedIds, id in archivedIds, id in mutedIds || id in archivedIds)
    }

    override suspend fun send(body: SendMessageRequest): MessageDto {
        sendRequests += body
        val failure = sendFailures.removeFirstOrNull()
        if (failure != null && !persistBeforeFailing) throw failure
        val stored = MessageDto(
            id = "m${history.size + 1}",
            conversationId = body.conversationId,
            senderId = "user-1",
            clientMessageId = body.clientMessageId,
            content = body.content,
            sequence = history.size + 1,
            changeVersion = nextVersion(),
        )
        history += stored
        beforeReply?.invoke()
        if (failure != null) throw failure
        return stored
    }
}

/**
 * Dublê de [RealtimeClient]. O teste publica pares (destino, corpo) em [frames]; o estado vem de [connection].
 *
 * `connect` e `disconnect` não fazem nada.
 */
class FakeRealtimeClient : RealtimeClient {
    val frames = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 16)
    val connection = MutableStateFlow<RealtimeState>(RealtimeState.Disconnected)
    override val state: StateFlow<RealtimeState> = connection

    override fun subscribe(destination: String): Flow<String> =
        frames.filter { it.first == destination }.map { it.second }

    override suspend fun send(destination: String, body: String): ApiResult<Unit> = ApiResult.Success(Unit)

    override fun connect() = Unit

    override fun disconnect() = Unit
}
