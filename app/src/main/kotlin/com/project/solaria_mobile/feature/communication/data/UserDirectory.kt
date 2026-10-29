package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.getOrNull
import com.project.solaria_mobile.core.database.ConversationDao
import com.project.solaria_mobile.core.database.MessageDao
import com.project.solaria_mobile.core.database.UserDao
import com.project.solaria_mobile.core.database.UserEntity
import com.project.solaria_mobile.core.database.isChatbot
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.profile.data.ProfileApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Nomes e avatares das pessoas que aparecem nas conversas, guardados na tabela `users`.
 *
 * Com internet, consulta o api-core e grava a pessoa só se ela é nova ou mudou. Sem internet, a consulta falha em
 * silêncio e a tela usa o que já está no banco. O diretório não consulta a mesma pessoa de novo antes de 30 s, porque a
 * lista, o chat e a sincronização pedem os mesmos nomes quase ao mesmo tempo. O nome serve para a conversa direta (o outro participante) e para as
 * bolhas de grupo (quem enviou). O avatar é guardado, mas a tela ainda mostra só as iniciais.
 *
 * Depende de:
 * - [ProfileApi] -> `GET /api/users/auth/{authId}/summary` do api-core.
 * - [ApiCallExecutor] -> executa a chamada e devolve o resultado, sem lançar erro de rede.
 * - [UserDao] -> tabela `users`.
 * - [ConversationDao] e [MessageDao] -> dizem quem aparece em cada tela.
 *
 * Principais operações:
 * - [observe] -> acompanha as pessoas guardadas, pelo `authId` em minúsculas.
 * - [refreshFor] -> atualiza do api-core as pessoas que uma tela vai mostrar.
 * - [forget] -> esquece quando cada pessoa foi consultada. Chamado no logout.
 */
@Singleton
class UserDirectory @Inject constructor(
    private val api: ProfileApi,
    private val executor: ApiCallExecutor,
    private val users: UserDao,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
) {
    private val askedAt = ConcurrentHashMap<String, Long>()

    // Relógio da regra dos 30 s. Só os testes trocam.
    internal var now: () -> Long = System::currentTimeMillis

    /** Emite as pessoas guardadas, indexadas pelo `authId` em minúsculas, a cada mudança. */
    fun observe(): Flow<Map<String, UserEntity>> = users.observeAll().map { rows -> rows.associateBy { it.authId } }

    /**
     * Atualiza do api-core as pessoas que a tela vai mostrar, uma chamada por pessoa.
     *
     * Sem [conversationId], atualiza o outro participante de cada conversa direta, para a lista. Com ele, atualiza
     * os participantes e quem enviou mensagens na conversa, para as bolhas. Conversa com o chatbot é ignorada.
     *
     * shortcut: chamadas em série, o que basta para grupos pequenos. Limitar a simultaneidade se grupos grandes
     * ficarem lentos.
     *
     * @param conversationId conversa aberta, ou `null` para a lista.
     * @param me `sub` do usuário logado em minúsculas. Ele mesmo não é consultado.
     */
    suspend fun refreshFor(conversationId: String?, me: String?) {
        val ids = if (conversationId == null) {
            conversations.all().filter { it.conversationType == DIRECT_TYPE }.flatMap { it.peerIds(me) }
        } else {
            val conversation = conversations.find(conversationId)?.takeUnless { it.isChatbot } ?: return
            conversation.peerIds(me) + messages.senders(conversationId).filterNot { it.equals(me, ignoreCase = true) }
        }
        for (id in ids.map { it.lowercase() }.distinct()) refresh(id)
    }

    private suspend fun refresh(authId: String) {
        val last = askedAt[authId]
        if (last != null && now() - last < MIN_INTERVAL_MILLIS) return
        val summary = executor.execute { api.summary(authId) }.getOrNull() ?: return
        askedAt[authId] = now()
        val name = summary.username?.takeIf { it.isNotBlank() } ?: return
        users.save(UserEntity(authId, name, summary.avatar?.takeIf { it.isNotBlank() }))
    }

    /** Esquece quando cada pessoa foi consultada, para o próximo login refazer as consultas sem esperar os 30 s. */
    fun forget() {
        askedAt.clear()
    }

    private companion object {
        /** Intervalo mínimo entre duas consultas da mesma pessoa: 30 s. */
        const val MIN_INTERVAL_MILLIS = 30_000L
    }
}
