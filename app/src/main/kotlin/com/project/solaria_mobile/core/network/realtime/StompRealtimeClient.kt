package com.project.solaria_mobile.core.network.realtime

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ApplicationScope
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.CredentialProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementação de [RealtimeClient]
 *
 * mantém **uma** conexão STOMP
 *
 * refaz quando perde conexão, e não tenta quando a causa é auth
 *
 *
 * Depende de:
 * - [StompConnector] -> abre a sessão STOMP
 * - [RealtimeEndpoint] -> URL do WebSocket
 * - [CredentialProvider] -> token de acesso enviado no `CONNECT`
 *
 * Principais operações:
 * - [connect] | [disconnect]->— abre/fecha a sessão STOMP
 * - [subscribe] -> observa mensagens de um destino STOMP
 * - [send] —> publica na sessão ativa
 *
 */
@Singleton
class StompRealtimeClient @Inject constructor(
    private val connector: StompConnector,
    private val endpoint: RealtimeEndpoint,
    private val credentials: CredentialProvider,
    @ApplicationScope private val scope: CoroutineScope,
) : RealtimeClient {

    private val _state = MutableStateFlow<RealtimeState>(RealtimeState.Disconnected)
    override val state: StateFlow<RealtimeState> = _state.asStateFlow()

    private val link = MutableStateFlow<StompLink?>(null)

    private val lock = Any()
    private var job: Job? = null

    override fun connect() {
        synchronized(lock) {
            if (job?.isActive == true) return
            job = scope.launch { superviseConnection() }
        }
    }

    override fun disconnect() {
        synchronized(lock) {
            job?.cancel()
            job = null
            _state.value = RealtimeState.Disconnected
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun subscribe(destination: String): Flow<String> =
        link.flatMapLatest { current ->
            current?.subscribe(destination)?.catch { } ?: emptyFlow()
        }

    override suspend fun send(destination: String, body: String): ApiResult<Unit> {
        val current = link.value
            ?: return ApiResult.Failure(ErrorKind.CONNECTIVITY)
        return try {
            current.send(destination, body)
            ApiResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Failure(ErrorKind.CONNECTIVITY)
        }
    }

    private suspend fun superviseConnection() {
        val self = currentCoroutineContext().job

        fun publish(newState: RealtimeState) {
            synchronized(lock) { if (self.isActive) _state.value = newState }
        }

        var attempt = 0
        while (currentCoroutineContext().isActive) {
            val url = endpoint.url()
            if (url == null) {
                publish(RealtimeState.NotConfigured)
                return
            }
            val token = credentials.accessToken()
            if (token.isNullOrBlank()) {
                publish(RealtimeState.AuthFailed(AuthFailure.NO_TOKEN))
                return
            }

            publish(if (attempt == 0) RealtimeState.Connecting else RealtimeState.Reconnecting(attempt))

            val opened = try {
                connector.connect(url, token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: StompAuthException) {
                publish(RealtimeState.AuthFailed(e.reason))
                return
            } catch (e: Exception) {
                null
            }

            if (opened == null) {
                attempt++
                publish(RealtimeState.Reconnecting(attempt))
                delay(backoffMillis(attempt))
                continue
            }

            attempt = 0
            link.value = opened
            publish(RealtimeState.Connected)

            val authFailure = try {
                opened.awaitLoss()
            } finally {
                link.value = null
                withContext(NonCancellable) { runCatching { opened.close() } }
            }

            if (authFailure != null) {
                publish(RealtimeState.AuthFailed(authFailure))
                return
            }

            attempt = 1
            publish(RealtimeState.Reconnecting(attempt))
            delay(backoffMillis(attempt))
        }
    }

    // 1s, 2s, 4s, 8s, 16s e 30 s
    private fun backoffMillis(attempt: Int): Long =
        (BASE_BACKOFF_MILLIS shl (attempt - 1).coerceAtMost(5)).coerceAtMost(MAX_BACKOFF_MILLIS)

    private companion object {
        const val BASE_BACKOFF_MILLIS = 1_000L
        const val MAX_BACKOFF_MILLIS = 30_000L
    }
}
