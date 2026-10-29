package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.MessageDao
import com.project.solaria_mobile.core.database.MessageEntity
import com.project.solaria_mobile.core.database.isChatbot
import com.project.solaria_mobile.core.database.mergeMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Único caminho de entrada das mensagens confirmadas pelo servidor, e única fonte de leitura delas.
 *
 * Recebe o que vem do histórico, do catch-up, dos eventos ao vivo e da confirmação de envio. Mensagem de conversa
 * humana vai para o Room pela regra de versão maior vence. Mensagem de conversa com o chatbot não é guardada. Ela fica só
 * em memória, enquanto o processo vive, e some no logout ou quando o app fecha.
 *
 * Depende de:
 * - [MessageDao] -> tabela `messages`.
 * - [ConversationDao] -> tipo da conversa e prévia da lista.
 *
 * Principais operações:
 * - [apply] -> grava um lote de mensagens vindas do servidor.
 * - [observe] -> acompanha as mensagens de uma conversa, do Room ou da memória.
 * - [forget] -> esquece as mensagens do chatbot guardadas em memória.
 */
@Singleton
class MessageStore @Inject constructor(
    private val messages: MessageDao,
    private val conversations: ConversationDao,
) {
    // shortcut: sem limite de mensagens do chatbot em memória (são textos curtos e somem ao fechar o app), limitar se uma conversa passar de alguns milhares
    private val chatbot = MutableStateFlow<Map<String, Map<String, MessageEntity>>>(emptyMap())

    /**
     * Grava as mensagens do lote que são mais novas que as guardadas.
     *
     * Depois de gravar, corrige a prévia da lista quando a última mensagem de uma conversa mudou, para uma exclusão
     * apagar o texto também da prévia.
     *
     * @param incoming mensagens como vieram do servidor.
     */
    suspend fun apply(incoming: List<MessageDto>) {
        if (incoming.isEmpty()) return
        val chatbots = conversations.chatbotIds().toSet()
        val (bot, human) = incoming.map { it.toEntity() }.partition { it.conversationId in chatbots }
        if (human.isNotEmpty()) refreshPreviews(messages.upsert(human))
        if (bot.isNotEmpty()) remember(bot)
    }

    /**
     * Acompanha as mensagens de uma conversa em ordem de `sequence`.
     *
     * Lê do Room, ou da memória se a conversa é com o chatbot.
     *
     * @param conversationId conversa a acompanhar.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(conversationId: String): Flow<List<MessageEntity>> =
        conversations.observe(conversationId)
            .map { it?.isChatbot == true }
            .distinctUntilChanged()
            .flatMapLatest { isChatbot ->
                if (isChatbot) {
                    chatbot.map { byConversation -> byConversation[conversationId].orEmpty().values.sortedBy { it.sequence } }
                } else {
                    messages.observe(conversationId)
                }
            }

    /** Esquece as mensagens do chatbot que estão em memória. Chamado no logout, junto com a limpeza do banco. */
    fun forget() {
        chatbot.value = emptyMap()
    }

    private suspend fun refreshPreviews(applied: List<MessageEntity>) {
        for (last in applied.groupBy { it.conversationId }.values.map { group -> group.maxBy { it.sequence } }) {
            conversations.refreshPreview(last.conversationId, last.id, last.content, last.deleted)
        }
    }

    private fun remember(incoming: List<MessageEntity>) {
        chatbot.update { current ->
            val changed = HashMap<String, MutableMap<String, MessageEntity>>()
            for (message in incoming) {
                val byId = changed.getOrPut(message.conversationId) { current[message.conversationId].orEmpty().toMutableMap() }
                byId[message.id] = mergeMessage(byId[message.id], message) ?: continue
            }
            current + changed
        }
    }
}
