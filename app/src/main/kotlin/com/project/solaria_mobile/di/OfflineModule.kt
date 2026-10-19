package com.project.solaria_mobile.di

import android.content.Context
import androidx.room.Room
import com.project.solaria_mobile.core.offline.CacheDatabase
import com.project.solaria_mobile.core.offline.CacheStore
import com.project.solaria_mobile.core.offline.RoomCacheStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Banco local onde as leituras ficam guardadas para o modo offline. */
@Module
@InstallIn(SingletonComponent::class)
object OfflineModule {
    @Provides
    @Singleton
    fun cacheStore(@ApplicationContext context: Context): CacheStore {
        val database = Room.databaseBuilder(context, CacheDatabase::class.java, "offline-cache.db").build()
        return RoomCacheStore(database.dao())
    }
}
