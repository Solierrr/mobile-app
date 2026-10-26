package com.project.solaria_mobile.core.network.realtime

import com.project.solaria_mobile.core.auth.accessTokenSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompErrorFrameReceived
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * [StompConnector]
 *
 * usa biblioteca Krossbow
 * Reaproveita `OkHttpClient`
 *
 * Contrato:
 * - WebSocket
 * - `/ws` (local) | `/messaging/ws` (local com kong | QA | prod)
 * - jwt vai no header `Authorization` do frame `CONNECT`
 * - heartbeat de 10 s (Kong derruba conexão ociosa em 60 s)
 * - eventos privados em `/topic/users.{userId}.{acks|errors|conversations}`, com o `sub` do jwt
 * - servidor confere expiração do token apenas quando chega um frame
 *
 *
 * Dependência:
 * - [OkHttpClient]
 */
@Singleton
class KrossbowStompConnector @Inject constructor(
    okHttpClient: OkHttpClient,
) : StompConnector {

    private val client = StompClient(OkHttpWebSocketClient(okHttpClient)) {
        heartBeat = HeartBeat(HEARTBEAT_SECONDS.seconds, HEARTBEAT_SECONDS.seconds)
    }

    override suspend fun connect(url: String, token: String): StompLink {
        val session = try {
            client.connect(url, customStompConnectHeaders = mapOf("Authorization" to "Bearer $token"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Um frame ERROR durante o CONNECT só acontece por recusa de autenticação
            throw authFailureOf(e)?.let { StompAuthException(it, e) } ?: e
        }
        return KrossbowStompLink(session, accessTokenSubject(token).orEmpty().lowercase())
    }

    private companion object {
        const val HEARTBEAT_SECONDS = 10
    }
}

private class KrossbowStompLink(
    private val session: StompSession,
    private val userId: String,
) : StompLink {

    override fun subscribe(destination: String): Flow<String> =
        flow { emitAll(session.subscribeText(destination)) }

    override suspend fun send(destination: String, body: String) {
        session.sendText(destination, body)
    }

    override suspend fun close() {
        session.disconnect()
    }

    override suspend fun awaitLoss(): AuthFailure? =
        try {
            session.subscribeText("/topic/users.$userId.errors").collect { }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            authFailureOf(e)
        }
}

internal fun authFailureOf(error: Throwable): AuthFailure? {
    val frameError = generateSequence(error) { it.cause }
        .filterIsInstance<StompErrorFrameReceived>()
        .firstOrNull()
        ?: return null

    val text = listOfNotNull(
        frameError.frame.headers.message,
        (frameError.frame.body as? FrameBody.Text)?.text,
        frameError.message,
    ).joinToString(" ")

    return if (text.contains("SESSION_LIMIT_EXCEEDED")) {
        AuthFailure.SESSION_LIMIT_EXCEEDED
    } else {
        AuthFailure.TOKEN_REJECTED
    }
}
