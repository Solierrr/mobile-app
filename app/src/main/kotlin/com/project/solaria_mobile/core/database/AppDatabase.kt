package com.project.solaria_mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Banco Room do aplicativo (`solaria.db`), criado pelo [DatabaseModule]
 * Tabelas existentes:
 * [PendingMessageEntity] -> guarda a fila de mensagens do chat, que por falta de internet não foram enviadas
 * [MessageEntity] -> cache das mensagens confirmadas das conversas humanas
 * [ConversationEntity] -> cache da lista de conversas, com a prévia e a marca de acesso perdido
 * [UserEntity] -> nome e avatar das pessoas que aparecem nas conversas
 * [SyncStateEntity] -> época e cursor do catch-up de mensagens
 *
 * Principais operações:
 * - [pendingMessages] -> DAO da tabela `pending_messages`
 * - [messages] -> DAO da tabela `messages`
 * - [conversations] -> DAO da tabela `conversations`
 * - [users] -> DAO da tabela `users`
 * - [syncState] -> DAO da tabela `sync_state`
 */
@Database(
    entities = [
        PendingMessageEntity::class,
        MessageEntity::class,
        ConversationEntity::class,
        UserEntity::class,
        SyncStateEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingMessages(): PendingMessageDao
    abstract fun messages(): MessageDao
    abstract fun conversations(): ConversationDao
    abstract fun users(): UserDao
    abstract fun syncState(): SyncStateDao
}
