package com.project.solaria_mobile.core.images

import android.util.LruCache
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Baixa imagens públicas (por exemplo, do Cloudinary) com um cliente próprio, sem interceptors:
 * o `Authorization` da sessão nunca é enviado a hosts de terceiros.
 */
@Singleton
class PublicImageFetcher @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val cache = object : LruCache<String, ByteArray>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    suspend fun bytes(url: String): ByteArray? {
        if (!url.startsWith("https://")) return null
        cache.get(url)?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (response.isSuccessful) response.body.bytes() else null
                }
            }.getOrNull()
        }
        if (loaded != null) cache.put(url, loaded)
        return loaded
    }

    private companion object {
        const val CACHE_BYTES = 6 * 1024 * 1024
    }
}

@HiltViewModel
class ImageLoaderViewModel @Inject constructor(private val fetcher: PublicImageFetcher) : ViewModel() {
    suspend fun bytes(url: String): ByteArray? = fetcher.bytes(url)
}
