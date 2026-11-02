package com.project.solaria_mobile.core.images

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

private const val MAX_PICKED_IMAGE_BYTES = 8L * 1024 * 1024
private val ACCEPTED_IMAGE_TYPES = setOf("image/jpeg", "image/png")

/**
 * Lê a imagem escolhida no seletor do sistema se for JPG ou PNG de até 8 MB, os limites aceitos pelo api-core para fotos
 * de projeto e de perfil. Devolve os bytes e o tipo, ou nulo quando o servidor recusaria o arquivo.
 */
fun readPickedImage(context: Context, uri: Uri): Pair<ByteArray, String>? {
    val resolver = context.contentResolver
    val type = resolver.getType(uri)?.takeIf { it in ACCEPTED_IMAGE_TYPES } ?: return null
    val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
    }
    if (size != null && size > MAX_PICKED_IMAGE_BYTES) return null
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return if (bytes.isEmpty() || bytes.size > MAX_PICKED_IMAGE_BYTES) null else bytes to type
}
