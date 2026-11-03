package com.project.solaria_mobile.feature.communication.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.offline.LocalDataCleaner
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

enum class PendingState { PENDING, SENDING, FAILED }

/**
 * Mensagem escrita pelo usuário e ainda não confirmada pelo servidor.
 *
 * `id` vai para o messenger como `clientMessageId`, que volta no histórico. `uncertain` marca que uma tentativa anterior
 * pode ter chegado ao servidor; antes de reenviar, a fila procura esse identificador no histórico para não duplicar.
 */
@Entity(tableName = "pending_message")
data class PendingMessage(
    @PrimaryKey val id: String,
    val account: String,
    val conversationId: String,
    val text: String,
    val createdAt: Long,
    val state: PendingState = PendingState.PENDING,
    val uncertain: Boolean = false,
    val failure: ErrorKind? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val conversationType: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val authAttempts: Int = 0,
)

@Dao
interface PendingMessageDao {
    @Query("SELECT * FROM pending_message WHERE account = :account AND conversationId = :conversationId ORDER BY createdAt")
    fun observe(account: String, conversationId: String): Flow<List<PendingMessage>>

    @Query("SELECT * FROM pending_message WHERE account = :account AND state = 'PENDING' ORDER BY createdAt")
    suspend fun pending(account: String): List<PendingMessage>

    @Query("SELECT * FROM pending_message WHERE id = :id")
    suspend fun get(id: String): PendingMessage?

    @Insert
    suspend fun insert(message: PendingMessage)

    @Update
    suspend fun update(message: PendingMessage)

    @Query("DELETE FROM pending_message WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE pending_message SET state = 'PENDING', uncertain = 1 WHERE state = 'SENDING'")
    suspend fun resetInterrupted()

    @Query("DELETE FROM pending_message")
    suspend fun clear()
}

@Database(entities = [PendingMessage::class], version = 3, exportSchema = false)
abstract class OutboxDatabase : RoomDatabase() {
    abstract fun dao(): PendingMessageDao
}

/** A versão 1 guardava uma contagem de mensagens com o mesmo texto, trocada pelo identificador do app; as mensagens são mantidas. */
val OUTBOX_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE pending_message_new (id TEXT NOT NULL, account TEXT NOT NULL, conversationId TEXT NOT NULL, text TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL, state TEXT NOT NULL, uncertain INTEGER NOT NULL, failure TEXT, PRIMARY KEY(id))",
        )
        db.execSQL(
            "INSERT INTO pending_message_new (id, account, conversationId, text, createdAt, state, uncertain, failure) " +
                "SELECT id, account, conversationId, text, createdAt, state, uncertain, failure FROM pending_message",
        )
        db.execSQL("DROP TABLE pending_message")
        db.execSQL("ALTER TABLE pending_message_new RENAME TO pending_message")
    }
}

/** Mantém a fila e acrescenta o tipo da conversa e o limite de recusas de credencial. */
val OUTBOX_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pending_message ADD COLUMN conversationType TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE pending_message ADD COLUMN authAttempts INTEGER NOT NULL DEFAULT 0")
    }
}

/** Conta e ambiente donos da fila; o modo mock tem a sua própria. */
fun interface OutboxAccount {
    fun current(): String?
}

/** Mensagem entregue: `message` vem da resposta do envio, ou é nula quando a entrega foi confirmada pelo histórico. */
data class Delivered(val conversationId: String, val message: ChatMessage?)

/** Resultado de uma passada pela fila: `retryLater` quando a rede ou o servidor falharam e vale tentar de novo. */
internal data class DrainResult(val retryLater: Boolean)

/**
 * Fila local de mensagens do chat. Guarda cada mensagem antes de enviar, envia na ordem de criação quando há conexão e
 * retoma depois de falhas de rede, de reinício do app ou de troca de tela.
 */
class MessageOutbox(
    private val dao: PendingMessageDao,
    private val repository: CommunicationRepository,
    private val account: OutboxAccount,
    private val canSend: Flow<Boolean>,
    private val dispatcher: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
) : LocalDataCleaner {
    private val lock = Mutex()
    private val trigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val mutableDelivered = MutableSharedFlow<Delivered>(extraBufferCapacity = DELIVERED_BUFFER)
    val delivered: SharedFlow<Delivered> = mutableDelivered
    private var started = false

    fun observe(conversationId: String): Flow<List<PendingMessage>> {
        val owner = account.current() ?: return emptyFlow()
        return dao.observe(owner, conversationId)
    }

    /** Guarda a mensagem para envio. Devolve falso sem conta ativa, quando não há a quem atribuir a mensagem. */
    suspend fun enqueue(conversationId: String, text: String, conversationType: String? = null): Boolean {
        val owner = account.current() ?: return false
        dao.insert(PendingMessage(UUID.randomUUID().toString(), owner, conversationId, text, clock(), conversationType = conversationType))
        trigger.tryEmit(Unit)
        return true
    }

    suspend fun retry(id: String) {
        val message = dao.get(id) ?: return
        if (message.account != account.current()) return
        dao.update(message.copy(state = PendingState.PENDING, failure = null, authAttempts = 0))
        trigger.tryEmit(Unit)
    }

    suspend fun discard(id: String) {
        if (dao.get(id)?.account == account.current()) dao.delete(id)
    }

    override suspend fun clear() = dao.clear()

    /** Começa a enviar a fila em segundo plano enquanto o app estiver aberto. Chamadas repetidas não têm efeito. */
    fun start() {
        if (started) return
        started = true
        CoroutineScope(SupervisorJob() + dispatcher).launch {
            dao.resetInterrupted()
            combine(canSend, trigger.onStart { emit(Unit) }) { allowed, _ -> allowed }.collectLatest { allowed ->
                var attempt = 0
                while (allowed && drain().retryLater) {
                    delay(RETRY_DELAYS_MILLIS[attempt.coerceAtMost(RETRY_DELAYS_MILLIS.lastIndex)])
                    attempt++
                }
            }
        }
    }

    internal suspend fun drain(): DrainResult = lock.withLock {
        val owner = account.current() ?: return DrainResult(retryLater = false)
        for (queued in dao.pending(owner)) {
            if (account.current() != owner) return DrainResult(retryLater = false)
            val message = dao.get(queued.id)?.takeIf { it.state == PendingState.PENDING } ?: continue
            // Se o processo morrer depois do POST, a próxima execução precisa conferir o histórico.
            try {
                dao.update(message.copy(state = PendingState.SENDING, uncertain = true))
                if (message.uncertain) {
                    when (val checked = alreadyDelivered(message)) {
                        is ApiResult.Success -> if (checked.data) {
                            dao.delete(message.id)
                            mutableDelivered.emit(Delivered(message.conversationId, null))
                            continue
                        }
                        is ApiResult.Failure -> {
                            if (checked.kind == ErrorKind.UNAUTHORIZED) {
                                if (recordAuthRejection(message)) return DrainResult(retryLater = true)
                                continue
                            }
                            if (checked.kind in setOf(ErrorKind.FORBIDDEN, ErrorKind.NOT_FOUND)) {
                                dao.update(message.copy(state = PendingState.FAILED, failure = checked.kind))
                                continue
                            }
                            dao.update(message.copy(state = PendingState.PENDING, uncertain = true))
                            return DrainResult(retryLater = true)
                        }
                    }
                }
                message.conversationType?.let { repository.rememberConversationType(message.conversationId, it) }
                when (val result = repository.sendMessage(message.conversationId, message.text, message.id)) {
                    is ApiResult.Success -> {
                        dao.delete(message.id)
                        mutableDelivered.emit(Delivered(message.conversationId, result.data))
                    }
                    is ApiResult.Failure -> when (result.kind) {
                        // A requisição pode ter sido gravada antes da falha: confere o histórico antes de reenviar.
                        ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT, ErrorKind.SERVER, ErrorKind.SERIALIZATION, ErrorKind.UNKNOWN -> {
                            dao.update(message.copy(state = PendingState.PENDING, uncertain = true))
                            return DrainResult(retryLater = true)
                        }
                        // Recusada antes de gravar: tenta de novo depois, sem precisar conferir o histórico.
                        ErrorKind.UNAUTHORIZED -> {
                            if (recordAuthRejection(message)) return DrainResult(retryLater = true)
                        }
                        ErrorKind.RATE_LIMITED -> {
                            dao.update(message.copy(state = PendingState.PENDING))
                            return DrainResult(retryLater = true)
                        }
                        else -> dao.update(message.copy(state = PendingState.FAILED, failure = result.kind))
                    }
                }
            } catch (cancelled: CancellationException) {
                // A coleta é cancelada quando a rede cai: a mensagem precisa voltar à fila sem exigir reinício do processo.
                withContext(NonCancellable) {
                    dao.get(message.id)?.takeIf { it.state == PendingState.SENDING }?.let {
                        dao.update(it.copy(state = PendingState.PENDING, uncertain = true))
                    }
                }
                throw cancelled
            }
        }
        DrainResult(retryLater = false)
    }

    private suspend fun recordAuthRejection(message: PendingMessage): Boolean {
        val attempts = message.authAttempts + 1
        val retry = attempts < MAX_AUTH_ATTEMPTS
        dao.update(message.copy(state = if (retry) PendingState.PENDING else PendingState.FAILED,
            authAttempts = attempts, failure = ErrorKind.UNAUTHORIZED))
        return retry
    }

    /** Verdadeiro se o histórico já tem a mensagem com este identificador; falha se o histórico não pôde ser lido. */
    private suspend fun alreadyDelivered(message: PendingMessage): ApiResult<Boolean> {
        var history = when (val result = repository.messages(message.conversationId)) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.data
        }
        var before = Int.MAX_VALUE
        while (true) {
            if (history.any { it.clientMessageId == message.id }) return ApiResult.Success(true)
            // Relógios do aparelho e do servidor podem divergir: só o fim do histórico confirma ausência.
            if (history.isEmpty()) return ApiResult.Success(false)
            val oldest = history.filter { it.sequence > 0 }.minOfOrNull { it.sequence } ?: return ApiResult.Success(false)
            if (oldest >= before) return ApiResult.Failure(ErrorKind.UNKNOWN)
            before = oldest
            history = when (val result = repository.olderMessages(message.conversationId, before)) {
                is ApiResult.Failure -> return result
                is ApiResult.Success -> result.data
            }
        }
    }

    private companion object {
        const val DELIVERED_BUFFER = 16
        const val MAX_AUTH_ATTEMPTS = 3
        val RETRY_DELAYS_MILLIS = listOf(5_000L, 15_000L, 30_000L, 60_000L)
    }
}
