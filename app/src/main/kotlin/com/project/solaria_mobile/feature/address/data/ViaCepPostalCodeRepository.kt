package com.project.solaria_mobile.feature.address.data

import com.project.solaria_mobile.feature.address.domain.PostalAddress
import com.project.solaria_mobile.feature.address.domain.PostalCodeRepository
import com.project.solaria_mobile.feature.address.domain.PostalLookup
import com.project.solaria_mobile.feature.address.domain.ZIP_LENGTH
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Consulta de CEP no ViaCEP, o mesmo serviço público que o web usa. O cliente é próprio e sem interceptors: o token da
 * sessão nunca vai a esse host, e a única informação enviada é o CEP.
 */
@Singleton
class ViaCepPostalCodeRepository @Inject constructor() : PostalCodeRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun lookup(zipDigits: String): PostalLookup {
        if (zipDigits.length != ZIP_LENGTH || zipDigits.any { !it.isDigit() }) return PostalLookup.NotFound
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(Request.Builder().url("https://viacep.com.br/ws/$zipDigits/json/").build()).execute().use { response ->
                    when {
                        response.code == 400 -> PostalLookup.NotFound
                        !response.isSuccessful -> PostalLookup.Failed
                        else -> parseViaCep(response.body.string(), zipDigits)
                    }
                }
            } catch (_: IOException) {
                PostalLookup.Failed
            }
        }
    }
}

/** Lê a resposta do ViaCEP; um CEP que não existe vem como `{"erro": true}` (ou `"true"`, dependendo da versão). */
internal fun parseViaCep(body: String, zipDigits: String): PostalLookup {
    val json = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return PostalLookup.Failed
    if ("erro" in json) return PostalLookup.NotFound
    fun text(key: String) = (json[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    val city = text("localidade")
    if (city.isEmpty()) return PostalLookup.NotFound
    return PostalLookup.Found(PostalAddress(zipDigits, text("logradouro"), text("bairro"), city, text("uf").uppercase()))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AddressModule {
    @Binds abstract fun bindPostalCodes(repository: ViaCepPostalCodeRepository): PostalCodeRepository
}
