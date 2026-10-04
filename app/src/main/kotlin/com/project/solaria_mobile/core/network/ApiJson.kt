package com.project.solaria_mobile.core.network

import kotlinx.serialization.json.Json

/**
 *  configuração de `Json` usada em todo projeto
 */
object ApiJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}
