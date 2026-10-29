package com.project.solaria_mobile.core.database

/**
 * Apaga todos os dados locais do aparelho.
 *
 * Existe como interface para que o código de sincronização e os testes JVM não dependam do Room. A implementação de
 * produção é criada pelo [DatabaseModule] e chama `clearAllTables()`, o que inclui a fila de envio.
 *
 * Principais operações:
 * - [wipe] -> apaga todas as tabelas do banco.
 */
fun interface LocalDataWiper {
    /** Apaga todas as tabelas do banco, inclusive as mensagens que ainda esperam envio. */
    suspend fun wipe()
}
