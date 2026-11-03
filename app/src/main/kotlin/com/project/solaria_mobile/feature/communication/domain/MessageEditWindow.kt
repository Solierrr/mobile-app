package com.project.solaria_mobile.feature.communication.domain

import java.time.Duration
import java.time.Instant

/** O servidor limita edição e exclusão a trinta minutos após o envio. Sem a data não se oferece a ação. */
fun ChatMessage.canModify(now: Instant = Instant.now()): Boolean {
    val sent = sentAt?.toInstant() ?: return false
    val age = Duration.between(sent, now)
    return mine && !deleted && id.isNotBlank() && !age.isNegative && age <= Duration.ofMinutes(30)
}
