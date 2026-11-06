package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Linhas da conversa: separadores de dia, o divisor de novas mensagens e as mensagens já agrupadas por remetente. */
sealed interface ChatListItem {
    val key: String

    data class Day(val date: LocalDate) : ChatListItem {
        override val key: String = "day-$date"
    }

    data object NewMessages : ChatListItem {
        override val key: String = "new-messages"
    }

    /**
     * [showSender] vale em grupo, na primeira mensagem de cada sequência de outra pessoa. [firstInGroup] e [lastInGroup]
     * delimitam a sequência de um mesmo remetente: o horário aparece só na última e os balões ficam mais juntos.
     */
    data class Entry(
        val message: ChatMessage,
        val showSender: Boolean,
        val firstInGroup: Boolean,
        val lastInGroup: Boolean,
        override val key: String,
    ) : ChatListItem
}

/** Mensagens do mesmo remetente com menos de 5 minutos entre elas, no mesmo dia, formam uma sequência. */
private val GROUP_WINDOW: Duration = Duration.ofMinutes(5)

internal fun buildChatItems(
    messages: List<ChatMessage>,
    firstUnreadSequence: Int?,
    group: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
): List<ChatListItem> {
    data class Row(val message: ChatMessage, val key: String, val groupedWithPrevious: Boolean)

    val result = mutableListOf<ChatListItem>()
    val rows = mutableListOf<Row>()
    var previous: ChatMessage? = null
    var previousDay: LocalDate? = null

    fun flush() {
        rows.forEachIndexed { index, row ->
            val next = rows.getOrNull(index + 1)
            val firstInGroup = !row.groupedWithPrevious
            result += ChatListItem.Entry(
                row.message,
                showSender = group && !row.message.mine && firstInGroup,
                firstInGroup = firstInGroup,
                lastInGroup = next == null || !next.groupedWithPrevious,
                key = row.key,
            )
        }
        rows.clear()
    }

    messages.forEachIndexed { index, message ->
        val day = message.sentAt?.atZoneSameInstant(zone)?.toLocalDate()
        val dayChanged = day != null && day != previousDay
        val divider = firstUnreadSequence != null && message.sequence == firstUnreadSequence
        if (dayChanged || divider) flush()
        if (dayChanged) result += ChatListItem.Day(day!!)
        if (divider) result += ChatListItem.NewMessages
        val grouped = previous != null && !dayChanged && !divider && sameSequence(previous!!, message)
        rows += Row(message, message.id.ifBlank { "index-$index" }, grouped)
        previous = message
        if (day != null) previousDay = day
    }
    flush()
    return result
}

private fun sameSequence(a: ChatMessage, b: ChatMessage): Boolean {
    if (a.mine != b.mine || a.senderId != b.senderId) return false
    val first = a.sentAt ?: return true
    val second = b.sentAt ?: return true
    return Duration.between(first, second).abs() <= GROUP_WINDOW
}

/**
 * Horário que o balão mostra: sempre a hora do dia (o dia já aparece no separador), no formato do idioma. Sem o horário
 * completo, cai no texto que o servidor já formatou.
 */
internal fun ChatMessage.clockTime(locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String =
    sentAt?.atZoneSameInstant(zone)?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)) ?: time
