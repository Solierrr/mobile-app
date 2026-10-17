package com.project.solaria_mobile.core.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val clock = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault())
private val day = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault()).withZone(ZoneId.systemDefault())

fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

fun formatTimestamp(value: String?, today: Instant = Instant.now()): String {
    val instant = value?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return ""
    val sameDay = day.format(instant) == day.format(today)
    return if (sameDay) clock.format(instant) else day.format(instant)
}
