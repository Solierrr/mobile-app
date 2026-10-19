package com.project.solaria_mobile.core.offline

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda no aparelho os bytes das imagens já vistas (logos, avatares, fotos) para abrirem sem internet. */
@Singleton
class ImageDiskCache @Inject constructor(@ApplicationContext context: Context) {
    private val directory = File(context.filesDir, "offline-images")

    fun get(key: String): ByteArray? = runCatching { file(key).takeIf { it.isFile }?.readBytes() }.getOrNull()

    fun put(key: String, bytes: ByteArray) {
        runCatching {
            directory.mkdirs()
            file(key).writeBytes(bytes)
        }
    }

    fun clear() {
        runCatching { directory.deleteRecursively() }
    }

    private fun file(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return File(directory, digest.joinToString("") { "%02x".format(it) })
    }
}
