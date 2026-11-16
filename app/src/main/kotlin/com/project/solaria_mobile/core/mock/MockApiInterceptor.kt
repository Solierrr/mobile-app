package com.project.solaria_mobile.core.mock

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.StartTransactionRequest
import com.project.solaria_mobile.core.auth.TokenResponse
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.EditMessageRequest
import com.project.solaria_mobile.feature.communication.data.ConversationStateDto
import com.project.solaria_mobile.feature.communication.data.DirectConversationRequest
import com.project.solaria_mobile.feature.communication.data.MessageUpdatesDto
import com.project.solaria_mobile.feature.communication.data.ConversationStateRequest
import com.project.solaria_mobile.feature.communication.data.SendMessageRequest
import com.project.solaria_mobile.feature.discovery.data.AddressDto
import com.project.solaria_mobile.feature.discovery.data.AddressListDto
import com.project.solaria_mobile.feature.discovery.data.GeocodeBody
import com.project.solaria_mobile.feature.profile.data.ConnectionRequest
import com.project.solaria_mobile.feature.profile.data.PeoplePageDto
import com.project.solaria_mobile.feature.profile.data.SelfMembershipDto
import com.project.solaria_mobile.feature.profile.data.SelfPersonDto
import com.project.solaria_mobile.feature.profile.data.SelfProfileDto
import com.project.solaria_mobile.feature.profile.data.SelfProfessionDto
import com.project.solaria_mobile.feature.profile.data.SelfTechnicianDto
import com.project.solaria_mobile.feature.profile.data.UserDto
import com.project.solaria_mobile.feature.project.data.ProjectActivityDto
import com.project.solaria_mobile.feature.project.data.ProjectDto
import com.project.solaria_mobile.feature.project.data.ProjectHistoryDto
import com.project.solaria_mobile.feature.project.data.ProjectLocationDto
import com.project.solaria_mobile.feature.project.data.ProjectMemberDto
import com.project.solaria_mobile.feature.project.data.ProjectPhotoDto
import com.project.solaria_mobile.feature.project.data.ProjectRequestDto
import java.net.URI
import java.time.OffsetDateTime
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.SerializationStrategy
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okhttp3.MultipartBody
import com.project.solaria_mobile.feature.project.data.ProjectStatusRequest
import com.project.solaria_mobile.feature.project.data.ProjectInvitationDto
import com.project.solaria_mobile.feature.project.data.ProjectInvitationRequestDto
import com.project.solaria_mobile.feature.project.data.ProjectActivityRequestDto
import com.project.solaria_mobile.feature.profile.data.CatalogCompanyDto
import com.project.solaria_mobile.feature.communication.data.NotificationDto
import com.project.solaria_mobile.feature.communication.data.NotificationPageDto
import com.project.solaria_mobile.feature.communication.data.UnreadCountDto
import com.project.solaria_mobile.feature.communication.data.CommunityDto
import com.project.solaria_mobile.feature.communication.data.CommunityMemberDto
import com.project.solaria_mobile.feature.communication.data.CommunityGroupDto
import com.project.solaria_mobile.feature.communication.data.CommunityCreateRequest
import com.project.solaria_mobile.feature.communication.data.CommunityGroupRequest
import com.project.solaria_mobile.feature.communication.data.CommunityMembersRequest
import com.project.solaria_mobile.feature.communication.data.GroupCreationDto
import com.project.solaria_mobile.feature.communication.data.ServiceCallDto
import com.project.solaria_mobile.feature.communication.data.ServiceScheduleRequest

@Singleton
class MockApiInterceptor @Inject constructor(
    private val mockMode: MockModeStore,
    private val tokens: TokenStore,
    private val data: MockApiData,
) : Interceptor {
    private val apiHosts = listOf(
        BuildConfig.AUTH_BASE_URL,
        BuildConfig.CORE_BASE_URL,
        BuildConfig.MESSENGER_BASE_URL,
        BuildConfig.GOOGLE_REGISTRY_BASE_URL,
        BuildConfig.NOTIFY_BASE_URL,
    ).mapNotNull { runCatching { URI(it).host }.getOrNull() }.toSet()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!mockMode.isEnabled() || request.url.host !in apiHosts) return chain.proceed(request)
        val segments = request.url.pathSegments
        val method = request.method
        val json = ApiJson.instance
        val actor = tokens.currentTokens()?.accessToken?.let(::accessTokenSubject) ?: "qa-mock-user"

        fun <T> success(serializer: SerializationStrategy<T>, value: T): Response =
            respond(chain, 200, json.encodeToString(serializer, value))
        fun notFound(): Response = respond(chain, 404, "{\"message\":\"Registro de demonstração não encontrado.\"}")
        fun empty(): Response = respond(chain, 204, "")

        return when {
            segments == listOf("api", "projects", "services") && method == "GET" ->
                success(ListSerializer(ServiceCallDto.serializer()), data.services())
            segments.size == 4 && segments.take(3) == listOf("api", "projects", "services") && method == "GET" ->
                data.service(segments[3])?.let { success(ServiceCallDto.serializer(), it) } ?: notFound()
            segments.size == 5 && segments.take(3) == listOf("api", "projects", "services") && method in setOf("POST", "PATCH") -> {
                val existing = data.service(segments[3])
                if (existing == null) notFound()
                else if (!existing.canManage || existing.managedByAgenda) respond(chain, 403, "{}")
                else {
                    val scheduled = if (segments[4] == "schedule") decode<ServiceScheduleRequest>(request.body?.let(::bodyText).orEmpty()).scheduledDate else null
                    data.changeService(segments[3], segments[4], scheduled)?.let { success(ServiceCallDto.serializer(), it) } ?: respond(chain, 422, "{}")
                }
            }
            segments.size == 4 && segments.take(3) == listOf("messaging", "communities", "project") && method == "GET" ->
                data.community(segments[3])?.let { success(CommunityDto.serializer(), data.communityForActor(it, actor)) } ?: notFound()
            segments == listOf("messaging", "communities") && method == "POST" -> {
                val body = decode<CommunityCreateRequest>(request.body?.let(::bodyText).orEmpty())
                if (data.community(body.projectId) != null) respond(chain, 409, "{}")
                else success(CommunityDto.serializer(), data.communityForActor(data.createCommunity(body, actor), actor))
            }
            segments.size == 4 && segments.take(2) == listOf("messaging", "communities") && segments[3] == "conversations" && method == "GET" ->
                success(ListSerializer(CommunityGroupDto.serializer()), data.communityGroups(segments[2], actor))
            segments.size == 4 && segments.take(2) == listOf("messaging", "communities") && segments[3] == "conversations" && method == "POST" -> {
                val body = decode<CommunityGroupRequest>(request.body?.let(::bodyText).orEmpty())
                data.createCommunityGroup(segments[2], body, actor)?.let { success(GroupCreationDto.serializer(), it) } ?: respond(chain, 422, "{}")
            }
            segments.size == 4 && segments.take(2) == listOf("messaging", "communities") && segments[3] == "members" && method == "POST" -> {
                val body = decode<CommunityMembersRequest>(request.body?.let(::bodyText).orEmpty())
                data.addCommunityMembers(segments[2], body.userIds)?.let { success(CommunityDto.serializer(), data.communityForActor(it, actor)) } ?: notFound()
            }
            segments == listOf("notify", "notifications", "unread-count") ->
                success(UnreadCountDto.serializer(), UnreadCountDto(data.notifications().count { it.readAt == null }.toLong()))
            segments == listOf("notify", "notifications", "read-all") && method == "PATCH" -> {
                data.markAllNotificationsRead()
                empty()
            }
            segments.size == 4 && segments.take(2) == listOf("notify", "notifications") && segments[3] == "read" && method == "PATCH" ->
                data.markNotificationRead(segments[2])?.let { success(NotificationDto.serializer(), it) } ?: notFound()
            segments == listOf("notify", "notifications") && method == "GET" -> {
                val page = request.url.queryParameter("page")?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val size = request.url.queryParameter("size")?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                val all = data.notifications().filter { request.url.queryParameter("unreadOnly") != "true" || it.readAt == null }
                success(NotificationPageDto.serializer(), NotificationPageDto(all.drop(page * size).take(size), page, size, all.size.toLong(), (all.size + size - 1) / size))
            }
            segments.size == 3 && segments.take(2) == listOf("notify", "devices") && method in setOf("PUT", "DELETE") -> empty()
            segments == listOf("api", "catalog", "companies") -> success(ListSerializer(CatalogCompanyDto.serializer()), listOf(
                CatalogCompanyDto("qa-company-1", "Equipe de demonstração", "equipe-de-demonstracao", "São Paulo", "SP"),
                CatalogCompanyDto("qa-company-2", "Energia de demonstração", "energia-de-demonstracao", "Campinas", "SP"),
            ))
            segments == listOf("api", "users", "me") && method == "PATCH" -> respond(chain, 200, "{}")
            segments.size == 6 && segments.take(2) == listOf("api", "user-photos") && method == "PUT" -> respond(chain, 200, "{}")
            segments.size == 6 && segments.take(2) == listOf("api", "user-photos") && method == "DELETE" -> empty()
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "activities" && method == "POST" -> {
                val body = decode<ProjectActivityRequestDto>(request.body?.let(::bodyText).orEmpty())
                success(ProjectActivityDto.serializer(), data.saveActivity(segments[2], null, body))
            }
            segments.size == 5 && segments.take(2) == listOf("api", "projects") && segments[3] == "activities" && method == "PUT" -> {
                val body = decode<ProjectActivityRequestDto>(request.body?.let(::bodyText).orEmpty())
                success(ProjectActivityDto.serializer(), data.saveActivity(segments[2], segments[4], body))
            }
            segments == listOf("api", "projects", "invitations", "me") -> success(ListSerializer(ProjectInvitationDto.serializer()), emptyList())
            segments.size == 4 && segments.take(3) == listOf("api", "projects", "invitations") && method == "DELETE" ->
                data.revokeInvitation(segments[3])?.let { success(ProjectInvitationDto.serializer(), it) } ?: notFound()
            segments.size == 5 && segments.take(3) == listOf("api", "projects", "invitations") && segments[4] == "resend" ->
                data.invitation(segments[3])?.let { success(ProjectInvitationDto.serializer(), it) } ?: notFound()
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "invitations" && method == "GET" ->
                success(ListSerializer(ProjectInvitationDto.serializer()), data.invitations(segments[2]))
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "invitations" && method == "POST" -> {
                val body = decode<ProjectInvitationRequestDto>(request.body?.let(::bodyText).orEmpty())
                success(ListSerializer(ProjectInvitationDto.serializer()), listOf(data.invite(segments[2], body)))
            }
            segments.size == 5 && segments.take(2) == listOf("api", "projects") && segments[3] == "members" && method == "DELETE" -> {
                data.removeMember(segments[4])
                empty()
            }
            segments.size == 3 && segments.take(2) == listOf("api", "projects") && method == "PUT" -> {
                val body = decode<ProjectRequestDto>(request.body?.let(::bodyText).orEmpty())
                data.updateProject(segments[2], body)?.let { success(ProjectDto.serializer(), it) } ?: notFound()
            }
            segments.size == 6 && segments.take(2) == listOf("api", "projects") && segments[3] == "activities" && segments[5] == "status" && method == "PATCH" -> {
                val body = decode<ProjectStatusRequest>(request.body?.let(::bodyText).orEmpty())
                data.changeActivityStatus(segments[2], segments[4], body.status)?.let { success(ProjectActivityDto.serializer(), it) } ?: notFound()
            }
            segments.size == 6 && segments.take(2) == listOf("api", "projects") && segments[3] == "photos" && segments[5] == "cover" && method == "PATCH" ->
                data.setCover(segments[2], segments[4])?.let { success(ProjectPhotoDto.serializer(), it) } ?: notFound()
            segments.size == 5 && segments.take(2) == listOf("api", "projects") && segments[3] == "photos" && method == "DELETE" -> {
                data.removePhoto(segments[2], segments[4])
                empty()
            }
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "photos" && method == "POST" -> {
                val part = (request.body as? MultipartBody)?.parts?.firstOrNull()?.body
                val bytes = part?.let { Buffer().also(it::writeTo).readByteArray() }
                if (bytes == null || bytes.isEmpty()) respond(chain, 422, "{\"message\":\"Selecione uma imagem.\"}")
                else success(ProjectPhotoDto.serializer(), data.addPhoto(segments[2], bytes, part.contentType()?.toString() ?: "image/jpeg"))
            }
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "status" && method == "PATCH" -> {
                val body = decode<ProjectStatusRequest>(request.body?.let(::bodyText).orEmpty())
                data.changeStatus(segments[2], body.status)?.let { success(ProjectDto.serializer(), it) } ?: notFound()
            }
            segments.size == 3 && segments.take(2) == listOf("api", "projects") && method == "DELETE" -> {
                data.archive(segments[2])
                empty()
            }
            segments == listOf("api", "projects") && method == "GET" -> success(ListSerializer(ProjectDto.serializer()), data.projects())
            segments == listOf("api", "projects") && method == "POST" -> {
                val draft = decode<ProjectRequestDto>(request.body?.let(::bodyText).orEmpty())
                success(ProjectDto.serializer(), data.createProject(draft))
            }
            segments.size == 3 && segments.take(2) == listOf("api", "projects") && method == "GET" ->
                data.project(segments[2])?.let { success(ProjectDto.serializer(), it) } ?: notFound()
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "map-location" ->
                data.project(segments[2])?.let { success(ProjectDto.serializer(), it) } ?: notFound()
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "members" -> success(ListSerializer(ProjectMemberDto.serializer()), data.membersForActor(segments[2], actor))
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "activities" -> success(ListSerializer(ProjectActivityDto.serializer()), data.activities(segments[2]))
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "history" -> success(ListSerializer(ProjectHistoryDto.serializer()), data.history(segments[2]))
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "photos" -> success(ListSerializer(ProjectPhotoDto.serializer()), data.photos(segments[2]))
            segments.size == 6 && segments.take(2) == listOf("api", "projects") && segments[3] == "photos" && segments[5] == "content" ->
                data.photoContent(segments[4])?.let { (bytes, mime) -> respondBytes(chain, bytes, mime) }
                    ?: respond(chain, 404, "{\"message\":\"Imagem de demonstração indisponível.\"}")

            segments == listOf("api", "users", "me") -> success(SelfProfileDto.serializer(), data.ownProfile())
            segments == listOf("api", "connections") && method == "GET" -> success(ListSerializer(UserDto.serializer()), data.connections())
            segments == listOf("api", "connections") && method == "POST" -> {
                val requestBody = decode<ConnectionRequest>(request.body?.let(::bodyText).orEmpty())
                success(UserDto.serializer(), data.setConnection(requestBody.connectedUserId, true))
            }
            segments.size == 3 && segments.take(2) == listOf("api", "connections") && method == "DELETE" -> {
                data.setConnection(segments[2], false)
                empty()
            }
            segments == listOf("api", "connections", "people") -> {
                val query = request.url.queryParameter("query").orEmpty()
                val page = request.url.queryParameter("page")?.toIntOrNull() ?: 1
                val connectedOnly = request.url.queryParameter("connectedOnly")?.toBooleanStrictOrNull() ?: false
                success(PeoplePageDto.serializer(), data.people(query, page, connectedOnly))
            }
            segments.size == 4 && segments.take(3) == listOf("api", "connections", "people") ->
                data.person(segments[3])?.let { success(UserDto.serializer(), it) } ?: notFound()

            segments.size == 4 && segments.take(2) == listOf("messaging", "conversations") && segments[3] == "state" && method == "PATCH" -> {
                val patch = decode<ConversationStateRequest>(request.body?.let(::bodyText).orEmpty())
                data.conversationState(segments[2], patch)?.let { success(ConversationStateDto.serializer(), it) } ?: notFound()
            }
            segments.size == 3 && segments.take(2) == listOf("messaging", "messages") && method == "PATCH" -> {
                val edit = decode<EditMessageRequest>(request.body?.let(::bodyText).orEmpty())
                data.mutateMessage(segments[2], request.header("Authorization").orEmpty(), edit.content).let { (code, body) -> respond(chain, code, body) }
            }
            segments.size == 3 && segments.take(2) == listOf("messaging", "messages") && method == "DELETE" ->
                data.mutateMessage(segments[2], request.header("Authorization").orEmpty(), null).let { (code, body) -> respond(chain, code, body) }
            segments == listOf("messaging", "updates") -> success(MessageUpdatesDto.serializer(), data.updates())
            segments == listOf("messaging", "conversations", "direct") && method == "POST" -> {
                val body = decode<DirectConversationRequest>(request.body?.let(::bodyText).orEmpty())
                success(ConversationDto.serializer(), data.chatForActor(data.direct(body.recipientId, actor), actor))
            }
            segments.size == 5 && segments.take(3) == listOf("api", "users", "auth") && segments[4] == "summary" ->
                success(com.project.solaria_mobile.feature.profile.data.UserSummaryDto.serializer(), com.project.solaria_mobile.feature.profile.data.UserSummaryDto("Pessoa de demonstração"))
            segments == listOf("messaging", "conversations", "me") -> success(ListSerializer(ConversationDto.serializer()), data.conversations(request.url.queryParameter("archived") == "true").map { data.chatForActor(it, actor) })
            segments.size == 3 && segments.take(2) == listOf("messaging", "conversations") ->
                data.conversation(segments[2])?.let { success(ConversationDto.serializer(), data.chatForActor(it, actor)) } ?: notFound()
            segments.size == 4 && segments.take(3) == listOf("messaging", "messages", "conversation") -> success(ListSerializer(MessageDto.serializer()), data.messages(segments[3]).filter { message ->
                val since = request.url.queryParameter("sinceSequence")?.toIntOrNull()
                val before = request.url.queryParameter("beforeSequence")?.toIntOrNull()
                (since == null || message.sequence > since) && (before == null || message.sequence < before)
            }.takeLast(request.url.queryParameter("limit")?.toIntOrNull() ?: 100))
            segments == listOf("messaging", "messages") && method == "POST" -> {
                val message = decode<SendMessageRequest>(request.body?.let(::bodyText).orEmpty())
                success(MessageDto.serializer(), data.sendMessage(message, request.header("Authorization").orEmpty()))
            }

            segments == listOf("v1", "address", "geocode") -> {
                val body = decode<GeocodeBody>(request.body?.let(::bodyText).orEmpty())
                success(AddressListDto.serializer(), data.geocode(body.address))
            }
            segments == listOf("health") -> empty()

            segments == listOf("auth", "mobile", "transactions") -> {
                val body = decode<StartTransactionRequest>(request.body?.let(::bodyText).orEmpty())
                success(com.project.solaria_mobile.core.auth.StartTransactionResponse.serializer(), com.project.solaria_mobile.core.auth.StartTransactionResponse(
                    transactionId = "qa-mock-transaction",
                    authorizationUrl = "${body.redirectUri}?code=qa-mock-code&state=${body.state}",
                ))
            }
            segments == listOf("auth", "mobile", "token") -> success(TokenResponse.serializer(), data.mockTokens())
            segments == listOf("auth", "refresh") -> {
                val current = tokens.currentTokens()
                success(TokenResponse.serializer(), TokenResponse(current?.accessToken ?: data.mockTokens().accessToken, current?.refreshToken ?: data.mockTokens().refreshToken))
            }
            segments == listOf("auth", "sessions", "current", "logout") -> empty()
            else -> respond(chain, 501, "{\"message\":\"Este endpoint ainda não tem fixture local no modo mock.\"}")
        }
    }

    private inline fun <reified T> decode(value: String): T = ApiJson.instance.decodeFromString(value)

    private fun bodyText(body: okhttp3.RequestBody): String = Buffer().also(body::writeTo).readUtf8()

    private fun respondBytes(chain: Interceptor.Chain, bytes: ByteArray, mime: String): Response = Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("Mock")
        .body(bytes.toResponseBody(mime.toMediaType()))
        .build()

    private fun respond(chain: Interceptor.Chain, code: Int, content: String): Response = Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code in 200..299) "Mock" else "Mock response")
        .body(content.toResponseBody("application/json; charset=utf-8".toMediaType()))
        .build()
}

@Singleton
class MockApiData @Inject constructor() {
    private val chatEpoch = "qa-mock-${java.util.UUID.randomUUID()}"
    private val notificationList = mutableListOf(
        NotificationDto("demo-message", "NEW_MESSAGE", "Nova mensagem", "Há uma mensagem para você na conversa de demonstração.", createdAt = java.time.Instant.now().toString()),
        NotificationDto("demo-scheduled", "SERVICE_SCHEDULED", "Serviço agendado", "Seu serviço foi agendado. Confira a data e organize sua agenda.", createdAt = java.time.Instant.now().minusSeconds(3600).toString()),
        NotificationDto("demo-read", "SERVICE_STATUS_CHANGED", "Serviço atualizado", "As informações do serviço foram atualizadas.", createdAt = java.time.Instant.now().minusSeconds(7200).toString(), readAt = java.time.Instant.now().minusSeconds(1800).toString()),
    )

    @Synchronized fun notifications(): List<NotificationDto> = notificationList.map { item ->
        if (item.type == "NEW_MESSAGE") item.copy(data = conversations().firstOrNull()?.id?.let { mapOf("chatId" to it) }.orEmpty()) else item
    }

    @Synchronized fun markNotificationRead(id: String): NotificationDto? {
        val index = notificationList.indexOfFirst { it.id == id }
        if (index < 0) return null
        notificationList[index] = notificationList[index].let { it.copy(readAt = it.readAt ?: java.time.Instant.now().toString()) }
        return notifications().first { it.id == id }
    }

    @Synchronized fun markAllNotificationsRead() {
        notificationList.indices.forEach { index ->
            notificationList[index] = notificationList[index].let { it.copy(readAt = it.readAt ?: java.time.Instant.now().toString()) }
        }
    }
    private val projects = mutableListOf(
        ProjectDto("qa-project-1", "Usina Solar Horizonte", "Instalação fotovoltaica residencial.", "Equipe de demonstração", "qa-company-1", createdAt = "2026-09-18T12:00:00Z", status = "IN_PROGRESS", location = location(-23.5505, -46.6333, "São Paulo", "SP", "Vila Mariana")),
        ProjectDto("qa-project-2", "Residencial Ipê", "Modernização do sistema de energia.", "Equipe de demonstração", "qa-company-1", createdAt = "2026-09-23T12:00:00Z", status = "OPEN", location = location(-23.5614, -46.6559, "São Paulo", "SP", "Pinheiros")),
        ProjectDto("qa-project-3", "Sítio Boa Vista", "Projeto de geração distribuída.", "Equipe de demonstração", "qa-company-2", createdAt = "2026-10-01T12:00:00Z", status = "COMPLETED", location = location(-23.5880, -46.6790, "São Paulo", "SP", "Morumbi")),
    )
    private val people = mutableListOf(
        UserDto("qa-person-1", "Ana Oliveira", username = "ana.oliveira", authId = "qa-person-1"),
        UserDto("qa-person-2", "Rafael Costa", username = "rafael.costa", authId = "qa-person-2"),
        UserDto("qa-person-3", "Marina Santos", username = "marina.santos", authId = "qa-person-3"),
    )
    private val serviceStore = linkedMapOf(
        "qa-service-1" to ServiceCallDto("qa-service-1", "qa-project-1", "Usina Solar Horizonte", "Revisão dos inversores", "OPEN",
            scheduledDate = OffsetDateTime.now().plusDays(1).toString(), location = projects.first().location, canManage = true, hasExecutors = true),
        "qa-service-2" to ServiceCallDto("qa-service-2", "qa-project-2", "Residencial Ipê", "Inspeção da instalação", "IN_PROGRESS",
            scheduledDate = OffsetDateTime.now().toString(), acceptedAt = OffsetDateTime.now().minusHours(1).toString(), location = projects[1].location, canManage = true, hasExecutors = true),
        "qa-service-3" to ServiceCallDto("qa-service-3", "qa-project-3", "Sítio Boa Vista", "Manutenção preventiva", "COMPLETED",
            endDate = OffsetDateTime.now().minusDays(2).toString(), location = projects[2].location),
    )

    @Synchronized fun services() = serviceStore.values.toList()
    @Synchronized fun service(id: String) = serviceStore[id]
    @Synchronized fun changeService(id: String, decision: String, scheduledDate: String?): ServiceCallDto? {
        val current = serviceStore[id] ?: return null
        if (!current.canManage || current.managedByAgenda) return null
        val next = when (decision) {
            "acceptance" -> if (current.status == "OPEN" && current.hasExecutors) current.copy(status = "IN_PROGRESS", acceptedAt = OffsetDateTime.now().toString()) else null
            "completion" -> if (current.status == "IN_PROGRESS") current.copy(status = "COMPLETED", endDate = OffsetDateTime.now().toString()) else null
            "cancellation" -> if (current.status in setOf("OPEN", "IN_PROGRESS")) current.copy(status = "CANCELED") else null
            "schedule" -> if (current.status in setOf("OPEN", "IN_PROGRESS") && scheduledDate?.let { runCatching { OffsetDateTime.parse(it).isAfter(OffsetDateTime.now()) }.getOrDefault(false) } == true) current.copy(scheduledDate = scheduledDate) else null
            else -> null
        }
        if (next != null) serviceStore[id] = next
        return next
    }
    private val connectedIds = linkedSetOf("qa-person-1")
    private val conversationList = mutableListOf(
        ConversationDto("qa-conversation-1", syncEpoch = chatEpoch, syncVersion = 1, conversationType = "DIRECT", participantIds = listOf("qa-mock-user", "qa-person-1"), title = "Ana Oliveira", lastInteractionAt = OffsetDateTime.now().minusHours(2).toString()),
        ConversationDto("qa-conversation-2", syncEpoch = chatEpoch, syncVersion = 1, conversationType = "GROUP", title = "Equipe de demonstração", lastInteractionAt = OffsetDateTime.now().minusDays(1).toString()),
    )
    private val conversationMessages = mutableMapOf(
        "qa-conversation-1" to mutableListOf(MessageDto("qa-message-1", "qa-conversation-1", senderId = "qa-person-1", content = "Olá! Esta conversa usa dados locais de demonstração.", timestamp = OffsetDateTime.now().minusHours(2).toString(), sequence = 1, changeVersion = 1)),
        "qa-conversation-2" to mutableListOf(MessageDto("qa-message-2", "qa-conversation-2", senderId = "qa-person-2", content = "O ambiente de testes está pronto.", timestamp = OffsetDateTime.now().minusDays(1).toString(), sequence = 1, changeVersion = 2)),
    )
    private var nextProject = 4
    private var nextMessage = 3
    private val selfId = "qa-mock-user"

    @Synchronized fun projects() = projects.toList()
    @Synchronized fun project(id: String) = projects.firstOrNull { it.id == id }?.copy(role = "OWNER")

    @Synchronized fun createProject(draft: ProjectRequestDto): ProjectDto {
        val id = "qa-project-${nextProject++}"
        val item = ProjectDto(id, draft.name, draft.description, "Pessoa de demonstração", status = "OPEN", createdAt = OffsetDateTime.now().toString(), location = draft.location)
        projects.add(0, item)
        return item
    }

    private val invitationStore = mutableListOf<ProjectInvitationDto>()
    private val removedMembers = mutableSetOf<String>()
    private var nextInvitation = 1

    @Synchronized fun invitations(projectId: String) = invitationStore.filter { it.projectId == projectId && it.status == "PENDING" }

    @Synchronized fun invitation(id: String) = invitationStore.firstOrNull { it.id == id }

    @Synchronized fun invite(projectId: String, request: ProjectInvitationRequestDto): ProjectInvitationDto {
        val invitation = ProjectInvitationDto("qa-invitation-${nextInvitation++}", projectId, project(projectId)?.name, request.email, request.role,
            request.functionName, "PENDING", OffsetDateTime.now().plusDays(7).toString())
        invitationStore += invitation
        return invitation
    }

    @Synchronized fun revokeInvitation(id: String): ProjectInvitationDto? {
        val index = invitationStore.indexOfFirst { it.id == id }
        if (index < 0) return null
        invitationStore[index] = invitationStore[index].copy(status = "REVOKED")
        return invitationStore[index]
    }

    @Synchronized fun removeMember(userId: String) {
        removedMembers += userId
    }

    @Synchronized fun members(projectId: String) = baseMembers(projectId).filterNot { it.userId in removedMembers }

    fun membersForActor(projectId: String, actor: String) = members(projectId).map {
        if (it.authId == selfId) it.copy(authId = actor) else it
    }

    private val communityStore = linkedMapOf<String, CommunityDto>()
    private val communityGroupStore = linkedMapOf<String, MutableList<CommunityGroupDto>>()

    @Synchronized fun community(projectId: String) = communityStore.values.firstOrNull { it.projectId == projectId }

    @Synchronized fun createCommunity(request: CommunityCreateRequest, actor: String): CommunityDto {
        val id = "qa-community-${java.util.UUID.randomUUID()}"
        val members = (request.memberIds.map { if (it == actor) selfId else it }.toSet() + selfId).map { CommunityMemberDto(it, if (it == selfId) "OWNER" else "MEMBER") }
        return CommunityDto(id, request.projectId, members, "ACTIVE").also { communityStore[id] = it }
    }

    @Synchronized fun communityGroups(id: String, actor: String) = communityGroupStore[id]?.map {
        it.copy(participantIds = it.participantIds.map { member -> if (member == selfId) actor else member }.toSet())
    }.orEmpty()

    fun communityForActor(community: CommunityDto, actor: String) = community.copy(members = community.members.map {
        if (it.userId == selfId) it.copy(userId = actor) else it
    })

    @Synchronized fun addCommunityMembers(id: String, users: Set<String>): CommunityDto? {
        val existing = communityStore[id] ?: return null
        val added = users.filter { candidate -> existing.members.none { it.userId == candidate } }.map { CommunityMemberDto(it, "MEMBER") }
        return existing.copy(members = existing.members + added).also { communityStore[id] = it }
    }

    @Synchronized fun createCommunityGroup(id: String, request: CommunityGroupRequest, actor: String): GroupCreationDto? {
        val community = communityStore[id] ?: return null
        val title = request.title
        if (community.members.size < 2 || title.isBlank() || title.length > 120) return null
        val participants = if (request.participantIds.isEmpty()) community.members.map { it.userId }.toSet()
            else (request.participantIds.map { if (it == actor) selfId else it }.toSet() + selfId).intersect(community.members.map { it.userId }.toSet())
        if (participants.size < 2) return null
        val groupId = "qa-group-${java.util.UUID.randomUUID()}"
        val group = CommunityGroupDto(groupId, title.trim(), participants, "ACTIVE")
        communityGroupStore.getOrPut(id) { mutableListOf() }.add(group)
        val conversation = ConversationDto(groupId, syncEpoch = chatEpoch, syncVersion = nextMessage.toLong(), conversationType = "GROUP", title = title.trim(), lastInteractionAt = OffsetDateTime.now().toString())
        conversationList.add(conversation)
        conversationMessages[groupId] = mutableListOf()
        return GroupCreationDto(conversation)
    }

    private fun baseMembers(projectId: String) = listOf(
        ProjectMemberDto("$projectId-member-1", selfId, "Pessoa de demonstração", "OWNER", "Responsável", authId = selfId),
        ProjectMemberDto("$projectId-member-2", "qa-person-1", "Ana Oliveira", "MEMBER", "Técnica solar", authId = "qa-person-1"),
    )

    private val activityStatus = mutableMapOf<String, String>()

    @Synchronized fun changeActivityStatus(projectId: String, activityId: String, status: String): ProjectActivityDto? {
        if (activities(projectId).none { it.id == activityId }) return null
        activityStatus[activityId] = status
        return activities(projectId).first { it.id == activityId }
    }

    private val savedActivities = mutableMapOf<String, ProjectActivityDto>()
    private var nextActivity = 1

    @Synchronized fun activities(projectId: String): List<ProjectActivityDto> {
        val base = baseActivities(projectId).map { savedActivities[it.id] ?: it }
        val created = savedActivities.values.filter { it.id.startsWith("$projectId-new-") }
        return (base + created).map { activity -> activityStatus[activity.id]?.let { activity.copy(status = it) } ?: activity }
            .sortedBy { it.startsAt }
    }

    @Synchronized fun saveActivity(projectId: String, activityId: String?, request: ProjectActivityRequestDto): ProjectActivityDto {
        val id = activityId ?: "$projectId-new-${nextActivity++}"
        val saved = ProjectActivityDto(id, request.title, request.description, request.type, "OPEN", request.startsAt, request.endsAt,
            request.onsite, request.assigneeIds)
        savedActivities[id] = saved
        return saved
    }

    private fun baseActivities(projectId: String): List<ProjectActivityDto> {
        val now = OffsetDateTime.now()
        return listOf(
            ProjectActivityDto("$projectId-activity-0", "Alinhamento com o cliente", "Revisão do cronograma", "MEETING", "OPEN", now.withHour(17).withMinute(0).withSecond(0).withNano(0).toString(), onsite = false),
            ProjectActivityDto("$projectId-activity-1", "Visita técnica", "Avaliação do local", "VISIT", "OPEN", now.plusDays(2).withHour(10).withMinute(0).toString(), onsite = true),
            ProjectActivityDto("$projectId-activity-2", "Instalação dos módulos", "Montagem e ligação", "SERVICE", "IN_PROGRESS", now.plusDays(4).withHour(8).withMinute(30).toString(), onsite = true),
        )
    }

    fun history(projectId: String) = listOf(
        ProjectHistoryDto("$projectId-history-1", "Projeto criado", "Solicitação recebida", "Pessoa de demonstração", OffsetDateTime.now().minusDays(5).toString()),
        ProjectHistoryDto("$projectId-history-2", "Equipe atualizada", "Ana Oliveira adicionada", "Pessoa de demonstração", OffsetDateTime.now().minusDays(2).toString()),
    )

    // Fotos enviadas no modo mock ficam só na memória do app, para a galeria de demonstração funcionar sem servidor.
    private val photoStore = mutableMapOf<String, MutableList<ProjectPhotoDto>>()
    private val photoBytes = mutableMapOf<String, Pair<ByteArray, String>>()
    private var nextPhoto = 1

    @Synchronized fun photos(projectId: String): List<ProjectPhotoDto> = photoStore[projectId].orEmpty().sortedByDescending { it.cover }

    @Synchronized fun addPhoto(projectId: String, bytes: ByteArray, mime: String): ProjectPhotoDto {
        val list = photoStore.getOrPut(projectId) { mutableListOf() }
        val photo = ProjectPhotoDto("qa-photo-${nextPhoto++}", cover = list.isEmpty())
        list += photo
        photoBytes[photo.id] = bytes to mime
        return photo
    }

    @Synchronized fun removePhoto(projectId: String, photoId: String) {
        val list = photoStore[projectId] ?: return
        val removed = list.firstOrNull { it.id == photoId } ?: return
        list.remove(removed)
        photoBytes.remove(photoId)
        if (removed.cover && list.isNotEmpty()) list[0] = list[0].copy(cover = true)
    }

    @Synchronized fun setCover(projectId: String, photoId: String): ProjectPhotoDto? {
        val list = photoStore[projectId] ?: return null
        if (list.none { it.id == photoId }) return null
        list.replaceAll { it.copy(cover = it.id == photoId) }
        return list.first { it.id == photoId }
    }

    @Synchronized fun photoContent(photoId: String): Pair<ByteArray, String>? = photoBytes[photoId]

    @Synchronized fun changeStatus(projectId: String, status: String): ProjectDto? {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index < 0) return null
        projects[index] = projects[index].copy(status = status)
        return projects[index]
    }

    @Synchronized fun updateProject(projectId: String, request: ProjectRequestDto): ProjectDto? {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index < 0) return null
        projects[index] = projects[index].copy(name = request.name, description = request.description, location = request.location)
        return projects[index]
    }

    @Synchronized fun archive(projectId: String) {
        projects.removeAll { it.id == projectId }
    }

    fun ownProfile() = SelfProfileDto(
        id = selfId,
        person = SelfPersonDto("Pessoa de demonstração", ""),
        technician = SelfTechnicianDto("qa-technician", listOf(SelfProfessionDto("qa-profession", "Instalação solar"))),
        companyMemberships = listOf(SelfMembershipDto("qa-company-1", "Equipe de demonstração", "Técnico")),
    )

    @Synchronized fun connections() = people.filter { it.id in connectedIds }.map { it.copy(connected = true) }

    @Synchronized fun setConnection(id: String, connected: Boolean): UserDto {
        if (connected) connectedIds.add(id) else connectedIds.remove(id)
        return person(id)?.copy(connected = connected) ?: UserDto(id, "Pessoa de demonstração", connected)
    }

    @Synchronized fun person(id: String): UserDto? = people.firstOrNull { it.id == id }?.copy(connected = id in connectedIds)

    @Synchronized fun people(query: String, page: Int, connectedOnly: Boolean): PeoplePageDto {
        val filtered = people.filter { user ->
            (query.isBlank() || listOfNotNull(user.name, user.username).any { it.contains(query, true) }) &&
                (!connectedOnly || user.id in connectedIds)
        }
        val pageSize = 20
        val pageItems = filtered.drop(((page - 1).coerceAtLeast(0)) * pageSize).take(pageSize).map { it.copy(connected = it.id in connectedIds) }
        return PeoplePageDto(pageItems, filtered.size > page * pageSize)
    }

    private val mutedConversations = mutableSetOf<String>()
    private val archivedConversations = mutableSetOf<String>()

    @Synchronized fun conversations(archived: Boolean = false) = conversationList.filter { (it.id in archivedConversations) == archived }
        .map { conversation ->
            val last = conversationMessages[conversation.id]?.lastOrNull()
            conversation.copy(syncEpoch = chatEpoch, syncVersion = nextMessage.toLong(), muted = conversation.id in mutedConversations, archived = archived,
                lastMessage = last?.let { com.project.solaria_mobile.feature.communication.data.LastMessageDto(it.id, it.sequence, it.content, it.deleted) })
        }
    @Synchronized fun conversation(id: String) = (conversations() + conversations(true)).firstOrNull { it.id == id }

    /** Estado pessoal: sem campos, só devolve o atual, como o messenger. */
    fun chatForActor(conversation: ConversationDto, actor: String): ConversationDto = conversation.copy(
        participantIds = conversation.participantIds.map { if (it == selfId) actor else it },
    )

    @Synchronized fun conversationState(id: String, patch: ConversationStateRequest): ConversationStateDto? {
        if (conversation(id) == null) return null
        patch.muted?.let { if (it) mutedConversations += id else mutedConversations -= id }
        patch.archived?.let { if (it) archivedConversations += id else archivedConversations -= id }
        return ConversationStateDto(id, syncEpoch = chatEpoch, syncVersion = (++nextMessage).toLong(), muted = id in mutedConversations, archived = id in archivedConversations, lastReadSequence = patch.lastReadSequence ?: 0)
    }

    /** Edita ou exclui só a mensagem da própria conta; mensagem excluída não volta a ser editada. */
    @Synchronized fun mutateMessage(id: String, authorization: String, content: String?): Pair<Int, String> {
        val me = subjectFrom(authorization) ?: selfId
        for (messages in conversationMessages.values) {
            val index = messages.indexOfFirst { it.id == id }
            if (index < 0) continue
            val current = messages[index]
            if (current.senderId != me) return 403 to "{}"
            if (content != null && current.deleted) return 422 to "{}"
            val updated = if (content == null) current.copy(content = "", deleted = true, changeVersion = (++nextMessage).toLong()) else current.copy(content = content, edited = true, changeVersion = (++nextMessage).toLong())
            messages[index] = updated
            return 200 to ApiJson.instance.encodeToString(MessageDto.serializer(), updated)
        }
        return 404 to "{}"
    }
    @Synchronized fun messages(id: String) = conversationMessages[id]?.toList().orEmpty()

    @Synchronized fun sendMessage(request: SendMessageRequest, authorization: String): MessageDto {
        val id = "qa-message-${nextMessage++}"
        val message = MessageDto(id, request.conversationId, senderId = subjectFrom(authorization) ?: selfId, content = request.content, timestamp = OffsetDateTime.now().toString(), sequence = messages(request.conversationId).size + 1, clientMessageId = request.clientMessageId, changeVersion = nextMessage.toLong())
        conversationMessages.getOrPut(request.conversationId) { mutableListOf() }.add(message)
        return message
    }

    @Synchronized fun updates() = MessageUpdatesDto(chatEpoch, nextMessage.toLong(), messages = conversationMessages.values.flatten())

    @Synchronized fun direct(recipient: String, actor: String): ConversationDto {
        conversationList.firstOrNull { it.conversationType == "DIRECT" && recipient in it.participantIds }?.let { return it }
        val conversation = ConversationDto("qa-direct-${nextMessage++}", syncEpoch = chatEpoch, syncVersion = nextMessage.toLong(), conversationType = "DIRECT", participantIds = listOf(actor, recipient))
        conversationList.add(conversation)
        conversationMessages[conversation.id] = mutableListOf()
        return conversation
    }

    fun geocode(address: String): AddressListDto {
        val offset = (address.hashCode() and 0x7fff) / 100000.0
        return AddressListDto(listOf(AddressDto(address.trim(), -23.5505 + offset, -46.6333 + offset)))
    }

    fun mockTokens(): TokenResponse = TokenResponse(MOCK_ACCESS_TOKEN, "qa-mock-refresh-token")

    private fun location(latitude: Double, longitude: Double, city: String, state: String, neighborhood: String) =
        ProjectLocationDto(latitude, longitude, OffsetDateTime.now().minusDays(1).toString(), city, state, neighborhood, street = "Rua de demonstração")

    private fun subjectFrom(authorization: String): String? = runCatching {
        val token = authorization.removePrefix("Bearer ").split('.')[1]
        val payload = String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8)
        Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
    }.getOrNull()

    private companion object {
        const val MOCK_ACCESS_TOKEN = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJxYS1tb2NrLXVzZXIiLCJleHAiOjk5OTk5OTk5OTl9.mock"
    }
}
