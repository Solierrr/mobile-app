package com.project.solaria_mobile.core.offline

/** Dado local de uma conta que precisa ser apagado no logout, junto com o cache. */
fun interface LocalDataCleaner {
    suspend fun clear()
}
