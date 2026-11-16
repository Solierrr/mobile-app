package com.project.solaria_mobile.core.database

import android.content.Context
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.auth.currentUserId
import androidx.room.Room
import com.project.solaria_mobile.core.mock.MockModeStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@Singleton
class ChatDatabaseRouter internal constructor(
    private val real: AppDatabase,
    private val mock: AppDatabase,
    private val mockMode: MockModeStore,
    private val credentials: CredentialProvider,
) {
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
        mockMode: MockModeStore,
        credentials: CredentialProvider,
    ) : this(database(context, "solaria.db"), database(context, "solaria-mock.db"), mockMode, credentials)

    suspend fun forOperation(): AppDatabase {
        val snapshot = currentCoroutineContext()[DatabaseSnapshot] ?: return current()
        if (snapshot.mock != mockMode.isEnabled() || snapshot.actor != credentials.currentUserId()) throw ChatContextChanged()
        return snapshot.database
    }

    suspend fun <T> session(block: suspend () -> T): T {
        if (currentCoroutineContext()[DatabaseSnapshot] != null) return block()
        val mock = mockMode.isEnabled()
        return withContext(DatabaseSnapshot(current(), mock, credentials.currentUserId())) { block() }
    }

    fun current(): AppDatabase = if (mockMode.isEnabled()) mock else real

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> observe(select: (AppDatabase) -> Flow<T>): Flow<T> =
        mockMode.enabled.flatMapLatest { select(current()) }

    fun clear() {
        real.clearAllTables()
        mock.clearAllTables()
    }

    private companion object {
        fun database(context: Context, name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(CHAT_MIGRATION_1_2, CHAT_MIGRATION_2_3).build()
    }
}

class ChatContextChanged : CancellationException()

private class DatabaseSnapshot(val database: AppDatabase, val mock: Boolean, val actor: String?) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<DatabaseSnapshot>
}
