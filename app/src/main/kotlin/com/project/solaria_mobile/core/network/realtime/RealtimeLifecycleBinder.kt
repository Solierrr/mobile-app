package com.project.solaria_mobile.core.network.realtime

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observa o ciclo de vida do processo
 *
 * conecta|desconecta o tempo real se o app está em primeiro ou segundo plano
 *
 *
 * Depende de:
 * - [RealtimeClient] -> recebe `connect()` e `disconnect()`.=
 *
 * Principais operações:
 * - [bind] -> começa a observar o ciclo de vida do processo
 */
@Singleton
class RealtimeLifecycleBinder @Inject constructor(
    private val client: RealtimeClient,
) : DefaultLifecycleObserver {

    /** Começa a observar o ciclo de vida do processo*/
    fun bind() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        client.connect()
    }

    override fun onStop(owner: LifecycleOwner) {
        client.disconnect()
    }
}
