package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable data class CommunityMemberDto(val userId: String, val role: String)
@Serializable data class CommunityDto(val id: String, val projectId: String, val members: List<CommunityMemberDto> = emptyList(), val status: String)
/** O messenger não manda `status` no subgrupo: ele vale o da comunidade, que é `ACTIVE` ou `ARCHIVED`. */
@Serializable data class CommunityGroupDto(val id: String, val title: String? = null, val participantIds: Set<String> = emptySet(), val managerUserId: String? = null)
@Serializable data class CommunityCreateRequest(val projectId: String, val memberIds: Set<String> = emptySet())
@Serializable data class CommunityMembersRequest(val userIds: Set<String>, val role: String = "MEMBER")
@Serializable data class CommunityStatusRequest(val status: String)
/** Quem assume cada subgrupo que o integrante removido gerencia. */
@Serializable data class CommunitySuccessorDto(val conversationId: String, val successorUserId: String)
@Serializable data class CommunityRemovalRequest(val successors: List<CommunitySuccessorDto> = emptyList())
@Serializable data class CommunityGroupRequest(val title: String, val participantIds: Set<String> = emptySet())
@Serializable data class GroupCreationDto(val conversation: ConversationDto, val rejectedParticipants: List<RejectedGroupParticipantDto> = emptyList())
@Serializable data class RejectedGroupParticipantDto(val userId: String? = null, val reasonCode: String? = null)

interface CommunityApi {
    @GET("messaging/communities/project/{projectId}")
    suspend fun community(@Path("projectId") projectId: String): CommunityDto

    @GET("messaging/communities/{id}/conversations")
    suspend fun groups(@Path("id") id: String): List<CommunityGroupDto>

    @POST("messaging/communities")
    suspend fun create(@Body body: CommunityCreateRequest): CommunityDto

    @POST("messaging/communities/{id}/conversations")
    suspend fun createGroup(@Path("id") id: String, @Body body: CommunityGroupRequest): GroupCreationDto

    @POST("messaging/communities/{id}/members")
    suspend fun addMember(@Path("id") id: String, @Body body: CommunityMembersRequest): CommunityDto

    @GET("messaging/communities/me")
    suspend fun mine(): List<CommunityDto>

    /** Arquivar ou reativar a comunidade (`ACTIVE` ou `ARCHIVED`); só OWNER e ADMIN. */
    @PATCH("messaging/communities/{id}")
    suspend fun updateStatus(@Path("id") id: String, @Body body: CommunityStatusRequest): CommunityDto

    /** DELETE com corpo: os sucessores dos subgrupos que o integrante gere. 503 `REMOVAL_INCOMPLETE` pede para repetir o mesmo pedido. */
    @HTTP(method = "DELETE", path = "messaging/communities/{id}/members/{userId}", hasBody = true)
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String, @Body body: CommunityRemovalRequest)
}
