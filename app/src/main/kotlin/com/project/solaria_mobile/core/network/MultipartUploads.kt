package com.project.solaria_mobile.core.network

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.IOException

/**
 * abre um [uri] e transforma em [RequestBody] para upload multipart em request HTTP
 *
 * @property contentResolver usado para abrir o [uri]
 * @property uri origem do conteúdo
 * @property mediaType tipo do arquivo | `null` se desconhecido
 */
class UriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType?,
) : RequestBody() {

    override fun contentType(): MediaType? = mediaType

    /**
     * tenta obter o tamanho do arquivo | se não retorna -1
     */
    override fun contentLength(): Long =
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L

    override fun writeTo(sink: BufferedSink) {
        val input = contentResolver.openInputStream(uri)
            ?: throw IOException("Não foi possível abrir $uri")
        input.source().use { sink.writeAll(it) }
    }
}

/**
 * transforma um [File] em parte multipart para upload em request HTTP
 *
 * @param partName nome do campo esperado pela API
 * @param mimeType tipo do conteúdo `image/jpeg`
 * @param fileName nome do arquivo enviado | padrão = nome do próprio arquivo
 */
fun File.toMultipartPart(
    partName: String,
    mimeType: String,
    fileName: String = name,
): MultipartBody.Part = MultipartBody.Part.createFormData(
    partName,
    fileName,
    asRequestBody(mimeType.toMediaTypeOrNull()),
)

/**

 * transforma um [Uri] em parte multipart para upload em request HTTP
 *
 * @param partName nome do campo esperado pela API
 * @param uri origem do arquivo
 * @param fileName nome do arquivo enviado
 * @param mimeType tipo do conteúdo, por exemplo `image/jpeg`
 */
fun ContentResolver.toMultipartPart(
    partName: String,
    uri: Uri,
    fileName: String,
    mimeType: String,
): MultipartBody.Part = MultipartBody.Part.createFormData(
    partName,
    fileName,
    UriRequestBody(this, uri, mimeType.toMediaTypeOrNull()),
)
