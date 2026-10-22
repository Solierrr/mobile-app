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

    private val messages = mutableMapOf<String, List<ChatMessage>>()

    private val supervisor = ServiceCall("k1", "Chamado atribuído a você pelo seu supervisor", "Alocaram você e sua equipe como responsáveis pelo atendimento do chamado #1048.", ServiceCallIcon.HASH, "!!")
    private val newCall = ServiceCall("k2", "Novo chamado para você!", "A empresa SolarTech abriu um chamado para manutenção em um projeto.", ServiceCallIcon.PHONE, "Novo")
    private val security = ServiceCall("k3", "Sua segurança está frágil", "Verifique seu e-mail o mais rápido possível. Clique aqui para verificar.", ServiceCallIcon.SHIELD_ALERT)
    private val olderCall = ServiceCall("k4", newCall.title, newCall.detail, ServiceCallIcon.PHONE)
    private val baseCalls = listOf(supervisor, newCall, security, olderCall)
    private val calls = baseCalls + baseCalls.map { it.copy(id = it.id + "-b") }

    override suspend fun conversations(): ApiResult<List<Conversation>> = ApiResult.Success(conversations)

    override suspend fun conversation(id: String): ApiResult<Conversation> =
        conversations.firstOrNull { it.id == id }?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ErrorKind.NOT_FOUND)

    override suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>> =
        ApiResult.Success(messages.getOrPut(conversationId) { initialMessages() })

    override suspend fun sendMessage(conversationId: String, text: String): ApiResult<ChatMessage> {
        if (text.isBlank()) return ApiResult.Failure(ErrorKind.VALIDATION, setOf("text"))
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val message = ChatMessage(text, true, time)
        messages[conversationId] = messages.getOrPut(conversationId) { initialMessages() } + message
        return ApiResult.Success(message)
    }

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

    private fun initialMessages() = listOf(
        ChatMessage("Olá! Você conseguiu revisar o cronograma do projeto?", false, "10:30"),
        ChatMessage("Consegui sim. Vou enviar os detalhes ainda hoje.", true, "10:42"),
        ChatMessage("Perfeito, obrigado!", false, "10:45"),
    )
}
