package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable data class CommunityMemberDto(val userId: String, val role: String)
@Serializable data class CommunityDto(val id: String, val projectId: String, val members: List<CommunityMemberDto> = emptyList(), val status: String)
/** O messenger não manda `status` no subgrupo: ele vale o da comunidade, que é `ACTIVE` ou `ARCHIVED`. */
@Serializable data class CommunityGroupDto(val id: String, val title: String? = null, val participantIds: Set<String> = emptySet())
@Serializable data class CommunityCreateRequest(val projectId: String, val memberIds: Set<String> = emptySet())
@Serializable data class CommunityMembersRequest(val userIds: Set<String>, val role: String = "MEMBER")
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
}
