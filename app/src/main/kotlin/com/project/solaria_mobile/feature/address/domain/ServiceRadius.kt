package com.project.solaria_mobile.feature.address.domain

/** Raios de atendimento oferecidos, em km. Valores fixos deixam o limite claro e dispensam arrastar no mapa. */
object ServiceRadius {
    val PRESETS = listOf(10, 25, 50, 100, 200)

    /** Passa para o próximo raio; depois do último volta a "sem raio" (`null`). */
    fun next(current: Int?): Int? = when (current) {
        null -> PRESETS.first()
        else -> PRESETS.getOrNull(PRESETS.indexOf(current) + 1)
    }
}
