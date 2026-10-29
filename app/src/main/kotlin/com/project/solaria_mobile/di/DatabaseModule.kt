package com.project.solaria_mobile.di

import android.content.Context
import androidx.room.Room
import com.project.solaria_mobile.core.database.AppDatabase
import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.LocalDataWiper
import com.project.solaria_mobile.core.database.MessageDao
import com.project.solaria_mobile.core.database.PendingMessageDao
import com.project.solaria_mobile.core.database.SyncStateDao
import com.project.solaria_mobile.core.database.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DATABASE_NAME = "solaria.db"

/**
 * Módulo Hilt que cria o banco Room `solaria.db` e entrega seus DAOs
 *
 * Principais operações:
 * - [provideDatabase] -> cria o [AppDatabase] do processo, recriado sem migração na troca de versão
 * - [providePendingMessageDao] -> entrega o DAO da fila de mensagens
 * - [provideMessageDao] -> entrega o DAO das mensagens confirmadas
 * - [provideConversationDao] -> entrega o DAO da lista de conversas
 * - [provideUserDao] -> entrega o DAO de nomes e avatares
 * - [provideSyncStateDao] -> entrega o DAO do cursor de sincronização
 * - [provideLocalDataWiper] -> entrega quem apaga todos os dados locais no logout
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * Cria o banco com o nome `solaria.db`
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun providePendingMessageDao(database: AppDatabase): PendingMessageDao = database.pendingMessages()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messages()

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao = database.conversations()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.users()

    @Provides
    fun provideSyncStateDao(database: AppDatabase): SyncStateDao = database.syncState()

    @Provides
    @Singleton
    fun provideLocalDataWiper(database: AppDatabase): LocalDataWiper =
        LocalDataWiper { withContext(Dispatchers.IO) { database.clearAllTables() } }
}
