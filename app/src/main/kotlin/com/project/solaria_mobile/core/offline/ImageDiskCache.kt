package com.project.solaria_mobile.core.offline

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageDiskCache @Inject constructor(@ApplicationContext context: Context, private val identity: CacheIdentity) {
    private val directory = File(context.filesDir, "offline-images")
    private var generation = 0L

    data class Request(val account: String, val generation: Long)

    @Synchronized
    fun request(): Request? = identity.current()?.let { Request(it, generation) }

    @Synchronized
    fun get(key: String, request: Request? = request()): ByteArray? {
        if (request == null || request.account != identity.current() || request.generation != generation) return null
        return runCatching { file(key, request).takeIf { it.isFile }?.readBytes() }.getOrNull()
    }

    @Synchronized
    fun put(key: String, bytes: ByteArray, request: Request? = request()) {
        if (request == null || request.account != identity.current() || request.generation != generation) return
        runCatching {
            directory.mkdirs()
            val target = file(key, request)
            val pending = File(target.path + ".tmp")
            pending.writeBytes(bytes)
            if (!pending.renameTo(target)) java.nio.file.Files.delete(pending.toPath())
        }
    }

    @Synchronized
    fun clear() {
        generation++
        directory.deleteRecursively()
    }

    private fun file(key: String, request: Request): File {
        val digest = MessageDigest.getInstance("SHA-256").digest("${request.account}:$key".toByteArray())
        return File(directory, digest.joinToString("") { "%02x".format(it) })
    }
}
