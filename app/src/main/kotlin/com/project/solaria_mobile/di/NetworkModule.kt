package com.project.solaria_mobile.di

import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.AuthHeaderInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.time.Duration
import javax.inject.Singleton

/**
 * Módulo Hilt da camada de rede.
 * Fornece as instâncias compartilhadas de [Json] e [OkHttpClient] da fundação REST.
 *
 * Cada classe chama `RetrofitFactory.create(url)`
 *
 * Principais operações:
 * - [provideJson] -> configuração única de serialização
 * - [provideOkHttpClient] -> cliente HTTP REST com timeouts padrão
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(30)
    private val READ_TIMEOUT: Duration = Duration.ofSeconds(60)
    private val WRITE_TIMEOUT: Duration = Duration.ofSeconds(60)

    @Provides
    @Singleton
    fun provideJson(): Json = ApiJson.instance

    /** Cria o cliente HTTP compartilhado, com os timeouts padrão do app. */
    @Provides
    @Singleton
    fun provideOkHttpClient(authHeaderInterceptor: AuthHeaderInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT)
            .readTimeout(READ_TIMEOUT)
            .writeTimeout(WRITE_TIMEOUT)
            .addInterceptor(authHeaderInterceptor)
            .build()
}
