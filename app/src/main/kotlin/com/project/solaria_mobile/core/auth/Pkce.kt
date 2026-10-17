package com.project.solaria_mobile.core.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object Pkce {
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        random.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    fun challengeFor(verifier: String): String =
        encoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
}
