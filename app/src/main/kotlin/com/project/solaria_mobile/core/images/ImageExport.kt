package com.project.solaria_mobile.core.images

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.project.solaria_mobile.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class ImageFormat(val mime: String, val extension: String)

/** Descobre o formato pelos primeiros bytes; o que não for reconhecido sai como JPEG. */
private fun formatOf(bytes: ByteArray): ImageFormat = when {
    bytes.size > 3 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> ImageFormat("image/png", "png")
    bytes.size > 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> ImageFormat("image/webp", "webp")
    else -> ImageFormat("image/jpeg", "jpg")
}

/** Salva a imagem na galeria do aparelho, na pasta com o nome do app. Não pede permissão (Android 10 ou mais). */
suspend fun saveImageToGallery(context: Context, bytes: ByteArray, baseName: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val format = formatOf(bytes)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$baseName.${format.extension}")
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + context.getString(R.string.app_name))
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
        val written = resolver.openOutputStream(uri)?.use { it.write(bytes); true } ?: false
        if (!written) {
            resolver.delete(uri, null, null)
            return@runCatching false
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        true
    }.getOrDefault(false)
}

/** Abre a folha de compartilhamento do Android com a imagem; o arquivo temporário fica só no cache do app. */
suspend fun shareImage(context: Context, bytes: ByteArray, baseName: String): Boolean {
    val format = formatOf(bytes)
    val uri = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, "shared").apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            val file = File(directory, "$baseName.${format.extension}").apply { writeBytes(bytes) }
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }.getOrNull()
    } ?: return false
    val send = Intent(Intent.ACTION_SEND).apply {
        type = format.mime
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(Intent.createChooser(send, "Compartilhar imagem")) }.isSuccess
}
