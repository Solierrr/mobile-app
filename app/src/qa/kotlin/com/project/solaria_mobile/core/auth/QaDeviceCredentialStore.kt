package com.project.solaria_mobile.core.auth

import android.content.Context
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class QaDeviceCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cipher: KeystoreCipher,
) {
    suspend fun read(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val stored = File(context.noBackupFilesDir, "qa-device-credential")
            val incoming = File(context.noBackupFilesDir, "qa-provisioning-token")
            if (incoming.exists()) {
                val token = try { incoming.readText().trim() } finally { incoming.delete() }
                if (!token.matches(TOKEN_FORMAT)) return@withContext null
                val target = AtomicFile(stored)
                val output = target.startWrite()
                try {
                    output.write(cipher.encrypt(token).toByteArray(Charsets.UTF_8))
                    target.finishWrite(output)
                } catch (failure: Exception) {
                    target.failWrite(output)
                    throw failure
                }
            }
            if (!stored.exists()) null else cipher.decrypt(stored.readText())?.takeIf { it.matches(TOKEN_FORMAT) }
        }.getOrNull()
    }

    private companion object {
        val TOKEN_FORMAT = Regex("[A-Za-z0-9_-]{43,128}")
    }
}
