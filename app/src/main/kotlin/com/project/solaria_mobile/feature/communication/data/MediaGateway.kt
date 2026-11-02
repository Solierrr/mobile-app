package com.project.solaria_mobile.feature.communication.data

import android.content.Context
import android.net.Uri
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.source

/** Permissão de envio que o messenger dá para um arquivo: a URL do bucket, o método e os cabeçalhos que a assinatura exige. */
data class MediaUploadTarget(val url: String, val method: String, val headers: Map<String, String>)

/**
 * A parte da mídia que mexe no aparelho e fala direto com o bucket. Os bytes nunca passam pela API: o envio vai para a URL
 * assinada, e o que se baixa fica em arquivo do app.
 */
interface MediaGateway {
    /** Envia os bytes de [uri] para [target], sem o `Authorization` da API (a assinatura já autoriza). */
    suspend fun upload(target: MediaUploadTarget, uri: String, mimeType: String, sizeBytes: Long): Boolean

    /** Grava o conteúdo baixado na pasta do app e devolve o URI do arquivo, ou nulo se não deu para gravar. */
    suspend fun save(mediaId: String, name: String?, body: ResponseBody): String?
}

/** O modo de demonstração devolve este prefixo em `upload.url`: o envio é aceito sem sair do aparelho. */
const val MOCK_UPLOAD_PREFIX = "mock://upload/"

@Singleton
class AndroidMediaGateway @Inject constructor(@ApplicationContext private val context: Context) : MediaGateway {
    // Cliente próprio, sem os interceptores da API: o token do usuário não pode ir para o host do bucket.
    private val client = OkHttpClient()

    override suspend fun upload(target: MediaUploadTarget, uri: String, mimeType: String, sizeBytes: Long): Boolean {
        if (target.url.startsWith(MOCK_UPLOAD_PREFIX)) return true
        return withContext(Dispatchers.IO) {
            val body = object : RequestBody() {
                override fun contentType() = (target.headers["Content-Type"] ?: mimeType).toMediaTypeOrNull()
                override fun contentLength() = sizeBytes
                override fun writeTo(sink: BufferedSink) {
                    val input = context.contentResolver.openInputStream(Uri.parse(uri)) ?: throw IOException("arquivo indisponível")
                    input.use { sink.writeAll(it.source()) }
                }
            }
            val request = Request.Builder().url(target.url).method(target.method.uppercase(), body)
                .apply { target.headers.forEach { (name, value) -> header(name, value) } }.build()
            // shortcut: o envio bloqueia a thread de IO até acabar e não é cancelado com a corrotina; trocar por call.cancel() se virar problema.
            runCatching { client.newCall(request).execute().use { it.isSuccessful } }.getOrDefault(false)
        }
    }

    override suspend fun save(mediaId: String, name: String?, body: ResponseBody): String? = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "chat-media").apply { mkdirs() }
        val extension = name?.substringAfterLast('.', "")?.filter(Char::isLetterOrDigit)?.take(8)?.takeIf { it.isNotEmpty() }
        val file = File(directory, mediaId.filter { it.isLetterOrDigit() || it == '-' || it == '_' } + (extension?.let { ".$it" } ?: ""))
        runCatching {
            body.use { source -> file.outputStream().use { out -> source.byteStream().copyTo(out) } }
            Uri.fromFile(file).toString()
        }.getOrNull()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds abstract fun gateway(implementation: AndroidMediaGateway): MediaGateway
}
