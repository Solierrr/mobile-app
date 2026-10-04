package com.project.solaria_mobile.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Constrói um [Retrofit] para um serviço, usando o mesmo `OkHttpClient` para todos.
 *
 * Depende de:
 * - [OkHttpClient] -> cliente HTTP compartilhado
 *
 * Principais operações:
 * - [create] -> Constrói um [Retrofit] para um serviço
 */
@Singleton
class RetrofitFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {

    /**
     * Monta o Retrofit de um backend
     *
     * @param baseUrl url do serviço
     * @param readTimeout substitui o timeout padrão de request
     * @return o Retrofit pronto
     */
    fun create(baseUrl: String, readTimeout: Duration? = null): Retrofit {
        val client = if (readTimeout == null) {
            okHttpClient
        } else {
            okHttpClient.newBuilder().readTimeout(readTimeout).build()
        }
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
