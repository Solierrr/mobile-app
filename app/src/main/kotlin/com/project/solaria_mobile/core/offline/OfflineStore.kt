package com.project.solaria_mobile.core.offline

import javax.inject.Inject
import javax.inject.Singleton

/** Ponto único para apagar os dados guardados (texto e imagens) quando a conta muda. */
@Singleton
class OfflineStore @Inject constructor(
    private val reader: OfflineReader,
    private val images: ImageDiskCache,
) {
    suspend fun clearAll() {
        reader.clear()
        images.clear()
    }
}
