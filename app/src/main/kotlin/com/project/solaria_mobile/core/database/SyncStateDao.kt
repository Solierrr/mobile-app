package com.project.solaria_mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO do ponto onde o catch-up parou, sobre a tabela `sync_state`.
 *
 * Principais operações:
 * - [get] -> lê o estado, ou `null` antes da primeira sincronização.
 * - [put] -> grava a época e o cursor.
 * - [clear] -> esquece o estado.
 */
@Dao
interface SyncStateDao {
    /** Lê o estado de sincronização, ou `null` se o aparelho nunca sincronizou. */
    @Query("SELECT * FROM sync_state LIMIT 1")
    suspend fun get(): SyncStateEntity?

    /** Grava o estado, trocando o anterior. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(state: SyncStateEntity)

    /** Apaga o estado. A próxima sincronização recomeça do zero. */
    @Query("DELETE FROM sync_state")
    suspend fun clear()
}
