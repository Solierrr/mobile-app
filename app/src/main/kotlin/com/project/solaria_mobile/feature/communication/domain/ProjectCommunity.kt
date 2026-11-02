package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult
import kotlinx.serialization.Serializable

@Serializable
data class CommunityGroup(val id: String, val title: String, val participants: Int, val active: Boolean, val canOpen: Boolean)

@Serializable data class CommunityPerson(val authId: String, val name: String, val joined: Boolean, val self: Boolean = false)

@Serializable
data class ProjectCommunity(
    val id: String? = null,
    val projectId: String,
    val active: Boolean = true,
    val canCreate: Boolean = false,
    val members: Int = 0,
    val groups: List<CommunityGroup> = emptyList(),
    val canManage: Boolean = false,
    val people: List<CommunityPerson> = emptyList(),
)

data class GroupCreated(val conversationId: String, val rejectedParticipants: Int)

interface ProjectCommunityRepository {
    suspend fun read(projectId: String): ReadResult<ProjectCommunity>
    suspend fun create(projectId: String): ApiResult<Unit>
    suspend fun createGroup(communityId: String, title: String, participants: Set<String> = emptySet()): ApiResult<GroupCreated>
    suspend fun addMember(communityId: String, authId: String): ApiResult<Unit>
}
