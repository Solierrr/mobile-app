package com.project.solaria_mobile.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Informa se o aparelho tem acesso a internet
 *
 * - [isOnline] -> bool que representa o estado atual da conexão
 */
interface NetworkMonitor {
    /** Emite o estado atual da conexão com internet e atualiza a cada mudança */
    val isOnline: Flow<Boolean>
}

/**
 * Implementação de [NetworkMonitor] sobre o `ConnectivityManager` do Android
 *
 * Observa a conexão de internet do aparelho
 */
@Singleton
class ConnectivityManagerNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkMonitor {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    // callbackFlow funciona como adapter | callback -> flow
    override val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }
        // registra esse callback para receber eventos de conexão
        connectivityManager.registerDefaultNetworkCallback(callback)
        // emite o estado atual da conexão
        trySend(hasInternet())
        // remove o callback quando o flow for cancelado
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged() // evita emitir valores repetidos
        .conflate() // usa o valor mais recente para as classes upstream

    /**
     * Verifica se o aparelho tem acesso a internet no momento da chamada
     */
    private fun hasInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
