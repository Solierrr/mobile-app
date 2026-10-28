package com.project.solaria_mobile.core.common

import javax.inject.Qualifier

/**
 * Qualificador do [CoroutineScope] do processo, fornecido por [CoroutineScopeModule].
 *
 * Usado por quem precisa de corrotinas que sobrevivem a uma tela, como o [StompRealtimeClient] e a [MessageOutbox].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
