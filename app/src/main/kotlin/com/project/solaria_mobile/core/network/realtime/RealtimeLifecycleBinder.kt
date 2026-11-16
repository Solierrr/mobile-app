package com.project.solaria_mobile.core.network.realtime

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.project.solaria_mobile.core.auth.TokenAuthenticator
import com.project.solaria_mobile.core.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.common.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.atomic.AtomicBoolean
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
class RealtimeLifecycleBinder internal constructor(
    private val client: RealtimeClient,
    private val mockMode: MockModeStore,
    private val tokens: TokenStore,
    private val authenticator: TokenAuthenticator,
    private val scope: CoroutineScope,
    private val renewalDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    private val clock: () -> Long,
) : DefaultLifecycleObserver {
    @Inject constructor(client: RealtimeClient, mockMode: MockModeStore, tokens: TokenStore, authenticator: TokenAuthenticator, @ApplicationScope scope: CoroutineScope) :
        this(client, mockMode, tokens, authenticator, scope, Dispatchers.IO, System::currentTimeMillis)


    /** Começa a observar o ciclo de vida do processo*/
    private val foreground = MutableStateFlow(false)
    private val started = AtomicBoolean(false)

    fun bind() {
        startWatching()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    internal fun startWatching() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            combine(foreground, mockMode.enabled, tokens.tokens) { visible, _, auth ->
                auth?.takeIf { visible && !mockMode.isEnabled() }
            }.distinctUntilChanged().collectLatest { token ->
                client.disconnect()
                if (token != null && token.accessToken.isNotBlank()) {
                    client.connect()
                    val expiry = token.accessTokenExpiresAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                    if (expiry != null) {
                        delay((expiry - clock() - 30_000L).coerceAtLeast(1_000L))
                        var retry = 1_000L
                        while (tokens.currentTokens() == token && foreground.value && !mockMode.isEnabled()) {
                            val renewed = withContext(renewalDispatcher) { authenticator.renewIfCurrent(token) }
                            if (renewed != null || tokens.currentTokens() != token) break
                            delay(retry)
                            retry = (retry * 2).coerceAtMost(60_000L)
                        }
                    }
                }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        foreground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground.value = false
        client.disconnect()
    }
}
