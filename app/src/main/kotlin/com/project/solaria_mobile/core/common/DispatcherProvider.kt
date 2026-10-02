package com.project.solaria_mobile.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * organiza as corrotinas para para decidir em qual thread cada trabalho vai rodar
 *
 * Principais dispatchers:
 * - [main] -> trabalho ligado à interface
 * - [io] -> operação bloquenate, como request http ou acesso ao BD
 * - [default] -> trabalho pesado como processamento de imagem
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

/** Implementação */
@Singleton
class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}
