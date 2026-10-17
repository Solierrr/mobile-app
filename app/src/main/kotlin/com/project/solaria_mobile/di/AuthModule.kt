package com.project.solaria_mobile.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.AuthApi
import com.project.solaria_mobile.core.auth.DataStoreTokenStore
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.network.RetrofitFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {
    @Binds
    abstract fun bindTokenStore(impl: DataStoreTokenStore): TokenStore
}

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.sessionDataStore

    @Provides
    @Singleton
    fun provideAuthApi(factory: RetrofitFactory): AuthApi =
        factory.create(BuildConfig.AUTH_BASE_URL).create(AuthApi::class.java)
}
