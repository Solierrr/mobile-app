package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.NotificationGroup
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.domain.ServiceCallIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Singleton
class FakeCommunicationRepository @Inject constructor() : CommunicationRepository {
    private val baseConversations = listOf(
        Conversation("c1", "RD", "Ricardo Dostoiévski", "O Will Smith, aquela lá do holiud", "17:40", AvatarTone.ORANGE),
        Conversation("c2", "AM", "Alexandre de Moraes", "Tu vai apagar agora aquela mensagem?", "16:32", AvatarTone.BLUE),
        Conversation("c3", "BB", "Bruce Banner", "Cara, minha mão deu até hoje slk", "16:20", AvatarTone.GREEN),
        Conversation("c4", "AS", "Anthony Stark", "mas o thanos é um filho da mãe", "Há 20 dias", AvatarTone.ORANGE),
        Conversation("c5", "FL", "Flash", "Tá certo isso? Eu não sou da DC??", "Mês passado", AvatarTone.PINK),
        Conversation("c6", "BN", "Bolonaro", "Prossegue com a compra e atraves...", "Mês passado", AvatarTone.GREEN),
        Conversation("c7", "FR", "Flash Reverso", "Tá errado isso! Eu sou da DC cara!", "Mês passado", AvatarTone.ORANGE),
    )
    private val conversations = baseConversations + baseConversations.map { it.copy(id = it.id + "-b") }

    private val messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    private val supervisor = ServiceCall("k1", "Chamado atribuído a você pelo seu supervisor", "Alocaram você e sua equipe como responsáveis pelo atendimento do chamado #1048.", ServiceCallIcon.HASH, "!!")
    private val newCall = ServiceCall("k2", "Novo chamado para você!", "A empresa SolarTech abriu um chamado para manutenção em um projeto.", ServiceCallIcon.PHONE, "Novo")
    private val security = ServiceCall("k3", "Sua segurança está frágil", "Verifique seu e-mail o mais rápido possível. Clique aqui para verificar.", ServiceCallIcon.SHIELD_ALERT)
    private val olderCall = ServiceCall("k4", newCall.title, newCall.detail, ServiceCallIcon.PHONE)
    private val baseCalls = listOf(supervisor, newCall, security, olderCall)
    private val calls = baseCalls + baseCalls.map { it.copy(id = it.id + "-b") }

    override val online: Flow<Boolean> = flowOf(true)

    override val syncResult: Flow<ApiResult<Unit>?> = flowOf(ApiResult.Success(Unit))

    override fun conversations(archived: Boolean): Flow<List<Conversation>> = flowOf(if (archived) emptyList() else conversations)

    override fun conversation(id: String): Flow<Conversation?> = flowOf(conversations.firstOrNull { it.id == id })

    override fun messages(conversationId: String): Flow<List<ChatMessage>> =
        messages.map { it[conversationId] ?: initialMessages(conversationId) }

    override suspend fun open(conversationId: String): ApiResult<Unit> =
        if (conversations.any { it.id == conversationId }) ApiResult.Success(Unit) else ApiResult.Failure(ErrorKind.NOT_FOUND)

    override fun refresh() = Unit

    override suspend fun refreshNames(conversationId: String?) = Unit

    override suspend fun sendMessage(conversationId: String, text: String): ApiResult<Unit> {
        if (text.isBlank()) return ApiResult.Failure(ErrorKind.VALIDATION, setOf("text"))
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val history = messagesOf(conversationId)
        val sequence = history.size + 1
        val message = ChatMessage("$conversationId-$sequence", text, true, time, sequence)
        messages.update { it + (conversationId to (history + message)) }
        return ApiResult.Success(Unit)
    }

    override suspend fun retryMessage(messageId: String) = Unit

    override suspend fun discardMessage(messageId: String) = Unit

    override suspend fun markRead(conversationId: String) = Unit

    override suspend fun setMuted(conversationId: String, muted: Boolean): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun setArchived(conversationId: String, archived: Boolean): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun editMessage(messageId: String, text: String): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun deleteMessage(messageId: String): ApiResult<Unit> = ApiResult.Success(Unit)

    private fun messagesOf(conversationId: String) = messages.value[conversationId] ?: initialMessages(conversationId)

    override suspend fun calls(): ApiResult<List<ServiceCall>> = ApiResult.Success(calls)

    override suspend fun call(id: String): ApiResult<ServiceCall> =
        calls.firstOrNull { it.id == id }?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ErrorKind.NOT_FOUND)

    override suspend fun notifications(): ApiResult<List<NotificationGroup>> = ApiResult.Success(
        listOf(
            NotificationGroup("Recentes", listOf(supervisor)),
            NotificationGroup("Há menos de 24h", listOf(newCall, security)),
            NotificationGroup("Há 1 semana", listOf(olderCall, security)),
            NotificationGroup("Há muito tempo...", listOf(olderCall, olderCall, olderCall, security)),
        )
    )

    private fun initialMessages(conversationId: String) = listOf(
        ChatMessage("$conversationId-1", "Olá! Você conseguiu revisar o cronograma do projeto?", false, "10:30", 1),
        ChatMessage("$conversationId-2", "Consegui sim. Vou enviar os detalhes ainda hoje.", true, "10:42", 2),
        ChatMessage("$conversationId-3", "Perfeito, obrigado!", false, "10:45", 3),
    )
}
