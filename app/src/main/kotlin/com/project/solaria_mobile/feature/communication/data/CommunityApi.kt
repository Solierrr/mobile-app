package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class CommunityMemberDto(
    val userId: String,
    val role: String = "MEMBER",
    val joinedAt: String? = null,
)

@Serializable
data class CommunityDto(
    val id: String,
    val projectId: String? = null,
    val members: List<CommunityMemberDto> = emptyList(),
    val status: String = "ACTIVE",
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CommunityRequest(
    val projectId: String,
    val memberIds: List<String> = emptyList(),
)

@Serializable
data class CommunityStatusRequest(val status: String)

@Serializable
data class CommunityMembersRequest(
    val userIds: List<String>,
    val role: String? = null,
)

@Serializable
data class SuccessorDto(
    val conversationId: String,
    val successorUserId: String,
)

@Serializable
data class MemberRemovalRequest(val successors: List<SuccessorDto> = emptyList())

@Serializable
data class CommunityConversationRequest(
    val title: String,
    val participantIds: List<String> = emptyList(),
)

@Serializable
data class RejectedParticipantDto(
    val userId: String,
    val reasonCode: String? = null,
)

@Serializable
data class GroupCreationDto(
    val conversation: ConversationDto,
    val admittedParticipantIds: List<String> = emptyList(),
    val rejectedParticipants: List<RejectedParticipantDto> = emptyList(),
)

@Serializable
data class SubgroupDto(
    val id: String,
    val title: String? = null,
    val status: String? = null,
    val participantIds: List<String> = emptyList(),
    val managerUserId: String? = null,
    val startedAt: String? = null,
    val lastInteractionAt: String? = null,
)

/**
 * Rotas de comunidades do api-messenger (`/messaging/communities`), na mesma base do [MessengerApi].
 *
 * Uma comunidade é o grupo de um projeto, com papéis `OWNER`, `ADMIN` e `MEMBER`. As conversas dela são grupos
 * comuns com `communityId`, e chegam ao app pela lista de conversas e pelo stream de eventos, sem rota própria.
 */
interface CommunityApi {
    @POST("messaging/communities")
    suspend fun create(@Body body: CommunityRequest): CommunityDto

    @GET("messaging/communities/me")
    suspend fun mine(): List<CommunityDto>

    @GET("messaging/communities/{id}")
    suspend fun community(@Path("id") id: String): CommunityDto

    @GET("messaging/communities/project/{projectId}")
    suspend fun byProject(@Path("projectId") projectId: String): CommunityDto

    @PATCH("messaging/communities/{id}")
    suspend fun setStatus(@Path("id") id: String, @Body body: CommunityStatusRequest): CommunityDto

    @POST("messaging/communities/{id}/members")
    suspend fun addMembers(@Path("id") id: String, @Body body: CommunityMembersRequest): CommunityDto

    // Resposta 204 sem corpo. Retorna Response porque o Retrofit não lê corpo vazio como Unit em todas as versões;
    // o repositório confere o código e lança o erro HTTP para o ApiCallExecutor classificar.
    @HTTP(method = "DELETE", path = "messaging/communities/{id}/members/{userId}", hasBody = true)
    suspend fun removeMember(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: MemberRemovalRequest,
    ): Response<Unit>

    @POST("messaging/communities/{id}/conversations")
    suspend fun createConversation(@Path("id") id: String, @Body body: CommunityConversationRequest): GroupCreationDto

    @GET("messaging/communities/{id}/conversations")
    suspend fun subgroups(@Path("id") id: String): List<SubgroupDto>
}
