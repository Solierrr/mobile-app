package com.project.solaria_mobile.core.auth

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun accessTokenSubject(token: String): String? = runCatching {
    val payload = token.split('.')[1]
    val decoded = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
    val claims: JsonObject = Json.parseToJsonElement(decoded).jsonObject
    claims["sub"]?.jsonPrimitive?.content
}.getOrNull()
