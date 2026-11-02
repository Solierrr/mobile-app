package com.project.solaria_mobile.core.mock

import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.PresenceState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tempo real de demonstração, com o mesmo contrato do STOMP do messenger: retrato de presença de todos os contatos ao conectar, "digitando…"
 * e mensagens que chegam. Os contatos de [MockChatSeed.reactive] respondem sozinhos depois de cada mensagem enviada, e o
 * botão de desenvolvimento das Configurações dispara uma mensagem avulsa ([simulateIncoming]).
 */
@Singleton
class MockChatRealtime @Inject constructor(
    private val data: MockApiData,
    private val scope: CoroutineScope,
) : ChatRealtime {
    private val bus = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 64)
    private val typingState = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private var replyIndex = 0

    init {
        data.messageListener = { message -> reply(message) }
        data.conversationListener = { id -> scope.launch { publishConversation(id) } }
    }

    override val connected: Flow<Boolean> = flowOf(true)

    override val events: Flow<ChatEvent> = bus
    // Como o retrato do messenger ao assinar o canal da conta: todos os contatos de todas as conversas, de uma vez.
    override val presence: Flow<PresenceState> =
        flowOf(PresenceState(known = true, online = MockChatSeed.presence.values.flatMap { it.filterValues { online -> online }.keys }.toSet()))
    override val typingByConversation: Flow<Map<String, Set<String>>> = typingState

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
        typingState.update { it + (conversationId to it[conversationId].orEmpty() + sender) }
        delay(typingMillis)
        val message = data.receive(conversationId, sender, text)
        typingState.update { it - conversationId }
        bus.emit(ChatEvent.MessageCreated(message))
        publishConversation(conversationId)
    }

    // Como o messenger: a conversa mudou para a conta, e se saiu da lista (arquivada) o app recebe a remoção.
    private suspend fun publishConversation(id: String) {
        bus.emit(data.liveConversation(id)?.let { ChatEvent.ConversationUpdated(it) } ?: ChatEvent.ConversationRemoved(id))
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
