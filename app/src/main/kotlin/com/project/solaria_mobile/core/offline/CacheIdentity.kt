package com.project.solaria_mobile.core.offline

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.network.ApiJson
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun interface CacheScope {
    fun current(): String?
}

@Singleton
class CacheIdentity @Inject constructor(private val tokens: TokenStore) : CacheScope {
    override fun current(): String? {
        val token = tokens.currentTokens()?.accessToken ?: return null
        val subject = runCatching {
            val payload = String(Base64.getUrlDecoder().decode(token.split('.')[1]), Charsets.UTF_8)
            ApiJson.instance.parseToJsonElement(payload).jsonObject["sub"]?.jsonPrimitive?.content
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return MessageDigest.getInstance("SHA-256").digest("${BuildConfig.ENVIRONMENT}:$subject".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
