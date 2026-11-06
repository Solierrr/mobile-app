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

    @Query("UPDATE pending_message SET state = 'PENDING' WHERE state = 'SENDING'")
    suspend fun resetInterrupted()

    @Query("DELETE FROM pending_message")
    suspend fun clear()
}

@Database(entities = [PendingMessage::class], version = 2, exportSchema = false)
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
    suspend fun enqueue(conversationId: String, text: String): Boolean {
        val owner = account.current() ?: return false
        dao.insert(PendingMessage(UUID.randomUUID().toString(), owner, conversationId, text, clock()))
        trigger.tryEmit(Unit)
        return true
    }

    suspend fun retry(id: String) {
        val message = dao.get(id) ?: return
        dao.update(message.copy(state = PendingState.PENDING, failure = null))
        trigger.tryEmit(Unit)
    }

    suspend fun discard(id: String) = dao.delete(id)

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
            dao.update(message.copy(state = PendingState.SENDING))
            if (message.uncertain) {
                when (alreadyDelivered(message)) {
                    true -> {
                        dao.delete(message.id)
                        mutableDelivered.emit(Delivered(message.conversationId, null))
                        continue
                    }
                    null -> {
                        dao.update(message.copy(state = PendingState.PENDING))
                        return DrainResult(retryLater = true)
                    }
                    false -> Unit
                }
            }
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
                    ErrorKind.UNAUTHORIZED, ErrorKind.RATE_LIMITED -> {
                        dao.update(message.copy(state = PendingState.PENDING))
                        return DrainResult(retryLater = true)
                    }
                    else -> dao.update(message.copy(state = PendingState.FAILED, failure = result.kind))
                }
            }
        }
        DrainResult(retryLater = false)
    }

    /** Verdadeiro se o histórico já tem a mensagem com este identificador; nulo se o histórico não pôde ser lido. */
    private suspend fun alreadyDelivered(message: PendingMessage): Boolean? {
        val history = repository.messages(message.conversationId) as? ApiResult.Success ?: return null
        return history.data.any { it.clientMessageId == message.id }
    }

    private companion object {
        const val DELIVERED_BUFFER = 16
        val RETRY_DELAYS_MILLIS = listOf(5_000L, 15_000L, 30_000L, 60_000L)
    }
}
