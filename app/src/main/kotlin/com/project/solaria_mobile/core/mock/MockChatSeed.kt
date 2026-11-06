package com.project.solaria_mobile.core.mock

import com.project.solaria_mobile.feature.communication.data.AttachmentDto
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.MessageDto
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Conversas de demonstração para exercitar o chat: histórico de meses (separadores de dia e paginação), textos curtos e longos,
 * markdown, links, imagem, documento, localização, grupo com vários remetentes, mensagens não lidas e uma conversa vazia.
 * Os horários são relativos ao momento da abertura do app.
 */
internal object MockChatSeed {
    const val SELF = "qa-mock-user"
    const val ANA = "qa-person-1"
    const val RAFAEL = "qa-person-2"
    const val MARINA = "qa-person-3"
    const val BEATRIZ = "qa-person-4"
    const val ASSISTANT = "qa-assistant"

    const val CONVERSATION_ANA = "qa-conversation-1"
    const val CONVERSATION_TEAM = "qa-conversation-2"
    const val CONVERSATION_RAFAEL = "qa-conversation-3"
    const val CONVERSATION_MARINA = "qa-conversation-4"
    const val CONVERSATION_EMPTY = "qa-conversation-5"
    const val CONVERSATION_ASSISTANT = "qa-conversation-assistant"
    const val ASSISTANT_THREAD = "qa-thread-1"

    /** Quem responde sozinho depois que a pessoa envia uma mensagem, com "digitando…" antes. */
    val reactive = mapOf(CONVERSATION_ANA to ANA, CONVERSATION_MARINA to MARINA)

    val names = mapOf(ANA to "Ana Oliveira", RAFAEL to "Rafael Costa", MARINA to "Marina Santos", BEATRIZ to "Beatriz Nunes", ASSISTANT to "Assistente", SELF to "Você")

    /** Quem está online em cada conversa, como no retrato de presença do messenger. */
    val presence: Map<String, Map<String, Boolean>> = mapOf(
        CONVERSATION_ANA to mapOf(ANA to true),
        CONVERSATION_TEAM to mapOf(ANA to true, RAFAEL to false, MARINA to true),
        CONVERSATION_RAFAEL to mapOf(RAFAEL to false),
        CONVERSATION_MARINA to mapOf(MARINA to true),
        CONVERSATION_EMPTY to mapOf(BEATRIZ to false),
    )

    private const val IMAGE_A = "mock-asset:usina-solar-panels-125f20ff9cb0.webp"
    private const val IMAGE_B = "mock-asset:residencial-solar-panels-64c4324f2d24.webp"

    private val longText = """
        Pessoal, segue o resumo completo da visita técnica de hoje, para ficar registrado e todo mundo alinhar os próximos passos.

        A cobertura comporta bem a instalação de doze módulos, mas o lado oeste tem uma caixa d'água que faz sombra depois das 15h. Conversei com o responsável e ele topou mudar a caixa de lugar na próxima reforma, o que melhora a geração no fim da tarde. Enquanto isso, a sugestão é posicionar os módulos mais ao leste e deixar a fileira de baixo para uma segunda etapa.

        Sobre a parte elétrica: o quadro de distribuição está com os disjuntores antigos e sem DPS. Para ligar o inversor com segurança, precisamos trocar o disjuntor geral, instalar o DPS e refazer o aterramento, que hoje está só com uma haste e medindo acima do aceitável. Já pedi o orçamento dos materiais e deve chegar até quarta.

        Em relação ao cronograma, estimo dois dias para estrutura e módulos, um dia para a parte elétrica e meio dia para comissionamento e teste. Se o tempo ajudar, entregamos na mesma semana. A concessionária pede a vistoria depois da instalação, e o prazo médio por aqui é de 15 a 20 dias, então já vale protocolar o pedido de acesso agora.

        Por último, combinei com o cliente que ele vai deixar o acesso ao telhado liberado e alguém para acompanhar. Qualquer dúvida, me chamem. Obrigada a todos pela ajuda hoje!
    """.trimIndent()

    private val markdown = """
        **Checklist da visita**

        - Conferir o *inversor* e a string 2
        - ~~Medir os cabos~~ já feito
        - Ler o [manual do fabricante](https://example.com/manual)

        > Atenção: usar EPI e linha de vida no telhado.

        Fórmula usada: `kWp = módulos × potência` e, em bloco:

        ```
        12 × 450 W = 5,4 kWp
        ```
    """.trimIndent()

    fun conversations(now: OffsetDateTime): List<ConversationDto> = listOf(
        ConversationDto(CONVERSATION_ANA, "USER", names.getValue(ANA), now.minusMinutes(25).toString()),
        ConversationDto(CONVERSATION_TEAM, "GROUP", "Equipe de demonstração", now.minusMinutes(50).toString()),
        ConversationDto(CONVERSATION_RAFAEL, "USER", names.getValue(RAFAEL), now.minusDays(2).toString()),
        ConversationDto(CONVERSATION_MARINA, "USER", names.getValue(MARINA), now.minusHours(3).toString()),
        ConversationDto(CONVERSATION_EMPTY, "USER", names.getValue(BEATRIZ), now.minusDays(9).toString()),
        // Sessão do assistente: a conversa guarda o identificador da sessão em `userDetails.threadId`, como o ai-assistant faz.
        ConversationDto(
            CONVERSATION_ASSISTANT, "CHAT_BOT", null, now.minusDays(1).toString(),
            userDetails = mapOf("threadId" to kotlinx.serialization.json.JsonPrimitive(ASSISTANT_THREAD)),
        ),
    )

    fun messages(now: OffsetDateTime): MutableMap<String, MutableList<MessageDto>> = mutableMapOf(
        CONVERSATION_ANA to ana(now),
        CONVERSATION_TEAM to team(now),
        CONVERSATION_RAFAEL to rafael(now),
        CONVERSATION_MARINA to marina(now),
        CONVERSATION_EMPTY to mutableListOf(),
        CONVERSATION_ASSISTANT to assistant(now),
    )

    private class Builder(private val conversationId: String, private val now: OffsetDateTime) {
        val items = mutableListOf<MessageDto>()

        fun add(sender: String, text: String, daysAgo: Long, hour: Int, minute: Int, attachment: AttachmentDto? = null, edited: Boolean = false) {
            val at = now.minusDays(daysAgo).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            val sequence = items.size + 1
            items += MessageDto(
                id = "$conversationId-m$sequence", conversationId = conversationId, senderId = sender, content = text,
                timestamp = at.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), sequence = sequence, edited = edited, senderName = names[sender], attachment = attachment,
            )
        }

        fun minutesAgo(sender: String, text: String, minutes: Long, attachment: AttachmentDto? = null) {
            val sequence = items.size + 1
            items += MessageDto(
                id = "$conversationId-m$sequence", conversationId = conversationId, senderId = sender, content = text,
                timestamp = now.minusMinutes(minutes).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), sequence = sequence, senderName = names[sender], attachment = attachment,
            )
        }
    }

    private fun ana(now: OffsetDateTime): MutableList<MessageDto> = Builder(CONVERSATION_ANA, now).apply {
        // Dois meses de conversa curta para a paginação ter o que carregar ao rolar para cima.
        val greetings = listOf("Bom dia!" to "Bom dia, tudo bem?", "Chegou o material?" to "Chegou sim, conferi agora.", "Perfeito." to "Qualquer coisa me avisa.", "Fechado." to "Combinado!")
        var day = 64L
        greetings.forEach { (a, b) ->
            repeat(4) { round ->
                add(ANA, a, day, 9 + round, 5); add(SELF, b, day, 9 + round, 12)
                day -= 1
            }
        }
        add(ANA, "Oi! Vi seu perfil e gostaria de conversar sobre a instalação.", 40, 10, 2)
        add(SELF, "Claro, pode falar!", 40, 10, 6)
        add(ANA, "ok", 39, 8, 40)
        add(SELF, "👍", 39, 8, 41)
        add(ANA, longText, 14, 18, 22)
        add(SELF, "Recebi, obrigado pelo resumo. Vou olhar o orçamento amanhã.", 14, 18, 30)
        add(ANA, markdown, 3, 11, 15)
        add(SELF, "Combinado, levo o checklist impresso.", 3, 11, 20)
        add(ANA, "Olha como ficou o telhado:", 1, 15, 3, AttachmentDto("IMAGE", url = IMAGE_A))
        add(ANA, "Segue a proposta em PDF para você revisar.", 1, 15, 8, AttachmentDto("FILE", url = "mock-file:proposta", name = "Proposta-instalacao.pdf", sizeBytes = 482_000, mimeType = "application/pdf"))
        add(SELF, "Perfeito, vou revisar hoje à noite.", 1, 15, 25, edited = true)
        minutesAgo(ANA, "O endereço da obra é esse, para o GPS:", 141)
        minutesAgo(ANA, "", 140, AttachmentDto("LOCATION", latitude = -23.5614, longitude = -46.6559, label = "Av. Paulista, 1000 — São Paulo"))
        minutesAgo(SELF, "Chego às 9h.", 135)
        minutesAgo(ANA, "Combinado! Te espero lá. Qualquer coisa, me chama por aqui mesmo.", 25)
    }.items

    private fun team(now: OffsetDateTime): MutableList<MessageDto> = Builder(CONVERSATION_TEAM, now).apply {
        add(RAFAEL, "O ambiente de testes está pronto.", 1, 9, 0)
        add(RAFAEL, "Podem validar a tela de agenda?", 1, 9, 1)
        add(MARINA, "Validando agora.", 1, 9, 6)
        add(SELF, "Eu pego a parte do mapa.", 1, 9, 9)
        add(SELF, "E depois a de projetos.", 1, 9, 9)
        minutesAgo(ANA, "Subi as fotos da obra:", 120, AttachmentDto("IMAGE", url = IMAGE_B))
        minutesAgo(RAFAEL, "Bom dia, time. A reunião de hoje passou para as 14h.", 55)
        minutesAgo(MARINA, "Anotado!", 53)
        minutesAgo(ANA, "Levo a pauta impressa.", 50)
    }.items

    private fun rafael(now: OffsetDateTime): MutableList<MessageDto> = Builder(CONVERSATION_RAFAEL, now).apply {
        add(SELF, "Rafael, conseguiu ver a string 2?", 3, 16, 10)
        add(RAFAEL, "Vi sim. Tensão ok, mas o conector estava frouxo.", 3, 16, 40)
        add(RAFAEL, "Deixei registrado aqui para o relatório:", 2, 9, 12, AttachmentDto("FILE", url = "mock-file:laudo", name = "Laudo-string-2.pdf", sizeBytes = 2_350_000, mimeType = "application/pdf"))
        add(SELF, "Valeu!", 2, 9, 30)
        add(RAFAEL, "Estou aqui, para onde mando a equipe?", 2, 9, 45)
        add(RAFAEL, "", 2, 9, 46, AttachmentDto("LOCATION", latitude = -23.5505, longitude = -46.6333, label = "Vila Mariana — São Paulo"))
    }.items

    private fun assistant(now: OffsetDateTime): MutableList<MessageDto> = Builder(CONVERSATION_ASSISTANT, now).apply {
        add(SELF, "Como funciona a visita técnica?", 1, 10, 2)
        add(ASSISTANT, "Uma **visita técnica** serve para avaliar o local antes da instalação. Em geral o profissional confere:\n\n- o estado e a orientação do telhado\n- sombras ao longo do dia\n- o quadro elétrico e o aterramento\n\nDepois da visita você recebe um orçamento com prazo e materiais.", 1, 10, 3)
        add(SELF, "E quanto tempo leva?", 1, 10, 5)
        add(ASSISTANT, "Normalmente de **30 a 60 minutos**, dependendo do tamanho da obra. Se quiser, posso ajudar a encontrar profissionais de instalação perto de você.", 1, 10, 6)
    }.items

    private fun marina(now: OffsetDateTime): MutableList<MessageDto> = Builder(CONVERSATION_MARINA, now).apply {
        minutesAgo(MARINA, "Oi! Tudo certo para a visita de amanhã?", 190)
        minutesAgo(SELF, "Tudo certo.", 185)
        minutesAgo(MARINA, "Ótimo.", 184)
    }.items
}
