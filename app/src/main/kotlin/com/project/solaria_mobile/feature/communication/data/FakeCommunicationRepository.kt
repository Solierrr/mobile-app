package com.project.solaria_mobile.feature.communication.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.domain.ServiceCallIcon

@Singleton
class FakeCommunicationRepository @Inject constructor() : CommunicationRepository {
    override val conversations = MutableStateFlow(
        listOf(
            Conversation("RD", "Ricardo Dostoiévski", "O Will Smith, aquela lá do holiud", "17:40", AvatarTone.ORANGE),
            Conversation("AM", "Alexandre de Moraes", "Tu vai apagar agora aquela mensagem?", "16:32", AvatarTone.BLUE),
            Conversation("BB", "Bruce Banner", "Cara, minha mão deu até hoje slk", "16:20", AvatarTone.GREEN),
            Conversation("AS", "Anthony Stark", "mas o thanos é um filho da mãe", "Há 20 dias", AvatarTone.ORANGE),
            Conversation("FL", "Flash", "Tá certo isso? Eu não sou da DC??", "Mês passado", AvatarTone.PINK),
            Conversation("BN", "Bolonaro", "Prossegue com a compra e atraves...", "Mês passado", AvatarTone.GREEN),
            Conversation("FR", "Flash Reverso", "Tá errado isso! Eu sou da DC cara!", "Mês passado", AvatarTone.ORANGE),
        )
    )
    private val mutableMessages = MutableStateFlow(
        listOf(
            ChatMessage("Olá! Você conseguiu revisar o cronograma do projeto?", false, "10:30"),
            ChatMessage("Consegui sim. Vou enviar os detalhes ainda hoje.", true, "10:42"),
            ChatMessage("Perfeito, obrigado!", false, "10:45"),
        )
    )
    override val messages: StateFlow<List<ChatMessage>> = mutableMessages
    override val calls = MutableStateFlow(
        listOf(
            ServiceCall("Chamado atribuído a você pelo seu supervisor", "Alocaram você e sua equipe como responsáveis pelo atendimento do chamado #1048.", ServiceCallIcon.HASH, "!!"),
            ServiceCall("Novo chamado para você!", "A empresa SolarTech abriu um chamado para manutenção em um projeto.", ServiceCallIcon.PHONE, "Novo"),
            ServiceCall("Sua segurança está frágil", "Verifique seu e-mail o mais rápido possível. Clique aqui para verificar.", ServiceCallIcon.SHIELD_ALERT),
            ServiceCall("Novo chamado para você!", "A empresa SolarTech abriu um chamado para manutenção em um projeto.", ServiceCallIcon.PHONE),
        )
    )
    private val supervisor = calls.value[0]
    private val newCall = calls.value[1]
    private val olderCall = newCall.copy(badge = null)
    private val security = calls.value[2]
    override val notifications = MutableStateFlow(
        listOf(
            "Recentes" to listOf(supervisor),
            "Há menos de 24h" to listOf(newCall, security),
            "Há 1 semana" to listOf(olderCall, security),
            "Há muito tempo..." to listOf(olderCall, olderCall, olderCall, security),
        )
    )

    override fun sendMessage(text: String) {
        if (text.isBlank()) return
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        mutableMessages.value = mutableMessages.value + ChatMessage(text, true, time)
    }
}
