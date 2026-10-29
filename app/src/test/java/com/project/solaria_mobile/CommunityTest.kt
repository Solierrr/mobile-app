package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.database.LocalDataWiper
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.feature.communication.CommunityNotice
import com.project.solaria_mobile.feature.communication.CommunityUiState
import com.project.solaria_mobile.feature.communication.CommunityViewModel
import com.project.solaria_mobile.feature.communication.data.CommunityApi
import com.project.solaria_mobile.feature.communication.data.CommunityConversationRequest
import com.project.solaria_mobile.feature.communication.data.CommunityDto
import com.project.solaria_mobile.feature.communication.data.CommunityMemberDto
import com.project.solaria_mobile.feature.communication.data.CommunityMembersRequest
import com.project.solaria_mobile.feature.communication.data.CommunityRequest
import com.project.solaria_mobile.feature.communication.data.CommunityStatusRequest
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.GroupCreationDto
import com.project.solaria_mobile.feature.communication.data.MemberRemovalRequest
import com.project.solaria_mobile.feature.communication.data.MessageStore
import com.project.solaria_mobile.feature.communication.data.MessageSync
import com.project.solaria_mobile.feature.communication.data.RejectedParticipantDto
import com.project.solaria_mobile.feature.communication.data.RemoteCommunityRepository
import com.project.solaria_mobile.feature.communication.data.SubgroupDto
import com.project.solaria_mobile.feature.communication.data.UserDirectory
import com.project.solaria_mobile.feature.communication.domain.Community
import com.project.solaria_mobile.feature.communication.domain.CommunityMember
import com.project.solaria_mobile.feature.communication.domain.CommunityRepository
import com.project.solaria_mobile.feature.communication.domain.CommunityRole
import com.project.solaria_mobile.feature.communication.domain.CommunityStatus
import com.project.solaria_mobile.feature.communication.domain.CommunitySubgroup
import com.project.solaria_mobile.feature.communication.domain.GroupCreation
import com.project.solaria_mobile.feature.communication.domain.Successor
import com.project.solaria_mobile.feature.communication.domain.canActivate
import com.project.solaria_mobile.feature.communication.domain.canAddMembers
import com.project.solaria_mobile.feature.communication.domain.canArchive
import com.project.solaria_mobile.feature.communication.domain.canCreateConversation
import com.project.solaria_mobile.feature.communication.domain.canGrantAdmin
import com.project.solaria_mobile.feature.communication.domain.canLeave
import com.project.solaria_mobile.feature.communication.domain.canRemoveMember
import com.project.solaria_mobile.feature.profile.data.UserSummaryDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

/** [CommunityApi] em memória. Registra as chamadas e falha com o erro programado, ou devolve a comunidade guardada. */
class ScriptedCommunityApi : CommunityApi {
    var community: CommunityDto = CommunityDto(
        id = "k1",
        projectId = "p1",
        members = listOf(CommunityMemberDto("user-1", "OWNER"), CommunityMemberDto("user-2", "MEMBER")),
    )
    var failure: Throwable? = null
    var removeCode = 204
    val calls = mutableListOf<String>()
    val removed = mutableListOf<String>()
    val removalBodies = mutableListOf<MemberRemovalRequest>()
    var creation = GroupCreationDto(
        conversation = ConversationDto(id = "g1", conversationType = "GROUP", title = "Equipe"),
        admittedParticipantIds = listOf("user-1", "user-2"),
        rejectedParticipants = listOf(RejectedParticipantDto("user-9", "PARTICIPANT_NOT_ADMITTED")),
    )

    private fun call(name: String): CommunityDto {
        calls += name
        failure?.let { throw it }
        return community
    }

    override suspend fun create(body: CommunityRequest) = call("create")
    override suspend fun mine() = listOf(call("mine"))
    override suspend fun community(id: String) = call("community")
    override suspend fun byProject(projectId: String) = call("byProject")
    override suspend fun setStatus(id: String, body: CommunityStatusRequest) = call("status:${body.status}").also { community = it.copy(status = body.status) }
    override suspend fun addMembers(id: String, body: CommunityMembersRequest) = call("add:${body.role}")

    override suspend fun removeMember(id: String, userId: String, body: MemberRemovalRequest): Response<Unit> {
        calls += "remove"
        failure?.let { throw it }
        removed += userId
        removalBodies += body
        return if (removeCode in 200..299) Response.success(Unit) else Response.error(removeCode, "{}".toResponseBody("application/json".toMediaType()))
    }

    override suspend fun createConversation(id: String, body: CommunityConversationRequest): GroupCreationDto {
        calls += "conversation:${body.title}"
        failure?.let { throw it }
        return creation
    }

    override suspend fun subgroups(id: String): List<SubgroupDto> {
        calls += "subgroups"
        failure?.let { throw it }
        return listOf(SubgroupDto("g1", "Equipe", "ACTIVE", listOf("user-1", "user-2"), "user-1"))
    }
}

/** Testes das regras de papel, do [RemoteCommunityRepository] e do [CommunityViewModel]. */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val conversationDao = InMemoryConversationDao()
    private val messageDao = InMemoryMessageDao(conversationDao)
    private val userDao = InMemoryUserDao()
    private val api = ScriptedMessengerApi()
    private val communityApi = ScriptedCommunityApi()
    private val profile = FakeProfileApi()
    private val store = MessageStore(messageDao, conversationDao)

    private fun TestScope.repository(): RemoteCommunityRepository {
        val executor = ApiCallExecutor(ApiErrorParser(ApiJson.instance))
        val credentials = object : CredentialProvider { override fun accessToken() = tokenFor("user-1") }
        val networkMonitor = object : NetworkMonitor { override val isOnline = MutableStateFlow(true) }
        val users = UserDirectory(profile, executor, userDao, conversationDao, messageDao)
        val sync = MessageSync(
            api = api,
            executor = executor,
            tokens = FakeTokenStore(),
            realtime = FakeRealtimeClient(),
            networkMonitor = networkMonitor,
            json = ApiJson.instance,
            store = store,
            conversations = conversationDao,
            messages = messageDao,
            syncState = InMemorySyncStateDao(),
            users = users,
            wiper = LocalDataWiper { },
            scope = backgroundScope,
        )
        return RemoteCommunityRepository(communityApi, executor, credentials, users, conversationDao, sync)
    }

    // regras de papel

    @Test fun rolesFollowTheServerRules() {
        val owner = CommunityRole.OWNER
        val admin = CommunityRole.ADMIN
        val member = CommunityRole.MEMBER

        assertTrue(canArchive(owner)); assertFalse(canArchive(admin)); assertFalse(canArchive(member)); assertFalse(canArchive(null))
        assertTrue(canActivate(owner)); assertTrue(canActivate(admin)); assertFalse(canActivate(member))
        assertTrue(canAddMembers(owner)); assertTrue(canAddMembers(admin)); assertFalse(canAddMembers(member))
        assertTrue(canGrantAdmin(owner)); assertFalse(canGrantAdmin(admin))
        assertTrue(canLeave(admin)); assertTrue(canLeave(member)); assertFalse(canLeave(owner)); assertFalse(canLeave(null))
    }

    @Test fun ownerIsNeverRemovedAndMembersCannotRemoveAnyone() {
        assertTrue(canRemoveMember(CommunityRole.OWNER, CommunityRole.MEMBER))
        assertTrue(canRemoveMember(CommunityRole.ADMIN, CommunityRole.MEMBER))
        assertFalse(canRemoveMember(CommunityRole.OWNER, CommunityRole.OWNER))
        assertFalse(canRemoveMember(CommunityRole.MEMBER, CommunityRole.MEMBER))
        assertFalse(canRemoveMember(null, CommunityRole.MEMBER))
    }

    @Test fun subgroupsOnlyInActiveCommunitiesAndOnlyForMembers() {
        assertTrue(canCreateConversation(CommunityRole.MEMBER, CommunityStatus.ACTIVE))
        assertFalse(canCreateConversation(CommunityRole.MEMBER, CommunityStatus.ARCHIVED))
        assertFalse(canCreateConversation(null, CommunityStatus.ACTIVE))
    }

    // repositório

    @Test fun projectWithoutCommunityIsNotAnError() = runTest {
        communityApi.failure = httpError(404)

        assertEquals(ApiResult.Success(null), repository().byProject("p1"))
    }

    @Test fun communityShowsRolesNamesAndMyRole() = runTest {
        profile.summaries["user-2"] = UserSummaryDto("Ana Bia", null)

        val community = (repository().byProject("p1") as ApiResult.Success).data!!

        assertEquals(CommunityRole.OWNER, community.myRole)
        assertEquals(CommunityStatus.ACTIVE, community.status)
        assertEquals(listOf("Usuário" to CommunityRole.OWNER, "Ana Bia" to CommunityRole.MEMBER), community.members.map { it.name to it.role })
    }

    @Test fun otherErrorsOfTheProjectLookupAreKept() = runTest {
        communityApi.failure = httpError(503)

        assertEquals(ApiResult.Failure(ErrorKind.SERVER), repository().byProject("p1"))
    }

    @Test fun removalRefusedByTheServerBecomesAFailureEvenThoughItIsATypedResponse() = runTest {
        communityApi.removeCode = 403

        assertEquals(ApiResult.Failure(ErrorKind.FORBIDDEN), repository().removeMember("k1", "user-2"))
    }

    @Test fun removalSendsTheSuccessorsAndLeavingUsesTheOwnId() = runTest {
        val repository = repository()

        assertEquals(ApiResult.Success(Unit), repository.removeMember("k1", "user-2", listOf(Successor("g1", "user-3"))))
        assertEquals(ApiResult.Success(Unit), repository.leave("k1"))

        assertEquals(listOf("user-2", "user-1"), communityApi.removed)
        assertEquals("user-3", communityApi.removalBodies.first().successors.single().successorUserId)
        assertTrue(communityApi.removalBodies.last().successors.isEmpty())
    }

    @Test fun newSubgroupEntersTheConversationListButNeverOverwritesOneAlreadyThere() = runTest {
        val repository = repository()

        val created = repository.createConversation("k1", "Equipe", listOf("user-2"))

        assertEquals(ApiResult.Success(GroupCreation("g1", listOf("user-9"))), created)
        assertNotNull(conversationDao.find("g1"))

        val real = conversationDao.find("g1")!!.copy(title = "Título real", syncVersion = 50)
        conversationDao.put(real)
        repository.createConversation("k1", "Equipe")

        assertEquals(real, conversationDao.find("g1"))
    }

    @Test fun addingAdminsSendsTheRoleAndAddingMembersOmitsIt() = runTest {
        val repository = repository()

        repository.addMembers("k1", listOf("user-3"))
        repository.addMembers("k1", listOf("user-3"), CommunityRole.ADMIN)

        assertEquals(listOf("add:null", "add:ADMIN"), communityApi.calls)
    }

    // ViewModel

    private class FakeCommunityRepository : CommunityRepository {
        var community: Community? = null
        var createResult: ApiResult<Community> = ApiResult.Failure(ErrorKind.UNKNOWN)
        var leaveResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var loads = 0

        override suspend fun mine(): ApiResult<List<Community>> = ApiResult.Success(listOfNotNull(community))
        override suspend fun byId(communityId: String): ApiResult<Community> = community?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ErrorKind.NOT_FOUND)
        override suspend fun byProject(projectId: String): ApiResult<Community?> {
            loads++
            return ApiResult.Success(community)
        }
        override suspend fun subgroups(communityId: String): ApiResult<List<CommunitySubgroup>> =
            ApiResult.Success(listOf(CommunitySubgroup("g1", "Equipe", 2, "user-1", true)))
        override suspend fun create(projectId: String, memberIds: List<String>): ApiResult<Community> = createResult
        override suspend fun setStatus(communityId: String, status: CommunityStatus): ApiResult<Community> = ApiResult.Failure(ErrorKind.FORBIDDEN)
        override suspend fun addMembers(communityId: String, userIds: List<String>, role: CommunityRole): ApiResult<Community> = ApiResult.Failure(ErrorKind.VALIDATION)
        override suspend fun removeMember(communityId: String, userId: String, successors: List<Successor>): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun leave(communityId: String): ApiResult<Unit> = leaveResult
        override suspend fun createConversation(communityId: String, title: String, participantIds: List<String>): ApiResult<GroupCreation> =
            ApiResult.Success(GroupCreation("g2", listOf("user-9")))
    }

    private fun communityOf(role: CommunityRole, status: CommunityStatus = CommunityStatus.ACTIVE) = Community(
        id = "k1",
        projectId = "p1",
        status = status,
        members = listOf(
            CommunityMember("user-1", "Eu", role),
            CommunityMember("user-2", "Ana", CommunityRole.MEMBER),
            CommunityMember("user-3", "Beto", CommunityRole.ADMIN),
            CommunityMember("user-4", "Dono", CommunityRole.OWNER),
        ),
        myRole = role,
    )

    private fun TestScope.viewModelOf(repository: CommunityRepository): Pair<CommunityViewModel, () -> CommunityUiState> {
        val viewModel = CommunityViewModel(SavedStateHandle(mapOf("id" to "p1")), repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel to { (viewModel.uiState.value as UiState.Content).data }
    }

    @Test fun projectWithoutCommunityOffersCreationAndNothingElse() = runTest {
        val (_, state) = viewModelOf(FakeCommunityRepository())

        assertNull(state().community)
        assertFalse(state().mayCreateConversation)
        assertFalse(state().mayArchive)
        assertTrue(state().removable.isEmpty())
    }

    @Test fun ownerCanArchiveAddAndRemoveEveryoneButHimself() = runTest {
        val repository = FakeCommunityRepository().apply { community = communityOf(CommunityRole.OWNER) }
        val (_, state) = viewModelOf(repository)

        assertTrue(state().mayArchive)
        assertFalse(state().mayActivate)
        assertTrue(state().mayAddMembers)
        assertTrue(state().mayGrantAdmin)
        assertFalse(state().mayLeave)
        assertTrue(state().mayCreateConversation)
        // O dono (eu) e o outro dono não saem da lista de remoção; administrador e membro comum sim
        assertEquals(setOf("user-2", "user-3"), state().removable)
        assertEquals(listOf("Equipe"), state().subgroups.map { it.title })
    }

    @Test fun plainMemberCanOnlyLeaveAndCreateSubgroups() = runTest {
        val repository = FakeCommunityRepository().apply { community = communityOf(CommunityRole.MEMBER) }
        val (_, state) = viewModelOf(repository)

        assertFalse(state().mayArchive)
        assertFalse(state().mayAddMembers)
        assertTrue(state().mayLeave)
        assertTrue(state().mayCreateConversation)
        assertTrue(state().removable.isEmpty())
    }

    @Test fun archivedCommunityOffersActivationInsteadOfEditing() = runTest {
        val repository = FakeCommunityRepository().apply { community = communityOf(CommunityRole.ADMIN, CommunityStatus.ARCHIVED) }
        val (_, state) = viewModelOf(repository)

        assertTrue(state().mayActivate)
        assertFalse(state().mayArchive)
        assertFalse(state().mayAddMembers)
        assertFalse(state().mayCreateConversation)
        assertTrue(state().removable.isEmpty())
    }

    @Test fun serverRefusalsBecomeNoticesAndCanBeDismissed() = runTest {
        val repository = FakeCommunityRepository().apply { community = communityOf(CommunityRole.OWNER) }
        val (viewModel, state) = viewModelOf(repository)

        viewModel.archive()
        advanceUntilIdle()
        assertEquals(CommunityNotice.NOT_ALLOWED, state().notice)

        viewModel.addMembers(listOf("user-5"))
        advanceUntilIdle()
        assertEquals(CommunityNotice.REFUSED, state().notice)

        viewModel.dismissNotice()
        assertNull(state().notice)
    }

    @Test fun creatingTheCommunityTwiceShowsAConflictAndSuccessReloads() = runTest {
        val repository = FakeCommunityRepository().apply { createResult = ApiResult.Failure(ErrorKind.CONFLICT) }
        val (viewModel, state) = viewModelOf(repository)

        viewModel.create()
        advanceUntilIdle()
        assertEquals(CommunityNotice.CONFLICT, state().notice)

        repository.createResult = ApiResult.Success(communityOf(CommunityRole.OWNER))
        repository.community = communityOf(CommunityRole.OWNER)
        viewModel.create()
        advanceUntilIdle()
        assertNotNull(state().community)
    }

    @Test fun newSubgroupReportsWhoWasNotAdmitted() = runTest {
        val repository = FakeCommunityRepository().apply { community = communityOf(CommunityRole.OWNER) }
        val (viewModel, state) = viewModelOf(repository)

        viewModel.createConversation("Obra")
        advanceUntilIdle()

        assertEquals("g2", state().lastCreation?.conversationId)
        assertEquals(listOf("user-9"), state().lastCreation?.rejected)
    }

    @Test fun leavingEmitsAnEventAndFailuresShowANotice() = runTest {
        val repository = FakeCommunityRepository().apply { community = communityOf(CommunityRole.MEMBER) }
        val (viewModel, state) = viewModelOf(repository)
        val events = mutableListOf<Unit>()
        backgroundScope.launch { viewModel.leftCommunity.collect { events += it } }

        repository.leaveResult = ApiResult.Failure(ErrorKind.CONNECTIVITY)
        viewModel.leave()
        advanceUntilIdle()
        assertTrue(events.isEmpty())
        assertEquals(CommunityNotice.OFFLINE, state().notice)

        repository.leaveResult = ApiResult.Success(Unit)
        viewModel.leave()
        advanceUntilIdle()
        runCurrent()
        assertEquals(1, events.size)
    }
}
