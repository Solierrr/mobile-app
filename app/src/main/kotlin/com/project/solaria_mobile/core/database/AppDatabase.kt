package com.project.solaria_mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Banco Room do aplicativo (`solaria.db`), criado pelo [DatabaseModule]
 * Tabelas existentes:
 * [PendingMessageEntity] -> guarda a fila de mensagens do chat, que por falta de internet não foram enviadas
 *
 * Principais operações:
 * - [pendingMessages] -> DAO da tabela `pending_messages`
 */
@Database(entities = [PendingMessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingMessages(): PendingMessageDao
}
