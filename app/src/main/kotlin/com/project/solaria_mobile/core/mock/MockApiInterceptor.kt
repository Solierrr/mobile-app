package com.project.solaria_mobile.core.mock

import com.project.solaria_mobile.feature.project.data.ProjectInviteLinkRequestDto
import com.project.solaria_mobile.feature.project.data.ProjectInviteLinkDto
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.StartTransactionRequest
import com.project.solaria_mobile.core.auth.TokenResponse
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.AttachmentDto
import com.project.solaria_mobile.feature.communication.data.MessagePreviewDto
import com.project.solaria_mobile.feature.communication.data.EditMessageRequest
import com.project.solaria_mobile.feature.communication.data.ConversationStateDto
import com.project.solaria_mobile.feature.communication.data.ConversationStatePatch
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
import com.project.solaria_mobile.feature.project.data.ProjectReportDto
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
import com.project.solaria_mobile.feature.communication.data.CommunityRemovalRequest
import com.project.solaria_mobile.feature.communication.data.CommunityStatusRequest
import com.project.solaria_mobile.feature.communication.data.GroupConversationRequest
import com.project.solaria_mobile.feature.communication.data.MediaUploadDto
import com.project.solaria_mobile.feature.communication.data.MediaUploadRequest
import com.project.solaria_mobile.feature.communication.data.MediaUploadTargetDto
import com.project.solaria_mobile.feature.communication.data.NpsRequest
import com.project.solaria_mobile.feature.communication.data.ParticipantsRequest
import com.project.solaria_mobile.feature.communication.data.RejectedGroupParticipantDto
import com.project.solaria_mobile.feature.communication.data.TransferManagerRequest
import com.project.solaria_mobile.feature.profile.data.UserSummaryDto
import com.project.solaria_mobile.feature.rating.data.RatingDto
import com.project.solaria_mobile.feature.rating.data.RatingRequest
import com.project.solaria_mobile.feature.rating.data.RatingStatusRequest
import com.project.solaria_mobile.feature.rating.data.RatingUpdateRequest
import com.project.solaria_mobile.feature.communication.data.ServiceCallDto
import com.project.solaria_mobile.feature.communication.data.ServiceScheduleRequest

@Singleton
class MockApiInterceptor @Inject constructor(
    private val mockMode: MockModeStore,
    private val tokens: TokenStore,
    private val data: MockApiData,
    private val images: MockImageCatalog,
) : Interceptor {
    init {
        data.emptyAccount.value = mockMode.isEmptyAccount()
    }

    private val apiHosts = listOf(
        BuildConfig.AUTH_BASE_URL,
        BuildConfig.CORE_BASE_URL,
        BuildConfig.MESSENGER_BASE_URL,
        BuildConfig.AI_ASSISTANT_BASE_URL,
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
            segments == listOf("messaging", "communities", "me") && method == "GET" ->
                success(ListSerializer(CommunityDto.serializer()), data.communitiesOf().map { data.communityForActor(it, actor) })
            segments.size == 3 && segments.take(2) == listOf("messaging", "communities") && method == "GET" ->
                data.communitiesOf().firstOrNull { it.id == segments[2] }?.let { success(CommunityDto.serializer(), data.communityForActor(it, actor)) } ?: notFound()
            segments.size == 3 && segments.take(2) == listOf("messaging", "communities") && method == "PATCH" -> {
                val body = decode<CommunityStatusRequest>(request.body?.let(::bodyText).orEmpty())
                data.setCommunityStatus(segments[2], body.status)?.let { success(CommunityDto.serializer(), data.communityForActor(it, actor)) } ?: notFound()
            }
            segments.size == 5 && segments.take(2) == listOf("messaging", "communities") && segments[3] == "members" && method == "DELETE" -> {
                val body = request.body?.let(::bodyText).orEmpty().takeIf { it.isNotBlank() }?.let { decode<CommunityRemovalRequest>(it) } ?: CommunityRemovalRequest()
                data.removeCommunityMember(segments[2], segments[4].let { if (it == actor) MockChatSeed.SELF else it }, body).let { (code, text) -> respond(chain, code, text) }
            }
            segments.size == 4 && segments.take(2) == listOf("messaging", "communities") && segments[3] == "members" && method == "POST" -> {
                val body = decode<CommunityMembersRequest>(request.body?.let(::bodyText).orEmpty())
                data.addCommunityMembers(segments[2], body.userIds.map { if (it == actor) MockChatSeed.SELF else it }.toSet(), body.role)?.let { success(CommunityDto.serializer(), data.communityForActor(it, actor)) } ?: notFound()
            }
            segments.size == 4 && segments.take(3) == listOf("api", "shifts", "technician") && method == "GET" ->
                success(ListSerializer(com.project.solaria_mobile.feature.discovery.data.ShiftDto.serializer()), data.shifts())
            segments == listOf("api", "shifts") && method == "POST" ->
                data.saveShift(null, decode(request.body?.let(::bodyText).orEmpty()))?.let { success(com.project.solaria_mobile.feature.discovery.data.ShiftDto.serializer(), it) } ?: respond(chain, 422, "{}")
            segments.size == 3 && segments[0] == "api" && segments[1] == "shifts" && method == "PUT" ->
                data.saveShift(segments[2], decode(request.body?.let(::bodyText).orEmpty()))?.let { success(com.project.solaria_mobile.feature.discovery.data.ShiftDto.serializer(), it) } ?: notFound()
            segments.size == 3 && segments[0] == "api" && segments[1] == "shifts" && method == "DELETE" ->
                if (data.deleteShift(segments[2])) empty() else notFound()
            segments == listOf("v1", "calendar", "connect") && method == "GET" -> {
                // No modo demonstração a autorização é simulada: abrir o endereço já conta como conectado.
                data.setCalendarConnected(true)
                success(com.project.solaria_mobile.feature.discovery.data.CalendarConnectDto.serializer(), com.project.solaria_mobile.feature.discovery.data.CalendarConnectDto("https://calendar.google.com/"))
            }
            segments.size == 4 && segments.take(3) == listOf("v1", "calendar", "technicians") && segments[3] == "availability" && method == "GET" ->
                data.calendarBusy()?.let { success(com.project.solaria_mobile.feature.discovery.data.CalendarAvailabilityDto.serializer(), com.project.solaria_mobile.feature.discovery.data.CalendarAvailabilityDto(it)) } ?: notFound()
            segments.size == 5 && segments.take(3) == listOf("v1", "calendar", "technicians") && segments[4] == "connection" && method == "DELETE" -> {
                data.setCalendarConnected(false)
                empty()
            }
            segments.size == 4 && segments.take(3) == listOf("api", "company-photos", "company") && method == "GET" ->
                success(ListSerializer(com.project.solaria_mobile.feature.profile.data.CompanyPhotoDto.serializer()), data.companyPhotos(segments[3]))
            segments == listOf("api", "technicians", "me", "profile") && method == "GET" ->
                success(com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto.serializer(), data.technicianProfile())
            segments == listOf("api", "technicians", "me", "profile") && method == "PUT" ->
                success(com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto.serializer(), data.saveTechnicianProfile(decode(request.body?.let(::bodyText).orEmpty())))
            segments == listOf("api", "technicians", "me", "profile", "completion") && method == "POST" ->
                data.completeTechnicianProfile()?.let { success(com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto.serializer(), it) } ?: respond(chain, 400, "{\"message\":\"Perfil incompleto\"}")
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
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "invite-links" && method == "GET" ->
                success(ListSerializer(ProjectInviteLinkDto.serializer()), data.inviteLinks(segments[2]))
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "invite-links" && method == "POST" -> {
                val body = decode<ProjectInviteLinkRequestDto>(request.body?.let(::bodyText).orEmpty())
                success(ProjectInviteLinkDto.serializer(), data.createInviteLink(segments[2], body))
            }
            segments.size == 5 && segments.take(2) == listOf("api", "projects") && segments[3] == "invite-links" && method == "DELETE" ->
                if (data.revokeInviteLink(segments[4])) empty() else notFound()
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
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "reports" && method == "POST" -> {
                val parts = (request.body as? MultipartBody)?.parts.orEmpty()
                fun partName(part: MultipartBody.Part) = Regex("name=\"([^\"]*)\"").find(part.headers?.get("Content-Disposition").orEmpty())?.groupValues?.get(1)
                fun text(name: String) = parts.firstOrNull { partName(it) == name }?.body?.let { Buffer().also(it::writeTo).readUtf8() }.orEmpty().trim()
                val images = parts.filter { partName(it) == "files" }.mapNotNull { part ->
                    Buffer().also(part.body::writeTo).readByteArray().takeIf { it.isNotEmpty() }?.let { it to (part.body.contentType()?.toString() ?: "image/jpeg") }
                }
                if (text("title").isEmpty() || text("content").isEmpty() || images.size > 5) respond(chain, 422, "{\"message\":\"Confira o relatório.\"}")
                else success(ProjectReportDto.serializer(), data.addReport(segments[2], text("title"), text("content"), images))
            }
            segments.size == 5 && segments.take(2) == listOf("api", "projects") && segments[3] == "reports" && method == "GET" ->
                data.report(segments[2], segments[4])?.let { success(ProjectReportDto.serializer(), it) } ?: notFound()
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "history" -> success(ListSerializer(ProjectHistoryDto.serializer()), data.history(segments[2]))
            segments.size == 4 && segments.take(2) == listOf("api", "projects") && segments[3] == "photos" -> success(ListSerializer(ProjectPhotoDto.serializer()), data.photos(segments[2], if (mockMode.isImagesEnabled()) images::forProject else null))
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
                val patch = decode<ConversationStatePatch>(request.body?.let(::bodyText).orEmpty())
                data.conversationState(segments[2], patch)?.let { success(ConversationStateDto.serializer(), it) } ?: notFound()
            }
            segments.size == 3 && segments.take(2) == listOf("messaging", "messages") && method == "PATCH" -> {
                val edit = decode<EditMessageRequest>(request.body?.let(::bodyText).orEmpty())
                data.mutateMessage(segments[2], request.header("Authorization").orEmpty(), edit.content).let { (code, body) -> respond(chain, code, body) }
            }
            segments.size == 3 && segments.take(2) == listOf("messaging", "messages") && method == "DELETE" ->
                data.mutateMessage(segments[2], request.header("Authorization").orEmpty(), null).let { (code, body) -> respond(chain, code, body) }
            segments == listOf("messaging", "conversations", "me") && request.url.queryParameter("archived") == "true" ->
                success(ListSerializer(ConversationDto.serializer()), data.archivedConversationList().map { data.forActor(it, actor) })
            segments == listOf("messaging", "conversations", "group") && method == "POST" -> {
                val body = decode<GroupConversationRequest>(request.body?.let(::bodyText).orEmpty())
                val created = data.createGroup(body, actor)
                respond(chain, 201, json.encodeToString(GroupCreationDto.serializer(), created.copy(conversation = data.forActor(created.conversation, actor))))
            }
            segments.size == 4 && segments.take(2) == listOf("messaging", "conversations") && segments[3] == "participants" && method == "POST" -> {
                val body = decode<ParticipantsRequest>(request.body?.let(::bodyText).orEmpty())
                data.addParticipants(segments[2], body.userIds, actor)?.let { success(ConversationDto.serializer(), data.forActor(it, actor)) } ?: respond(chain, 403, "{\"errorCode\":\"UNAUTHORIZED_ACCESS\"}")
            }
            segments.size == 4 && segments.take(2) == listOf("messaging", "conversations") && segments[3] == "manager" && method == "PUT" -> {
                val body = decode<TransferManagerRequest>(request.body?.let(::bodyText).orEmpty())
                if (data.transferManager(segments[2], body.successorUserId, actor)) respond(chain, 200, "{}") else respond(chain, 403, "{\"errorCode\":\"UNAUTHORIZED_ACCESS\"}")
            }
            segments.size == 5 && segments.take(2) == listOf("messaging", "conversations") && segments[3] == "participants" && method == "DELETE" ->
                data.removeParticipant(segments[2], segments[4], request.url.queryParameter("successorUserId"), actor).let { (code, text) -> respond(chain, code, text) }
            segments == listOf("messaging", "media") && method == "GET" -> {
                val id = request.url.queryParameter("conversationId")
                val limit = (request.url.queryParameter("limit")?.toIntOrNull() ?: 30).coerceIn(1, 100)
                val offset = request.url.queryParameter("cursor")?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val available = data.conversationIds().filter { id == null || it == id }.flatMap { conversationId ->
                    data.messages(conversationId).filter { !it.deleted && it.attachment?.mediaId != null }.map { message ->
                        com.project.solaria_mobile.feature.communication.data.MediaItemDto(message.id, conversationId, message.senderId, message.timestamp, message.attachment!!)
                    }
                }.sortedByDescending { it.timestamp }
                success(com.project.solaria_mobile.feature.communication.data.MediaPageDto.serializer(),
                    com.project.solaria_mobile.feature.communication.data.MediaPageDto(available.drop(offset).take(limit), (offset + limit).takeIf { it < available.size }?.toString()))
            }
            segments == listOf("messaging", "media") && method == "POST" -> {
                val body = decode<MediaUploadRequest>(request.body?.let(::bodyText).orEmpty())
                success(MediaUploadDto.serializer(), data.mediaUpload(body))
            }
            segments.size == 4 && segments.take(2) == listOf("messaging", "media") && segments[3] == "content" && method == "GET" ->
                data.mediaType(segments[2])?.let { respondBytes(chain, data.mediaBytes(it), if (it == "IMAGE") "image/png" else "application/octet-stream") } ?: respond(chain, 410, "{\"errorCode\":\"MEDIA_EXPIRED\"}")
            segments == listOf("messaging", "nps") && method == "POST" -> {
                val body = decode<NpsRequest>(request.body?.let(::bodyText).orEmpty())
                if (data.answerNps(body.conversationId)) respond(chain, 201, "{}") else respond(chain, 409, "{\"errorCode\":\"DUPLICATE_RESOURCE\"}")
            }
            segments == listOf("messaging", "ratings") && method == "POST" -> {
                val body = decode<RatingRequest>(request.body?.let(::bodyText).orEmpty())
                if (body.evaluatedId == actor) respond(chain, 422, "{\"errorCode\":\"BUSINESS_RULE_VIOLATION\"}") else respond(chain, 201, json.encodeToString(RatingDto.serializer(), data.createRating(body, actor)))
            }
            segments.size == 4 && segments.take(3) == listOf("messaging", "ratings", "evaluated") -> success(ListSerializer(RatingDto.serializer()), data.ratingsOf(evaluated = segments[3]))
            segments.size == 4 && segments.take(3) == listOf("messaging", "ratings", "evaluator") -> success(ListSerializer(RatingDto.serializer()), data.ratingsOf(evaluator = segments[3]))
            segments.size == 3 && segments.take(2) == listOf("messaging", "ratings") && method == "PATCH" -> {
                val body = decode<RatingUpdateRequest>(request.body?.let(::bodyText).orEmpty())
                data.updateRating(segments[2]) { it.copy(rating = body.rating ?: it.rating, comment = body.comment ?: it.comment) }?.let { success(RatingDto.serializer(), it) } ?: notFound()
            }
            segments.size == 4 && segments.take(2) == listOf("messaging", "ratings") && segments[3] == "status" && method == "PATCH" -> {
                val body = decode<RatingStatusRequest>(request.body?.let(::bodyText).orEmpty())
                data.updateRating(segments[2]) { it.copy(status = body.status) }?.let { success(RatingDto.serializer(), it) } ?: notFound()
            }
            segments.size == 5 && segments.take(3) == listOf("api", "users", "auth") && segments[4] == "summary" ->
                success(UserSummaryDto.serializer(), UserSummaryDto(MockChatSeed.names[segments[3]] ?: "Pessoa de demonstração"))
            segments == listOf("messaging", "conversations", "direct") && method == "POST" -> {
                val direct = decode<com.project.solaria_mobile.feature.communication.data.DirectConversationRequest>(request.body?.let(::bodyText).orEmpty())
                respond(chain, 201, json.encodeToString(ConversationDto.serializer(), data.directConversation(direct.recipientId)))
            }
            segments == listOf("messaging", "conversations", "me") -> success(ListSerializer(ConversationDto.serializer()), data.conversations().map { data.forActor(it, actor) })
            segments.size == 3 && segments.take(2) == listOf("messaging", "conversations") ->
                data.conversation(segments[2])?.let { success(ConversationDto.serializer(), data.forActor(it, actor)) } ?: notFound()
            segments.size == 4 && segments.take(3) == listOf("messaging", "messages", "conversation") -> success(ListSerializer(MessageDto.serializer()), data.messages(
                segments[3],
                before = request.url.queryParameter("beforeSequence")?.toIntOrNull(),
                since = request.url.queryParameter("sinceSequence")?.toIntOrNull(),
                limit = request.url.queryParameter("limit")?.toIntOrNull() ?: 100,
            ))
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
            segments == listOf("chat") && method == "POST" -> {
                val turn = decode<com.project.solaria_mobile.feature.communication.data.AssistantChatRequest>(request.body?.let(::bodyText).orEmpty())
                // O assistente real leva alguns segundos para responder; o atraso deixa ver o "pensando…".
                Thread.sleep(2_200)
                success(com.project.solaria_mobile.feature.communication.data.AssistantChatResponse.serializer(), data.assistantTurn(turn.conversationId, turn.message, actor))
            }
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
class MockApiData internal constructor(private val profileStore: MockProfileStore) {
    @Inject constructor(store: FileMockProfileStore) : this(store as MockProfileStore)
    constructor() : this(MockProfileStore.None)

    private val notificationList = mutableListOf(
        NotificationDto("demo-message", "NEW_MESSAGE", "Nova mensagem", "Há uma mensagem para você na conversa de demonstração.", createdAt = java.time.Instant.now().toString()),
        NotificationDto("demo-scheduled", "SERVICE_SCHEDULED", "Serviço agendado", "Seu serviço foi agendado. Confira a data e organize sua agenda.", createdAt = java.time.Instant.now().minusSeconds(3600).toString()),
        NotificationDto("demo-read", "SERVICE_STATUS_CHANGED", "Serviço atualizado", "As informações do serviço foram atualizadas.", createdAt = java.time.Instant.now().minusSeconds(7200).toString(), readAt = java.time.Instant.now().minusSeconds(1800).toString()),
    )

    @Synchronized fun notifications(): List<NotificationDto> = if (emptyAccount.value) emptyList() else notificationList.map { item ->
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
        ProjectDto("qa-project-1", "Usina Solar Horizonte", "Instalação fotovoltaica residencial.", "Equipe de demonstração", "qa-company-1", ownerCompanyLogoUrl = "mock-asset:company-logo-1.webp", createdAt = "2026-09-18T12:00:00Z", status = "IN_PROGRESS", location = location(-23.5505, -46.6333, "São Paulo", "SP", "Vila Mariana")),
        ProjectDto("qa-project-2", "Residencial Ipê", "Modernização do sistema de energia.", "Equipe de demonstração", "qa-company-1", ownerCompanyLogoUrl = "mock-asset:company-logo-1.webp", createdAt = "2026-09-23T12:00:00Z", status = "OPEN", location = location(-23.5614, -46.6559, "São Paulo", "SP", "Pinheiros")),
        ProjectDto("qa-project-3", "Sítio Boa Vista", "Projeto de geração distribuída.", "Equipe de demonstração", "qa-company-2", createdAt = "2026-10-01T12:00:00Z", status = "COMPLETED", location = location(-23.5880, -46.6790, "São Paulo", "SP", "Morumbi")),
    )
    private val demoProjectIds = projects.map { it.id }.toSet()
    private val people = mutableListOf(
        UserDto("qa-person-1", "Ana Oliveira", username = "ana.oliveira", avatar = "mock-asset:avatar-ana.webp", authId = "qa-person-1"),
        UserDto("qa-person-2", "Rafael Costa", username = "rafael.costa", avatar = "mock-asset:avatar-rafael.webp", authId = "qa-person-2"),
        UserDto("qa-person-3", "Marina Santos", username = "marina.santos", avatar = "mock-asset:avatar-marina.webp", authId = "qa-person-3"),
    )
    private val serviceStore = linkedMapOf(
        "qa-service-1" to ServiceCallDto("qa-service-1", "qa-project-1", "Usina Solar Horizonte", "Revisão dos inversores", "OPEN",
            scheduledDate = OffsetDateTime.now().plusDays(1).toString(), location = projects.first().location, canManage = true, hasExecutors = true),
        "qa-service-2" to ServiceCallDto("qa-service-2", "qa-project-2", "Residencial Ipê", "Inspeção da instalação", "IN_PROGRESS",
            scheduledDate = OffsetDateTime.now().toString(), acceptedAt = OffsetDateTime.now().minusHours(1).toString(), location = projects[1].location, canManage = true, hasExecutors = true),
        "qa-service-3" to ServiceCallDto("qa-service-3", "qa-project-3", "Sítio Boa Vista", "Manutenção preventiva", "COMPLETED",
            endDate = OffsetDateTime.now().minusDays(2).toString(), location = projects[2].location),
    )

    @Synchronized fun services() = if (emptyAccount.value) emptyList() else serviceStore.values.toList()
    @Synchronized fun service(id: String) = if (emptyAccount.value) null else serviceStore[id]
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
    private val seedTime = OffsetDateTime.now()
    private val conversationList = MockChatSeed.conversations(seedTime).toMutableList()
    private val conversationMessages = MockChatSeed.messages(seedTime)
    /** Até que mensagem cada conversa foi lida; a conversa de Ana e a do grupo começam com mensagens novas. */
    private val lastRead = conversationMessages.mapValues { (id, items) ->
        val last = items.maxOfOrNull { it.sequence } ?: 0
        when (id) { MockChatSeed.CONVERSATION_ANA -> last - 1; MockChatSeed.CONVERSATION_TEAM -> last - 3; else -> last }
    }.toMutableMap()
    /** Quem quiser reagir a uma mensagem enviada pela pessoa (o cliente de tempo real falso, para simular respostas). */
    @Volatile var messageListener: ((MessageDto) -> Unit)? = null
    /** Avisado com o id da conversa quando algo nela muda para a conta (mensagem, leitura, estado), como o `CONVERSATION_UPDATED` do messenger. */
    @Volatile var conversationListener: ((String) -> Unit)? = null
    private var nextProject = 4
    private var nextMessage = 1000
    private val selfId = "qa-mock-user"

    /** Conta recém-criada: sem os projetos, chamados, conversas, conexões e notificações de demonstração; o que a pessoa cria continua aparecendo. */
    val emptyAccount = kotlinx.coroutines.flow.MutableStateFlow(false)

    private fun visibleProjects() = if (emptyAccount.value) projects.filterNot { it.id in demoProjectIds } else projects.toList()

    @Synchronized fun projects() = visibleProjects()
    @Synchronized fun project(id: String) = visibleProjects().firstOrNull { it.id == id }?.copy(role = "OWNER")

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

    private val linkStore = mutableListOf<Pair<String, ProjectInviteLinkDto>>()
    private var nextLink = 1

    /** Como no servidor, a listagem não traz o endereço: ele só é devolvido na criação. */
    @Synchronized fun inviteLinks(projectId: String) =
        linkStore.filter { it.first == projectId && it.second.status == "ACTIVE" }.map { it.second.copy(url = null) }

    @Synchronized fun createInviteLink(projectId: String, request: ProjectInviteLinkRequestDto): ProjectInviteLinkDto {
        val number = nextLink++
        val link = ProjectInviteLinkDto("qa-link-$number", request.role, "ACTIVE", OffsetDateTime.now().plusDays(request.expiresInDays.toLong()).toString(),
            null, 0, "https://example.invalid/convite/qa-token-$number")
        linkStore += projectId to link
        return link
    }

    @Synchronized fun revokeInviteLink(id: String): Boolean {
        val index = linkStore.indexOfFirst { it.second.id == id }
        if (index < 0) return false
        linkStore[index] = linkStore[index].first to linkStore[index].second.copy(status = "REVOKED")
        return true
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
        it.copy(participantIds = it.participantIds.map { member -> if (member == selfId) actor else member }.toSet(), managerUserId = it.managerUserId?.let { manager -> if (manager == selfId) actor else manager })
    }.orEmpty()

    fun communityForActor(community: CommunityDto, actor: String) = community.copy(members = community.members.map {
        if (it.userId == selfId) it.copy(userId = actor) else it
    })

    /** Quem já é integrante só muda de papel (nunca para dono); quem não é entra com o papel pedido. */
    @Synchronized fun addCommunityMembers(id: String, users: Set<String>, role: String = "MEMBER"): CommunityDto? {
        val existing = communityStore[id] ?: return null
        val granted = if (role == "ADMIN") "ADMIN" else "MEMBER"
        val updated = existing.members.map { if (it.userId in users && it.role != "OWNER") it.copy(role = granted) else it }
        val added = users.filter { candidate -> existing.members.none { it.userId == candidate } }.map { CommunityMemberDto(it, granted) }
        return existing.copy(members = updated + added).also { communityStore[id] = it }
    }

    @Synchronized fun communitiesOf(): List<CommunityDto> = communityStore.values.toList()

    @Synchronized fun setCommunityStatus(id: String, status: String): CommunityDto? =
        communityStore[id]?.copy(status = status)?.also {
            communityStore[id] = it
            conversationList.replaceAll { conversation -> if (conversation.communityId == id) conversation.copy(communityStatus = status) else conversation }
            communityGroupStore[id].orEmpty().forEach { group -> conversationListener?.invoke(group.id) }
        }

    /** Tira o integrante da comunidade; 409 se ele gere um subgrupo com gente e nenhum sucessor foi indicado. */
    @Synchronized fun removeCommunityMember(id: String, userId: String, removal: CommunityRemovalRequest): Pair<Int, String> {
        val community = communityStore[id] ?: return 404 to "{}"
        val groups = communityGroupStore[id].orEmpty()
        val needing = groups.filter { it.managerUserId == userId && it.participantIds.any { member -> member != userId } }
        if (needing.any { group -> removal.successors.none { it.conversationId == group.id } }) return 409 to "{\"errorCode\":\"SUCCESSOR_REQUIRED\"}"
        communityStore[id] = community.copy(members = community.members.filterNot { it.userId == userId })
        communityGroupStore[id] = groups.map { group ->
            val successor = removal.successors.firstOrNull { it.conversationId == group.id }?.successorUserId
            group.copy(participantIds = group.participantIds - userId, managerUserId = if (group.managerUserId == userId) successor else group.managerUserId)
        }.toMutableList()
        return 204 to ""
    }

    @Synchronized fun createCommunityGroup(id: String, request: CommunityGroupRequest, actor: String): GroupCreationDto? {
        val community = communityStore[id] ?: return null
        val title = request.title
        if (community.members.size < 2 || title.isBlank() || title.length > 120) return null
        val participants = if (request.participantIds.isEmpty()) community.members.map { it.userId }.toSet()
            else (request.participantIds.map { if (it == actor) selfId else it }.toSet() + selfId).intersect(community.members.map { it.userId }.toSet())
        if (participants.size < 2) return null
        val groupId = "qa-group-${java.util.UUID.randomUUID()}"
        val group = CommunityGroupDto(groupId, title.trim(), participants, selfId)
        communityGroupStore.getOrPut(id) { mutableListOf() }.add(group)
        val conversation = ConversationDto(groupId, "GROUP", title.trim(), OffsetDateTime.now().toString(), participantIds = participants.toList(), managerUserId = selfId, communityId = id, communityStatus = community.status)
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

    @Synchronized fun history(projectId: String): List<ProjectHistoryDto> = reportStore[projectId].orEmpty().reversed().map { report ->
        ProjectHistoryDto("${report.id}-history", "REPORT_ADDED", report.title, report.author, report.createdAt, report.id)
    } + listOf(
        ProjectHistoryDto("$projectId-history-1", "Projeto criado", "Solicitação recebida", "Pessoa de demonstração", OffsetDateTime.now().minusDays(5).toString()),
        ProjectHistoryDto("$projectId-history-2", "Equipe atualizada", "Ana Oliveira adicionada", "Pessoa de demonstração", OffsetDateTime.now().minusDays(2).toString()),
    )

    // Relatórios criados no modo mock ficam só na memória; as imagens entram nas fotos do projeto, como no servidor.
    private val reportStore = mutableMapOf<String, MutableList<ProjectReportDto>>()
    private var nextReport = 1

    @Synchronized fun addReport(projectId: String, title: String, content: String, images: List<Pair<ByteArray, String>>): ProjectReportDto {
        val photos = images.map { (bytes, mime) -> addPhoto(projectId, bytes, mime) }
        val report = ProjectReportDto("qa-report-${nextReport++}", title, content, "Pessoa de demonstração", OffsetDateTime.now().toString(), photos)
        reportStore.getOrPut(projectId) { mutableListOf() } += report
        return report
    }

    @Synchronized fun report(projectId: String, reportId: String): ProjectReportDto? = reportStore[projectId].orEmpty().firstOrNull { it.id == reportId }

    // Fotos enviadas no modo mock ficam só na memória do app, para a galeria de demonstração funcionar sem servidor.
    private val photoStore = mutableMapOf<String, MutableList<ProjectPhotoDto>>()
    private val photoBytes = mutableMapOf<String, Pair<ByteArray, String>>()
    private var nextPhoto = 1
    private val seededPhotos = mutableSetOf<String>()
    private val demoPhotoIds = mutableSetOf<String>()

    /**
     * [demo] devolve as imagens de demonstração do projeto de índice dado; `null` as desliga. Com elas desligadas
     * a galeria mostra só o que foi enviado, o que permite testar o estado vazio. Sem arquivos no acervo
     * a galeria também fica vazia: nunca há imagens geradas no lugar das reais.
     */
    @Synchronized fun photos(projectId: String, demo: ((Int) -> List<MockImage>)? = null): List<ProjectPhotoDto> {
        if (demo != null && seededPhotos.add(projectId)) {
            val index = projects.indexOfFirst { it.id == projectId }
            if (index >= 0) {
                demo(index).forEach { demoPhotoIds += addPhoto(projectId, it.bytes, it.mime).id }
            }
        }
        val visible = photoStore[projectId].orEmpty().filter { demo != null || it.id !in demoPhotoIds }
        val withCover = if (visible.isNotEmpty() && visible.none { it.cover }) visible.mapIndexed { i, photo -> photo.copy(cover = i == 0) } else visible
        return withCover.sortedByDescending { it.cover }
    }

    @Synchronized fun addPhoto(projectId: String, bytes: ByteArray, mime: String): ProjectPhotoDto {
        val list = photoStore.getOrPut(projectId) { mutableListOf() }
        val photo = ProjectPhotoDto("qa-photo-${nextPhoto++}-${bytes.contentHashCode().toUInt().toString(16)}", cover = list.isEmpty())
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
        avatar = "mock-asset:avatar-own.webp",
        banner = "mock-asset:banner-own.webp",
        person = SelfPersonDto("Pessoa de demonstração", "(11) 98765-4321"),
        technician = SelfTechnicianDto("qa-technician", listOf(SelfProfessionDto("qa-profession", "Instalação solar"))),
        activeAffiliations = listOf(com.project.solaria_mobile.feature.profile.data.SelfAffiliationDto("INDEPENDENT")),
        companyMemberships = listOf(SelfMembershipDto("qa-company-1", "Equipe de demonstração", "Técnico")),
    )

    fun companyPhotos(companyId: String): List<com.project.solaria_mobile.feature.profile.data.CompanyPhotoDto> {
        val number = if (companyId == "qa-company-2") "2" else "1"
        fun photo(type: String, file: String) = com.project.solaria_mobile.feature.profile.data.CompanyPhotoDto("qa-company-photo-$type-$number", companyId, type, "mock-asset:$file")
        return listOfNotNull(photo("PROFILE", "company-logo-$number.webp"), photo("BANNER", "company-banner-1.webp").takeIf { number == "1" })
    }

    private var technicianProfile = profileStore.load() ?: com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto()

    @Synchronized fun technicianProfile(): com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto = technicianProfile

    @Synchronized fun saveTechnicianProfile(request: com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto): com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto {
        technicianProfile = withProgress(request.copy(completed = false))
        profileStore.save(technicianProfile)
        return technicianProfile
    }

    /** Conclui só quando o rascunho tem o endereço completo e uma especialidade, como o servidor. */
    @Synchronized fun completeTechnicianProfile(): com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto? {
        if (technicianProfile.missing.isNotEmpty()) return null
        technicianProfile = technicianProfile.copy(completed = true)
        profileStore.save(technicianProfile)
        return technicianProfile
    }

    /** Apaga o perfil, para o onboarding recomeçar do zero. */
    @Synchronized fun resetTechnicianProfile() {
        technicianProfile = com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto()
        profileStore.save(null)
    }

    private fun withProgress(profile: com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto): com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto {
        val a = profile.address
        val address = a != null && listOf(a.street, a.number, a.neighborhood, a.city).all { !it.isNullOrBlank() } &&
            a.zipCode.orEmpty().length == 8 && a.state.orEmpty().length == 2
        val other = profile.specialties.contains("OTHER") && profile.otherSpecialty.isNullOrBlank()
        val specialties = profile.specialties.isNotEmpty() && !other
        val done = listOf(address, specialties, profile.yearsExperience != null, profile.serviceRadiusKm != null, !profile.bio.isNullOrBlank()).count { it }
        return profile.copy(progress = done * 100 / 5, missing = listOfNotNull("ADDRESS".takeIf { !address }, "SPECIALTIES".takeIf { profile.specialties.isEmpty() }, "OTHER_SPECIALTY".takeIf { other }, "EXPERIENCE".takeIf { profile.yearsExperience == null }))
    }

    private val shifts = mutableListOf<com.project.solaria_mobile.feature.discovery.data.ShiftDto>()
    private var shiftSeed = 0
    private var calendarConnected = false

    @Synchronized fun shifts(): List<com.project.solaria_mobile.feature.discovery.data.ShiftDto> = shifts.toList()

    @Synchronized fun saveShift(id: String?, body: com.project.solaria_mobile.feature.discovery.data.ShiftRequestDto): com.project.solaria_mobile.feature.discovery.data.ShiftDto? {
        val shift = com.project.solaria_mobile.feature.discovery.data.ShiftDto(id ?: "qa-shift-${++shiftSeed}", body.technicianId, body.dayWeek, body.startDate, body.endDate)
        if (id == null) shifts += shift
        else {
            val index = shifts.indexOfFirst { it.id == id }
            if (index < 0) return null
            shifts[index] = shift
        }
        return shift
    }

    @Synchronized fun deleteShift(id: String): Boolean = shifts.removeAll { it.id == id }

    @Synchronized fun setCalendarConnected(connected: Boolean) { calendarConnected = connected }

    @Synchronized fun calendarBusy(): List<com.project.solaria_mobile.feature.discovery.data.CalendarSlotDto>? {
        if (!calendarConnected) return null
        val day = java.time.LocalDate.now()
        fun slot(hourStart: Int, hourEnd: Int) = com.project.solaria_mobile.feature.discovery.data.CalendarSlotDto(
            day.atTime(hourStart, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toString(),
            day.atTime(hourEnd, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toString(),
        )
        return listOf(slot(12, 13), slot(15, 16))
    }

    @Synchronized fun connections() = if (emptyAccount.value) emptyList() else people.filter { it.id in connectedIds }.map { it.copy(connected = true) }

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

    @Synchronized fun conversations() = if (emptyAccount.value) emptyList() else conversationList.filterNot { it.id in archivedConversations }
        .map { it.copy(muted = it.id in mutedConversations, archived = false, unreadCount = unread(it.id).toLong(), lastMessage = lastMessage(it.id)) }
        .sortedByDescending { conversationMessages[it.id]?.lastOrNull()?.timestamp.orEmpty() }

    private fun unread(id: String): Int = conversationMessages[id].orEmpty().count { it.senderId != MockChatSeed.SELF && it.sequence > (lastRead[id] ?: 0) }

    private fun lastMessage(id: String): MessagePreviewDto? = conversationMessages[id]?.lastOrNull()?.let { message ->
        val label = when (message.attachment?.type) {
            "IMAGE" -> "Foto"
            "FILE" -> message.attachment.name.orEmpty().ifBlank { "Arquivo" }
            "LOCATION" -> "Localização"
            else -> null
        }
        MessagePreviewDto(message.id, message.senderId, if (message.deleted) "" else listOfNotNull(label, message.content.takeIf { it.isNotBlank() }).joinToString(": "), message.deleted)
    }

    /** A conversa como a lista a mostra agora, com a última interação na hora da última mensagem; nulo se foi arquivada. */
    @Synchronized fun liveConversation(id: String): ConversationDto? =
        conversations().firstOrNull { it.id == id }?.let { it.copy(lastInteractionAt = conversationMessages[id]?.lastOrNull()?.timestamp ?: it.lastInteractionAt) }

    /** Quem está online, como o retrato de presença que o messenger manda ao assinar a conversa. */
    fun presence(conversationId: String): Map<String, Boolean> = MockChatSeed.presence[conversationId].orEmpty()

    /** Mensagem que chega de outra pessoa, como se o messenger a tivesse recebido. */
    @Synchronized fun receive(conversationId: String, senderId: String, text: String, attachment: AttachmentDto? = null): MessageDto {
        val list = conversationMessages.getOrPut(conversationId) { mutableListOf() }
        val sequence = (list.maxOfOrNull { it.sequence } ?: 0) + 1
        val message = MessageDto("qa-message-${nextMessage++}", conversationId, senderId, text, OffsetDateTime.now().toString(), sequence, senderName = MockChatSeed.names[senderId], attachment = attachment)
        list.add(message)
        return message
    }

    fun conversationIds(): List<String> = conversationList.map { it.id }

    private var nextGroup = 1
    private var nextMedia = 1
    private val mediaStore = mutableMapOf<String, String>()
    private val npsAnswered = mutableSetOf<String>()
    private val ratingStore = mutableListOf<RatingDto>()
    private var nextRating = 1

    /** A conta de demonstração aparece com o identificador do token da sessão, como nas comunidades. */
    fun forActor(conversation: ConversationDto, actor: String): ConversationDto = conversation.copy(
        participantIds = conversation.participantIds.map { if (it == selfId) actor else it },
        managerUserId = conversation.managerUserId?.let { if (it == selfId) actor else it },
    )

    @Synchronized fun archivedConversationList(): List<ConversationDto> =
        if (emptyAccount.value) emptyList() else conversationList.filter { it.id in archivedConversations }.map { it.copy(archived = true, unreadCount = 0, lastMessage = lastMessage(it.id)) }

    @Synchronized fun createGroup(request: GroupConversationRequest, actor: String): GroupCreationDto {
        val participants = (request.participantIds.map { if (it == actor) selfId else it }.toSet() + selfId).toList()
        val conversation = ConversationDto("qa-group-${nextGroup++}", "GROUP", request.title.trim(), OffsetDateTime.now().toString(), participantIds = participants, managerUserId = selfId)
        conversationList.add(conversation)
        conversationMessages[conversation.id] = mutableListOf()
        conversationListener?.invoke(conversation.id)
        return GroupCreationDto(conversation)
    }

    /** Só o gestor adiciona; quem já está no grupo conta como recusado, como o servidor. */
    @Synchronized fun addParticipants(id: String, users: Set<String>, actor: String): ConversationDto? {
        val index = conversationList.indexOfFirst { it.id == id }
        val conversation = conversationList.getOrNull(index)?.takeIf { it.managerUserId == selfId } ?: return null
        val incoming = users.map { if (it == actor) selfId else it }
        val fresh = incoming.filterNot { it in conversation.participantIds }
        val updated = conversation.copy(participantIds = conversation.participantIds + fresh)
        conversationList[index] = updated
        conversationListener?.invoke(id)
        return updated.copy(rejectedParticipants = (incoming - fresh.toSet()).map { RejectedGroupParticipantDto(it, "ALREADY_PARTICIPANT") })
    }

    @Synchronized fun transferManager(id: String, successor: String, actor: String): Boolean {
        val index = conversationList.indexOfFirst { it.id == id }
        val conversation = conversationList.getOrNull(index)?.takeIf { it.managerUserId == selfId } ?: return false
        val target = if (successor == actor) selfId else successor
        if (target !in conversation.participantIds) return false
        conversationList[index] = conversation.copy(managerUserId = target)
        conversationListener?.invoke(id)
        return true
    }

    /** Sair é remover a própria conta; o gestor que sai com gente no grupo precisa indicar o sucessor. */
    @Synchronized fun removeParticipant(id: String, userId: String, successor: String?, actor: String): Pair<Int, String> {
        val index = conversationList.indexOfFirst { it.id == id }
        val conversation = conversationList.getOrNull(index) ?: return 404 to "{}"
        val target = if (userId == actor) selfId else userId
        if (target !in conversation.participantIds) return 404 to "{}"
        val others = conversation.participantIds - target
        if (target != selfId && conversation.managerUserId != selfId) return 403 to "{\"errorCode\":\"UNAUTHORIZED_ACCESS\"}"
        val manager = conversation.managerUserId
        var nextManager = manager
        if (target == manager && others.isNotEmpty()) {
            if (successor == null) return 409 to "{\"errorCode\":\"SUCCESSOR_REQUIRED\"}"
            val chosen = if (successor == actor) selfId else successor
            if (chosen !in others) return 409 to "{\"errorCode\":\"SUCCESSOR_INELIGIBLE\"}"
            nextManager = chosen
        }
        if (target == selfId) {
            conversationList.removeAt(index)
            conversationMessages.remove(id)
        } else conversationList[index] = conversation.copy(participantIds = others, managerUserId = nextManager)
        conversationListener?.invoke(id)
        return 204 to ""
    }

    @Synchronized fun mediaUpload(request: MediaUploadRequest): MediaUploadDto {
        val id = "qa-media-${nextMedia++}"
        mediaStore[id] = request.type
        return MediaUploadDto(id, MediaUploadTargetDto("mock://upload/$id", "PUT", emptyMap()), maxBytes = request.sizeBytes)
    }

    @Synchronized fun mediaType(id: String): String? = mediaStore[id]

    /** Uma imagem de 1 pixel para as fotos; os demais tipos baixam um texto qualquer. */
    fun mediaBytes(type: String): ByteArray =
        if (type == "IMAGE") java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
        else "arquivo de demonstração".toByteArray()

    /** Uma nota por sessão: a segunda volta 409, como o servidor. */
    @Synchronized fun answerNps(conversationId: String): Boolean = npsAnswered.add(conversationId)

    @Synchronized fun createRating(request: RatingRequest, actor: String): RatingDto =
        RatingDto("qa-rating-${nextRating++}", actor, request.evaluatedId, request.typeRate, request.comment, request.rating, "ACTIVE", OffsetDateTime.now().toString()).also { ratingStore.add(it) }

    @Synchronized fun ratingsOf(evaluated: String? = null, evaluator: String? = null): List<RatingDto> =
        ratingStore.filter { (evaluated == null || it.evaluatedId == evaluated) && (evaluator == null || it.evaluatorId == evaluator) }

    @Synchronized fun updateRating(id: String, change: (RatingDto) -> RatingDto): RatingDto? {
        val index = ratingStore.indexOfFirst { it.id == id }
        if (index < 0) return null
        return change(ratingStore[index]).also { ratingStore[index] = it }
    }

    private var nextAssistantSession = 1
    private var nextDirect = 1

    /** Conversa direta com a pessoa: a que já existe, como o messenger, ou uma nova e vazia. */
    @Synchronized fun directConversation(recipientId: String): ConversationDto {
        val known = mapOf(
            MockChatSeed.ANA to MockChatSeed.CONVERSATION_ANA, MockChatSeed.RAFAEL to MockChatSeed.CONVERSATION_RAFAEL,
            MockChatSeed.MARINA to MockChatSeed.CONVERSATION_MARINA, MockChatSeed.BEATRIZ to MockChatSeed.CONVERSATION_EMPTY,
        )
        known[recipientId]?.let { id -> conversationList.firstOrNull { it.id == id }?.let { return it } }
        val created = ConversationDto(
            "qa-conversation-d${nextDirect++}", "USER", MockChatSeed.names[recipientId] ?: "Pessoa", OffsetDateTime.now().toString(),
        )
        conversationList.add(created)
        conversationMessages[created.id] = mutableListOf()
        return created
    }

    /**
     * Um turno do assistente: acha (ou cria, na primeira mensagem) a conversa da sessão `threadId`, grava a pergunta e a resposta
     * nela e devolve o texto, como o ai-assistant faz no api-messenger.
     */
    @Synchronized fun assistantTurn(threadId: String, message: String, actor: String): com.project.solaria_mobile.feature.communication.data.AssistantChatResponse {
        val existing = conversationList.firstOrNull { (it.userDetails?.get("threadId") as? kotlinx.serialization.json.JsonPrimitive)?.content == threadId }
        val conversation = existing ?: ConversationDto(
            "qa-assistant-${nextAssistantSession++}", "CHAT_BOT", null, OffsetDateTime.now().toString(),
            userDetails = mapOf("threadId" to kotlinx.serialization.json.JsonPrimitive(threadId)),
        ).also { conversationList.add(it); conversationMessages[it.id] = mutableListOf() }
        receive(conversation.id, actor, message)
        val reply = assistantReply(message)
        val saved = receive(conversation.id, MockChatSeed.ASSISTANT, reply)
        lastRead[conversation.id] = saved.sequence
        return com.project.solaria_mobile.feature.communication.data.AssistantChatResponse(reply, "ANSWERED", null, conversation.id)
    }

    private fun assistantReply(message: String): String = when {
        message.contains("instal", ignoreCase = true) -> "Para **instalação** de painéis solares, o mais comum é:\n\n1. Visita técnica\n2. Orçamento e projeto\n3. Instalação (1 a 3 dias)\n4. Vistoria da concessionária\n\nQuer que eu procure profissionais de instalação na sua região?"
        message.contains("profission", ignoreCase = true) -> "Encontrei **3 profissionais** de instalação próximos de você, com experiência entre 4 e 9 anos. Posso detalhar cada um, se quiser."
        else -> "Entendi. Posso ajudar com **projetos**, **profissionais** e dúvidas sobre o app. Pode dar mais detalhes do que você precisa?"
    }
    @Synchronized fun conversation(id: String) = conversationList.firstOrNull { it.id == id }
        ?.let { it.copy(muted = it.id in mutedConversations, unreadCount = unread(it.id).toLong()) }

    /** Estado pessoal: sem campos, só devolve o atual, como o messenger. */
    @Synchronized fun conversationState(id: String, patch: ConversationStatePatch): ConversationStateDto? {
        if (conversation(id) == null) return null
        patch.muted?.let { if (it) mutedConversations += id else mutedConversations -= id }
        patch.archived?.let { if (it) archivedConversations += id else archivedConversations -= id }
        patch.lastReadSequence?.let { lastRead[id] = maxOf(lastRead[id] ?: 0, it) }
        conversationListener?.invoke(id)
        return ConversationStateDto(id, id in mutedConversations, id in archivedConversations)
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
            val updated = if (content == null) current.copy(content = "", deleted = true) else current.copy(content = content, edited = true)
            messages[index] = updated
            conversationListener?.invoke(updated.conversationId)
            return 200 to ApiJson.instance.encodeToString(MessageDto.serializer(), updated)
        }
        return 404 to "{}"
    }
    @Synchronized fun messages(id: String, before: Int? = null, since: Int? = null, limit: Int = 100): List<MessageDto> {
        val all = conversationMessages[id].orEmpty()
        return if (since != null) all.filter { it.sequence > since }.take(limit)
        else all.filter { before == null || it.sequence < before }.takeLast(limit)
    }

    @Synchronized fun sendMessage(request: SendMessageRequest, authorization: String): MessageDto {
        val id = "qa-message-${nextMessage++}"
        val list = conversationMessages.getOrPut(request.conversationId) { mutableListOf() }
        val message = MessageDto(
            id, request.conversationId, subjectFrom(authorization) ?: selfId, request.content, OffsetDateTime.now().toString(),
            (list.maxOfOrNull { it.sequence } ?: 0) + 1, request.clientMessageId, senderName = "Você", attachment = request.attachment,
        )
        list.add(message)
        lastRead[request.conversationId] = message.sequence
        messageListener?.invoke(message)
        conversationListener?.invoke(message.conversationId)
        return message
    }

    fun geocode(address: String): AddressListDto {
        val offset = (address.hashCode() and 0x3ff) / 100000.0
        val query = address.trim()
        // Como o geocoder real, devolve mais de um candidato: o primeiro é o que casa melhor.
        return AddressListDto(
            listOf(
                AddressDto("$query, São Paulo - SP, Brasil", -23.5505 + offset, -46.6333 + offset),
                AddressDto("$query, Guarulhos - SP, Brasil", -23.4543 + offset, -46.5333 + offset),
                AddressDto("$query, Santo André - SP, Brasil", -23.6639 + offset, -46.5383 + offset),
            ),
        )
    }

    fun mockTokens(): TokenResponse = TokenResponse(MOCK_ACCESS_TOKEN, "qa-mock-refresh-token")

    private fun location(latitude: Double, longitude: Double, city: String, state: String, neighborhood: String) =
        ProjectLocationDto(latitude, longitude, OffsetDateTime.now().minusDays(1).toString(), city, state, neighborhood, zipCode = "01000-000", street = "Rua de demonstração", number = "120")

    private fun subjectFrom(authorization: String): String? = runCatching {
        val token = authorization.removePrefix("Bearer ").split('.')[1]
        val payload = String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8)
        Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
    }.getOrNull()

    private companion object {
        private fun unsignedJwtPart(json: String) =
            Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray(Charsets.UTF_8))

        val MOCK_ACCESS_TOKEN: String = listOf(
            unsignedJwtPart("""{"alg":"none"}"""),
            unsignedJwtPart("""{"sub":"qa-mock-user","email":"pessoa@demonstracao.test","exp":9999999999}"""),
            "mock",
        ).joinToString(".")
    }
}
