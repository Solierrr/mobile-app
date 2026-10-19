package com.project.solaria_mobile.core.offline

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

/** "agora", "há 5 min", "há 3 h" ou "há 2 dias", para dizer quão velho é o dado guardado. */
fun ageLabel(syncedAt: Long, now: Long): String {
    val age = (now - syncedAt).coerceAtLeast(0)
    return when {
        age < MINUTE -> "agora"
        age < HOUR -> "há ${age / MINUTE} min"
        age < DAY -> "há ${age / HOUR} h"
        age < 2 * DAY -> "há 1 dia"
        else -> "há ${age / DAY} dias"
    }
}
