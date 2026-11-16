package com.project.solaria_mobile.core.offline

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class OfflineStore @Inject constructor(
    private val reader: OfflineReader,
    private val images: ImageDiskCache,
    private val cleaners: Set<@JvmSuppressWildcards LocalDataCleaner>,
) {
    suspend fun clearAll() {
        reader.clear()
        withContext(Dispatchers.IO) { images.clear() }
        cleaners.forEach { it.clear() }
    }
}
