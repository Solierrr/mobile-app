package com.project.solaria_mobile.core.offline

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/** Uma resposta de leitura guardada no aparelho, em JSON, com o instante em que veio do servidor. */
@Entity(tableName = "cache_entry")
data class CacheEntry(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val syncedAt: Long,
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entry WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): CacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntry)

    @Query("DELETE FROM cache_entry")
    suspend fun clear()
}

@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun dao(): CacheDao
}

/** Onde as leituras ficam guardadas; o app usa o Room e os testes usam uma versão em memória. */
interface CacheStore {
    suspend fun get(cacheKey: String): CacheEntry?
    suspend fun put(cacheKey: String, json: String, syncedAt: Long)
    suspend fun clear()
}

class RoomCacheStore(private val dao: CacheDao) : CacheStore {
    override suspend fun get(cacheKey: String): CacheEntry? = dao.get(cacheKey)
    override suspend fun put(cacheKey: String, json: String, syncedAt: Long) = dao.put(CacheEntry(cacheKey, json, syncedAt))
    override suspend fun clear() = dao.clear()
}
