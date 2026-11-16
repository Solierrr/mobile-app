package com.project.solaria_mobile.core.offline

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

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

    @Query("DELETE FROM cache_entry WHERE cacheKey = :cacheKey")
    suspend fun remove(cacheKey: String)

    @Query("DELETE FROM cache_entry WHERE substr(cacheKey,1,length(:prefix))=:prefix")
    suspend fun removePrefix(prefix: String)

    @Query("DELETE FROM cache_entry")
    suspend fun clear()
}

@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun dao(): CacheDao
}

interface CacheStore {
    suspend fun get(cacheKey: String): CacheEntry?
    suspend fun put(cacheKey: String, json: String, syncedAt: Long)
    suspend fun clear()
    suspend fun removePrefix(prefix: String)
    suspend fun remove(cacheKey: String)
}

class RoomCacheStore(private val dao: CacheDao) : CacheStore {
    override suspend fun get(cacheKey: String): CacheEntry? = dao.get(cacheKey)
    override suspend fun put(cacheKey: String, json: String, syncedAt: Long) = dao.put(CacheEntry(cacheKey, json, syncedAt))
    override suspend fun clear() = dao.clear()
    override suspend fun removePrefix(prefix: String) = dao.removePrefix(prefix)
    override suspend fun remove(cacheKey: String) = dao.remove(cacheKey)
}
