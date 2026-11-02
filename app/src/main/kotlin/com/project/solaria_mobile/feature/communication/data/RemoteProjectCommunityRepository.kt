package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.accessTokenSubject
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.communication.domain.CommunityGroup
import com.project.solaria_mobile.feature.communication.domain.CommunityPerson
import com.project.solaria_mobile.feature.communication.domain.CommunitySummary
import com.project.solaria_mobile.feature.communication.domain.GroupCreated
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunity
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunityRepository
import com.project.solaria_mobile.feature.project.data.ProjectApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

@Singleton
class RemoteProjectCommunityRepository @Inject constructor(
    retrofit: RetrofitFactory,
    private val projects: ProjectApi,
    private val credentials: CredentialProvider,
    private val executor: ApiCallExecutor,
    private val reader: OfflineReader,
    private val network: NetworkMonitor,
    private val mock: MockModeStore,
) : ProjectCommunityRepository {
    private val api = retrofit.create(BuildConfig.MESSENGER_BASE_URL).create(CommunityApi::class.java)

    override suspend fun read(projectId: String) = reader.readResult("community:$projectId", ProjectCommunity.serializer()) {
        val project = projects.project(projectId)
        val people = projects.members(projectId).mapNotNull { member ->
            member.authId?.let { CommunityPerson(it, member.name?.takeIf(String::isNotBlank) ?: "Integrante do projeto", false) }
        }.distinctBy { it.authId }
        val community = try { api.community(projectId) } catch (error: HttpException) {
            if (error.code() == 404) null else throw error
        }
        val mutable = !project.archived && project.status in setOf("OPEN", "IN_PROGRESS")
        if (community == null) ProjectCommunity(projectId = projectId, canCreate = project.role == "OWNER" && mutable, people = people)
        else {
            require(community.projectId == projectId)
            val me = credentials.accessToken()?.let(::accessTokenSubject)
            val groups = api.groups(community.id).map { group ->
                CommunityGroup(group.id, group.title?.takeIf(String::isNotBlank) ?: "Conversa do projeto", group.participantIds.size,
                    community.status == "ACTIVE", me != null && me in group.participantIds, group.managerUserId, group.participantIds.toList())
            }
            val role = community.members.firstOrNull { it.userId == me }?.role
            ProjectCommunity(community.id, projectId, community.status == "ACTIVE" && mutable, false, community.members.size, groups,
                role in setOf("OWNER", "ADMIN"),
                people.map { person ->
                    val member = community.members.firstOrNull { it.userId == person.authId }
                    person.copy(joined = member != null, self = person.authId == me, role = member?.role)
                },
                role, community.status == "ARCHIVED")
        }
    }

    override suspend fun create(projectId: String): ApiResult<Unit> = online {
        val project = projects.project(projectId)
        if (project.role != "OWNER" || project.archived || project.status !in setOf("OPEN", "IN_PROGRESS")) return@online ApiResult.Failure(ErrorKind.FORBIDDEN)
        val members = projects.members(projectId).mapNotNull { it.authId }.toSet()
        when (val result = executor.execute { api.create(CommunityCreateRequest(projectId, members)) }) {
            is ApiResult.Success -> {
                reader.invalidate("community:$projectId")
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> result
        }
    }

    override suspend fun createGroup(communityId: String, title: String, participants: Set<String>): ApiResult<GroupCreated> = online {
        val normalized = title.trim()
        if (normalized.isEmpty() || normalized.length > 120) return@online ApiResult.Failure(ErrorKind.VALIDATION)
        executor.execute { api.createGroup(communityId, CommunityGroupRequest(normalized, participants)) }
            .map { GroupCreated(it.conversation.id, it.rejectedParticipants.size) }
    }

    override suspend fun addMember(communityId: String, authId: String): ApiResult<Unit> = online {
        executor.execute { api.addMember(communityId, CommunityMembersRequest(setOf(authId))); Unit }
    }

    override suspend fun setArchived(communityId: String, archived: Boolean): ApiResult<Unit> = online {
        executor.execute { api.updateStatus(communityId, CommunityStatusRequest(if (archived) "ARCHIVED" else "ACTIVE")); Unit }
    }

    override suspend fun setAdmin(communityId: String, authId: String, admin: Boolean): ApiResult<Unit> = online {
        executor.execute { api.addMember(communityId, CommunityMembersRequest(setOf(authId), if (admin) "ADMIN" else "MEMBER")); Unit }
    }

    override suspend fun removeMember(communityId: String, authId: String, successors: Map<String, String>): ApiResult<Unit> = online {
        val body = CommunityRemovalRequest(successors.map { (conversationId, successor) -> CommunitySuccessorDto(conversationId, successor) })
        var result: ApiResult<Unit> = executor.execute { api.removeMember(communityId, authId, body) }
        // Uma falha no meio da cascata não desfaz nada: repetir o mesmo pedido continua de onde parou.
        var attempts = 1
        while (result is ApiResult.Failure && result.errorCode == "REMOVAL_INCOMPLETE" && attempts < REMOVAL_ATTEMPTS) {
            result = executor.execute { api.removeMember(communityId, authId, body) }
            attempts++
        }
        result
    }

    override suspend fun mine(): ApiResult<List<CommunitySummary>> = online {
        val me = credentials.accessToken()?.let(::accessTokenSubject)
        when (val result = executor.execute { api.mine() }) {
            is ApiResult.Failure -> result
            // O nome do projeto vem do api-core; se ele não responde, a comunidade aparece mesmo assim.
            is ApiResult.Success -> ApiResult.Success(result.data.map { community ->
                val name = runCatching { projects.project(community.projectId).name }.getOrNull().orEmpty()
                CommunitySummary(community.id, community.projectId, name, community.status == "ARCHIVED", community.members.size, community.members.firstOrNull { it.userId == me }?.role)
            })
        }
    }

    private suspend fun <T> online(block: suspend () -> ApiResult<T>): ApiResult<T> =
        if (mock.isEnabled() || network.isOnline.first()) executor.execute(block).let { result ->
            when (result) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> result
            }
        } else ApiResult.Failure(ErrorKind.CONNECTIVITY)
}

private const val REMOVAL_ATTEMPTS = 3

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectCommunityModule {
    @Binds abstract fun repository(implementation: RemoteProjectCommunityRepository): ProjectCommunityRepository
}
