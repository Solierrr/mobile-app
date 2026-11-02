package com.project.solaria_mobile.feature.communication.data

import android.content.Context
import androidx.room.Room
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.offline.CacheIdentity
import com.project.solaria_mobile.core.offline.LocalDataCleaner
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine

@Module
@InstallIn(SingletonComponent::class)
object OutboxModule {
    @Provides
    @Singleton
    fun messageOutbox(
        @ApplicationContext context: Context,
        repository: CommunicationRepository,
        identity: CacheIdentity,
        network: NetworkMonitor,
        mockMode: MockModeStore,
    ): MessageOutbox {
        val database = Room.databaseBuilder(context, OutboxDatabase::class.java, "message-outbox.db").addMigrations(OUTBOX_MIGRATION_1_2).build()
        // No modo mock as respostas são simuladas no próprio app, então a fila é separada e não depende de rede.
        val account = OutboxAccount { if (mockMode.isEnabled()) "mock:${BuildConfig.ENVIRONMENT}" else identity.current() }
        val canSend = combine(network.isOnline, mockMode.enabled) { online, mock -> online || mock }
        return MessageOutbox(database.dao(), repository, account, canSend, Dispatchers.IO)
    }

    @Provides
    @IntoSet
    fun outboxCleaner(outbox: MessageOutbox): LocalDataCleaner = outbox
}
