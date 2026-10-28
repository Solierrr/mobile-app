package com.project.solaria_mobile.di

import android.content.Context
import androidx.room.Room
import com.project.solaria_mobile.core.database.AppDatabase
import com.project.solaria_mobile.core.database.PendingMessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "solaria.db"

/**
 * Módulo Hilt que cria o banco Room `solaria.db` e entrega seus DAOs
 *
 * Principais operações:
 * - [provideDatabase] -> cria o [AppDatabase] do processo
 * - [providePendingMessageDao] -> entrega o DAO da fila de mensagens
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
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun providePendingMessageDao(database: AppDatabase): PendingMessageDao = database.pendingMessages()
}
