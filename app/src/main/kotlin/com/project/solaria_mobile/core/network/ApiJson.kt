package com.project.solaria_mobile.core.network

import kotlinx.serialization.json.Json

/**
 * Configuração de `Json` usada em todo o projeto.
 */
object ApiJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}
