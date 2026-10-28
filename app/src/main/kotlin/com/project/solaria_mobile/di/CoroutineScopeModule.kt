package com.project.solaria_mobile.di

import com.project.solaria_mobile.core.common.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Módulo Hilt que fornece o [CoroutineScope] do processo, marcado com [ApplicationScope].
 *
 * O escopo usa `SupervisorJob`: a falha de uma corrotina filha não cancela as irmãs.
 *
 * Principais operações:
 * - [provideApplicationScope] -> fornece o escopo único do processo.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {
    /**
     * Fornece o escopo do processo. As telas não o cancelam: ele vive enquanto o processo existir.
     *
     * @return escopo único, injetado com [ApplicationScope].
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
