package com.project.solaria_mobile.feature.discovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import com.project.solaria_mobile.presentation.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeoMessage(val text: String, val mine: Boolean, val at: OffsetDateTime = OffsetDateTime.now())

/** Uma sessão do assistente geográfico, sempre ligada a um projeto do mapa. */
data class GeoSession(val id: String, val projectId: String, val projectName: String, val messages: List<GeoMessage>, val updatedAt: OffsetDateTime)

/**
 * Sessões do assistente geográfico, guardadas só enquanto o app está aberto: o ai-geographic ainda não tem contrato, então
 * as sessões são simuladas aqui. Elas aparecem na página do assistente junto das sessões do ai-assistant.
 */
@Singleton
class GeoSessionStore @Inject constructor() {
    private val mutable = MutableStateFlow<List<GeoSession>>(emptyList())
    val sessions: StateFlow<List<GeoSession>> = mutable

    fun session(id: String): GeoSession? = mutable.value.firstOrNull { it.id == id }

    /** A sessão só passa a existir (e a aparecer na lista) com a primeira mensagem. */
    fun append(sessionId: String, project: ProjectSummary, message: GeoMessage) {
        mutable.update { all ->
            val current = all.firstOrNull { it.id == sessionId }
            val next = (current ?: GeoSession(sessionId, project.id, project.name, emptyList(), message.at)).let { it.copy(messages = it.messages + message, updatedAt = message.at) }
            listOf(next) + all.filterNot { it.id == sessionId }
        }
    }
}

/**
 * Mantém uma sessão por projeto enquanto a tela do mapa estiver aberta: quem fecha a folha sem querer volta à mesma conversa
 * ao reabri-la. Ao sair do mapa este ViewModel some, e a próxima visita começa uma sessão nova (a anterior continua na lista).
 */
@HiltViewModel
class GeoAssistantViewModel @Inject constructor(private val store: GeoSessionStore, handle: SavedStateHandle) : ViewModel() {
    private val route = runCatching { handle.toRoute<AppDestination.Map>() }.getOrNull()
    private val visit = mutableMapOf<String, String>().apply {
        val project = route?.projectId
        val session = route?.assistantSession
        if (project != null && session != null) put(project, session)
    }

    /** Pede a abertura da folha assim que o projeto for selecionado, quando o mapa foi aberto a partir de uma sessão da lista. */
    val openOnStart: Boolean = route?.assistantSession != null

    val sessions: StateFlow<List<GeoSession>> = store.sessions
    private val thinkingIds = MutableStateFlow<Set<String>>(emptySet())
    val thinking: StateFlow<Set<String>> = thinkingIds

    fun sessionId(project: ProjectSummary): String = visit.getOrPut(project.id) { UUID.randomUUID().toString() }

    fun messages(sessionId: String, all: List<GeoSession>): List<GeoMessage> = all.firstOrNull { it.id == sessionId }?.messages.orEmpty()

    /** Envia a pergunta e, depois de um instante, uma resposta simulada: não há chamada de rede. */
    fun send(project: ProjectSummary, text: String) {
        val trimmed = text.trim()
        val id = sessionId(project)
        if (trimmed.isBlank() || id in thinkingIds.value) return
        store.append(id, project, GeoMessage(trimmed, mine = true))
        thinkingIds.update { it + id }
        viewModelScope.launch {
            delay(REPLY_DELAY_MILLIS)
            store.append(id, project, GeoMessage(simulatedReply(project, trimmed), mine = false))
            thinkingIds.update { it - id }
        }
    }

    private companion object {
        const val REPLY_DELAY_MILLIS = 1_800L
    }
}

private fun simulatedReply(project: ProjectSummary, question: String): String = when {
    question.contains("profission", ignoreCase = true) ->
        "Encontrei **3 profissionais** de instalação num raio de 10 km de ${project.name}:\n\n- Ana Oliveira · 4 km\n- Rafael Costa · 7 km\n- Marina Santos · 9 km\n\nQuer que eu abra o perfil de algum deles?"
    question.contains("projeto", ignoreCase = true) ->
        "Há **2 outros projetos** na mesma região, a menos de 5 km. Posso mostrar no mapa se quiser."
    question.contains("acesso", ignoreCase = true) || question.contains("entorno", ignoreCase = true) ->
        "O entorno de ${project.name} é urbano e bem servido:\n\n- acesso por via principal\n- estacionamento no quarteirão\n- sem sombra relevante de prédios vizinhos ao longo do dia"
    else -> "Posso ajudar com **profissionais próximos**, **outros projetos da região** e o **acesso e entorno** de ${project.name}. O que você quer saber?"
}
