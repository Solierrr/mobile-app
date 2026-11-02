package com.project.solaria_mobile.core.mock

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

class MockImage(val bytes: ByteArray, val mime: String)

/**
 * Fotos de demonstração vindas do acervo de mocks, empacotadas só no flavor qa (`assets/mock-images`).
 * Cada arquivo é `<grupo>-<id do acervo>.webp`; os projetos de demonstração se revezam entre os grupos.
 */
@Singleton
class MockImageCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    fun forProject(index: Int): List<MockImage> = runCatching {
        val group = GROUPS[index.mod(GROUPS.size)]
        context.assets.list(DIRECTORY).orEmpty()
            .filter { it.startsWith("$group-") }
            .sorted()
            .map { name -> MockImage(context.assets.open("$DIRECTORY/$name").use { it.readBytes() }, "image/webp") }
    }.getOrDefault(emptyList())

    /** Arquivo avulso do acervo (foto de pessoa ou de empresa), ou `null` fora do flavor qa. */
    fun asset(name: String): MockImage? = runCatching {
        MockImage(context.assets.open("$DIRECTORY/$name").use { it.readBytes() }, "image/webp")
    }.getOrNull()

    companion object {
        /** Prefixo das URLs de demonstração: `mock-asset:<arquivo>`, resolvidas localmente em vez de baixadas. */
        const val URL_PREFIX = "mock-asset:"
        private const val DIRECTORY = "mock-images"
        val GROUPS = listOf("usina", "residencial", "obra")
    }
}
