package com.project.solaria_mobile.core.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
    if (bytes.isEmpty() || bytes.size > MAX_PICKED_IMAGE_BYTES) return null
    return upright(bytes, type)
}

private const val MAX_EDGE_PIXELS = 4096
private const val JPEG_QUALITY = 92

/**
 * Fotos de câmera guardam a rotação no EXIF em vez de girar os pixels, e o servidor e os visualizadores ignoram essa marca,
 * então a foto aparece deitada ou de cabeça para baixo. Aqui os pixels são girados (e espelhados, quando for o caso)
 * conforme o EXIF e o JPEG é regravado já na posição certa. Sem rotação a aplicar, ou em qualquer falha, devolve o original.
 */
private fun upright(bytes: ByteArray, type: String): Pair<ByteArray, String> {
    if (type != "image/jpeg") return bytes to type
    return runCatching {
        val orientation = ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bytes to type
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE_PIXELS || bounds.outHeight / sample > MAX_EDGE_PIXELS) sample *= 2
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return bytes to type
        val turned = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (turned !== source) source.recycle()
        val output = ByteArrayOutputStream()
        turned.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        turned.recycle()
        val result = output.toByteArray()
        if (result.isEmpty() || result.size > MAX_PICKED_IMAGE_BYTES) bytes to type else result to type
    }.getOrDefault(bytes to type)
}
