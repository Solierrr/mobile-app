package com.project.solaria_mobile.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val CHAT_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `senderId` TEXT, `clientMessageId` TEXT, `content` TEXT, `edited` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `changeVersion` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_conversationId_sequence` ON `messages` (`conversationId`, `sequence`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` TEXT NOT NULL, `syncEpoch` TEXT NOT NULL, `syncVersion` INTEGER NOT NULL, `conversationType` TEXT NOT NULL, `participantIds` TEXT NOT NULL, `title` TEXT, `archived` INTEGER NOT NULL, `lastInteractionAt` INTEGER, `lastMessageId` TEXT, `lastMessageSequence` INTEGER NOT NULL, `lastMessageContent` TEXT, `lastMessageDeleted` INTEGER NOT NULL, `accessLost` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`authId` TEXT NOT NULL, `username` TEXT NOT NULL, `avatar` TEXT, PRIMARY KEY(`authId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_state` (`id` INTEGER NOT NULL, `syncEpoch` TEXT NOT NULL, `cursor` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val CHAT_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `conversations` ADD COLUMN `lastReadSequence` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `conversations` ADD COLUMN `localReadSequence` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `conversations` ADD COLUMN `unreadCount` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `conversations` ADD COLUMN `muted` INTEGER NOT NULL DEFAULT 0")
    }
}
