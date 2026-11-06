package com.project.solaria_mobile.core.mock

import com.project.solaria_mobile.feature.communication.data.NotificationDto
import com.project.solaria_mobile.feature.communication.data.ServiceCallDto
import com.project.solaria_mobile.feature.profile.data.UserDto
import com.project.solaria_mobile.feature.project.data.ProjectDto
import com.project.solaria_mobile.feature.project.data.ProjectLocationDto
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Volume extra de demonstração: mais pessoas (com foto), projetos, chamados e notificações, para as listas, a busca, a paginação
 * e os filtros terem o que mostrar. Soma-se ao conjunto pequeno de [MockApiData]; os ids começam em `qa-person-5`,
 * `qa-project-demo-1` e `qa-service-demo-1` para não colidir com os originais nem com o que a pessoa criar durante o uso.
 */
internal object MockExtraSeed {
    private class Person(val slug: String, val name: String)

    private val roster = listOf(
        Person("carlos-mendes", "Carlos Mendes"), Person("juliana-rocha", "Juliana Rocha"), Person("pedro-lima", "Pedro Lima"),
        Person("camila-duarte", "Camila Duarte"), Person("thiago-barros", "Thiago Barros"), Person("fernanda-alves", "Fernanda Alves"),
        Person("lucas-ribeiro", "Lucas Ribeiro"), Person("patricia-gomes", "Patrícia Gomes"), Person("andre-martins", "André Martins"),
        Person("larissa-pinto", "Larissa Pinto"), Person("bruno-carvalho", "Bruno Carvalho"), Person("renata-freitas", "Renata Freitas"),
        Person("gustavo-moreira", "Gustavo Moreira"), Person("isabela-teixeira", "Isabela Teixeira"), Person("diego-nogueira", "Diego Nogueira"),
        Person("vanessa-araujo", "Vanessa Araújo"), Person("felipe-cardoso", "Felipe Cardoso"),
    )

    /** Primeiro número livre: `qa-person-1` a `qa-person-4` já existem (Ana, Rafael, Marina e Beatriz). */
    private const val FIRST_PERSON = 5

    val personIds: List<String> = roster.indices.map { "qa-person-${FIRST_PERSON + it}" }

    /** Nome de cada pessoa extra pelo id, usado para os remetentes do chat. */
    val names: Map<String, String> = roster.mapIndexed { index, person -> personIds[index] to person.name }.toMap()

    fun people(): List<UserDto> = roster.mapIndexed { index, person ->
        UserDto(
            personIds[index], person.name, username = person.slug.replace('-', '.'),
            avatar = "mock-asset:avatar-${person.slug}.webp", authId = personIds[index],
        )
    }

    private class Place(val city: String, val state: String, val neighborhood: String, val latitude: Double, val longitude: Double)

    private val places = listOf(
        Place("Campinas", "SP", "Cambuí", -22.8995, -47.0562), Place("Belo Horizonte", "MG", "Savassi", -19.9387, -43.9350),
        Place("Curitiba", "PR", "Batel", -25.4420, -49.2800), Place("Porto Alegre", "RS", "Moinhos de Vento", -30.0277, -51.2000),
        Place("Salvador", "BA", "Barra", -13.0100, -38.5330), Place("Recife", "PE", "Boa Viagem", -8.1190, -34.9040),
        Place("Fortaleza", "CE", "Aldeota", -3.7400, -38.4960), Place("Brasília", "DF", "Asa Sul", -15.8300, -47.9000),
        Place("Goiânia", "GO", "Setor Bueno", -16.7050, -49.2700),
    )

    private val projectNames = listOf(
        "Cobertura Comercial Central" to "Instalação de 40 módulos em cobertura de loja.",
        "Condomínio Vista Verde" to "Sistema compartilhado para as áreas comuns.",
        "Galpão Logístico Norte" to "Usina de 120 kWp sobre o galpão.",
        "Clínica Vida Plena" to "Geração solar e adequação do quadro elétrico.",
        "Escola Raio de Sol" to "Projeto educacional com painel de acompanhamento.",
        "Fazenda Três Pinheiros" to "Bombeamento solar e geração para a sede.",
        "Hotel Aurora Mar" to "Aquecimento solar e geração fotovoltaica.",
        "Padaria Pão Dourado" to "Redução da conta de energia dos fornos.",
        "Posto Rota 116" to "Cobertura da pista com módulos bifaciais.",
    )

    private val projectStatus = listOf("IN_PROGRESS", "OPEN", "COMPLETED", "IN_PROGRESS", "OPEN", "COMPLETED", "IN_PROGRESS", "OPEN", "COMPLETED")

    fun projects(): List<ProjectDto> = projectNames.mapIndexed { index, (name, description) ->
        val place = places[index]
        val company = if (index % 2 == 0) "qa-company-1" else "qa-company-2"
        ProjectDto(
            "qa-project-demo-${index + 1}", name, description, "Equipe de demonstração", company,
            ownerCompanyLogoUrl = if (company == "qa-company-1") "mock-asset:company-logo-1.webp" else "mock-asset:company-logo-2.webp",
            createdAt = OffsetDateTime.now().minusDays(70L - index * 6).toString(), status = projectStatus[index],
            location = ProjectLocationDto(
                place.latitude, place.longitude, OffsetDateTime.now().minusDays(1).toString(), place.city, place.state, place.neighborhood,
                zipCode = "01000-000", street = "Rua de demonstração", number = "${100 + index * 20}",
            ),
        )
    }

    private val purposes = listOf(
        "Visita técnica do telhado", "Instalação dos módulos", "Revisão dos inversores", "Medição de sombreamento", "Troca do disjuntor geral",
        "Comissionamento do sistema", "Inspeção termográfica", "Manutenção preventiva", "Instalação do DPS", "Limpeza dos módulos",
        "Configuração do monitoramento", "Vistoria final", "Reaperto dos conectores", "Laudo da string 3", "Treinamento da equipe do cliente",
    )

    private val serviceStatus = listOf(
        "OPEN", "OPEN", "IN_PROGRESS", "COMPLETED", "OPEN", "IN_PROGRESS", "COMPLETED", "CANCELED", "OPEN", "COMPLETED",
        "IN_PROGRESS", "OPEN", "COMPLETED", "IN_PROGRESS", "OPEN",
    )

    fun services(projects: List<ProjectDto>): List<ServiceCallDto> = purposes.mapIndexed { index, purpose ->
        val project = projects[index % projects.size]
        val status = serviceStatus[index]
        val now = OffsetDateTime.now()
        ServiceCallDto(
            "qa-service-demo-${index + 1}", project.id, project.name, purpose, status,
            scheduledDate = if (status == "OPEN" || status == "IN_PROGRESS") now.plusDays((index % 9) + 1L).toString() else null,
            acceptedAt = if (status == "IN_PROGRESS" || status == "COMPLETED") now.minusDays(3L + index % 5).toString() else null,
            endDate = if (status == "COMPLETED") now.minusDays(1L + index % 6).toString() else null,
            location = project.location, canManage = status == "OPEN" || status == "IN_PROGRESS", hasExecutors = status != "CANCELED",
        )
    }

    fun notifications(): List<NotificationDto> {
        val now = Instant.now()
        val items = listOf(
            Triple("SERVICE_SCHEDULED", "Serviço agendado", "A visita técnica do telhado foi marcada para amanhã."),
            Triple("NEW_MESSAGE", "Nova mensagem", "Carlos Mendes enviou uma mensagem no chat."),
            Triple("SERVICE_STATUS_CHANGED", "Serviço atualizado", "O comissionamento do sistema foi concluído."),
            Triple("SERVICE_SCHEDULED", "Serviço agendado", "A instalação dos módulos tem nova data."),
            Triple("NEW_MESSAGE", "Nova mensagem", "Juliana Rocha respondeu na conversa do projeto."),
            Triple("SERVICE_STATUS_CHANGED", "Serviço atualizado", "A inspeção termográfica foi aceita pela equipe."),
            Triple("SERVICE_SCHEDULED", "Serviço agendado", "A vistoria final foi agendada."),
            Triple("SERVICE_STATUS_CHANGED", "Serviço cancelado", "A troca do disjuntor geral foi cancelada pelo cliente."),
            Triple("NEW_MESSAGE", "Nova mensagem", "Pedro Lima enviou fotos da obra."),
        )
        return items.mapIndexed { index, (type, title, body) ->
            NotificationDto(
                "demo-extra-${index + 1}", type, title, body,
                createdAt = now.minusSeconds(3 * 3600L + index * 5400L).toString(),
                readAt = if (index >= 3) now.minusSeconds(3600L + index * 600L).toString() else null,
            )
        }
    }
}
