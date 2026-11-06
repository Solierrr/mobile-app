package com.project.solaria_mobile.core.mock

import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.MessageDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Tempo real de demonstração, com o mesmo contrato do STOMP do messenger: retrato de presença ao abrir a conversa, "digitando…"
 * e mensagens que chegam. Os contatos de [MockChatSeed.reactive] respondem sozinhos depois de cada mensagem enviada, e o
 * botão de desenvolvimento das Configurações dispara uma mensagem avulsa ([simulateIncoming]).
 */
@Singleton
class MockChatRealtime @Inject constructor(
    private val data: MockApiData,
    private val scope: CoroutineScope,
) : ChatRealtime {
    private val bus = MutableSharedFlow<Pair<String, ChatEvent>>(extraBufferCapacity = 64)
    private var replyIndex = 0

    init {
        data.messageListener = { message -> reply(message) }
    }

    override val connected: Flow<Boolean> = flowOf(true)

    override fun conversationEvents(conversationId: String): Flow<ChatEvent> = flow {
        emit(ChatEvent.PresenceSnapshot(data.presence(conversationId)))
        bus.filter { it.first == conversationId }.map { it.second }.collect { emit(it) }
    }

    override suspend fun typing(conversationId: String, started: Boolean) = Unit

    private fun reply(sent: MessageDto) {
        val partner = MockChatSeed.reactive[sent.conversationId] ?: return
        scope.launch { deliver(sent.conversationId, partner, REPLIES[replyIndex++ % REPLIES.size], typingMillis = 2_200, waitMillis = 1_200) }
    }

    /** Uma mensagem de outra pessoa, com "digitando…" antes, na primeira conversa de contato que responde sozinho. */
    fun simulateIncoming(conversationId: String = MockChatSeed.CONVERSATION_ANA) {
        val sender = MockChatSeed.reactive[conversationId] ?: MockChatSeed.ANA
        scope.launch { deliver(conversationId, sender, INCOMING[replyIndex++ % INCOMING.size], typingMillis = 1_500, waitMillis = 300) }
    }

    private suspend fun deliver(conversationId: String, sender: String, text: String, typingMillis: Long, waitMillis: Long) {
        delay(waitMillis)
        bus.emit(conversationId to ChatEvent.Typing(sender, true))
        delay(typingMillis)
        val message = data.receive(conversationId, sender, text)
        bus.emit(conversationId to ChatEvent.Typing(sender, false))
        bus.emit(conversationId to ChatEvent.MessageCreated(message))
    }

    private companion object {
        val REPLIES = listOf(
            "Entendido!",
            "Perfeito, obrigada.",
            "**Combinado!** Te mando os detalhes em instantes.",
            "Vou verificar aqui e já te respondo, só um minuto.",
            "Boa! Isso resolve o que faltava para fechar a etapa da instalação. Assim que o material chegar eu aviso e a gente marca a visita para a próxima semana.",
        )
        val INCOMING = listOf(
            "Oi, conseguiu ver a mensagem de ontem?",
            "Lembrete: a vistoria é amanhã às *9h*.\n\n- Levar o laudo\n- Levar o EPI",
            "Acabei de chegar na obra. Está tudo pronto por aqui. 🚀",
        )
    }
}
