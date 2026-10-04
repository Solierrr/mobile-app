package com.project.solaria_mobile.core.network

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

/** Converte erros HTTP em categoria e campos de validação necessários à interface. */
class ApiErrorParser @Inject constructor(
    private val json: Json,
) {
    fun parse(httpStatus: Int, body: String?): ApiResult.Failure {
        val kind = when (httpStatus) {
            400, 422 -> ErrorKind.VALIDATION
            401 -> ErrorKind.UNAUTHORIZED
            403 -> ErrorKind.FORBIDDEN
            404 -> ErrorKind.NOT_FOUND
            409 -> ErrorKind.CONFLICT
            429 -> ErrorKind.RATE_LIMITED
            in 500..599 -> ErrorKind.SERVER
            else -> ErrorKind.UNKNOWN
        }

        return ApiResult.Failure(
            kind = kind,
            invalidFields = if (kind == ErrorKind.VALIDATION) invalidFields(body) else emptySet(),
        )
    }

    // Core/Messenger: errors[].field identifica o campo que falhou na validação
    private fun invalidFields(body: String?): Set<String> {
        val payload = body?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.parseToJsonElement(it) }.getOrNull() as? JsonObject
        }
        return (payload?.get("errors") as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonObject)?.string("field") }
            .toSet()
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.isNotBlank() }
}
