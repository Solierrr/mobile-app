package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ProjectCommunityViewModel
import com.project.solaria_mobile.feature.communication.domain.CommunityPerson
import com.project.solaria_mobile.feature.communication.domain.GroupCreated
import com.project.solaria_mobile.feature.communication.domain.canChangeAdmin
import com.project.solaria_mobile.feature.communication.domain.canRemove
import com.project.solaria_mobile.feature.communication.domain.groupsNeedingSuccessor
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunity
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunityRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectCommunityTest {
    @get:Rule val main = MainDispatcherRule()

    private class FakeCommunity : ProjectCommunityRepository {
        var snapshot = ProjectCommunity(projectId = "p1", canCreate = true)
        var failure: ErrorKind? = null
        var gate: CompletableDeferred<Unit>? = null
        var createCalls = 0
        val groups = mutableListOf<String>()
        val invited = mutableListOf<Set<String>>()
        val added = mutableListOf<String>()
        override suspend fun read(projectId: String) = ReadResult(failure?.let { ApiResult.Failure(it) } ?: ApiResult.Success(snapshot))
        override suspend fun create(projectId: String): ApiResult<Unit> {
            createCalls++
            gate?.await()
            snapshot = snapshot.copy(id = "community", canCreate = false, members = 2)
            return ApiResult.Success(Unit)
        }
        override suspend fun createGroup(communityId: String, title: String, participants: Set<String>): ApiResult<GroupCreated> {
            groups += title
            invited += participants
            gate?.await()
            return ApiResult.Success(GroupCreated("group", 1))
        }
        override suspend fun addMember(communityId: String, authId: String): ApiResult<Unit> {
            added += authId
            return ApiResult.Success(Unit)
        }
        val archivedCalls = mutableListOf<Boolean>()
        val adminCalls = mutableListOf<Pair<String, Boolean>>()
        val removals = mutableListOf<Pair<String, Map<String, String>>>()
        override suspend fun setArchived(communityId: String, archived: Boolean): ApiResult<Unit> { archivedCalls += archived; return ApiResult.Success(Unit) }
        override suspend fun setAdmin(communityId: String, authId: String, admin: Boolean): ApiResult<Unit> { adminCalls += authId to admin; return ApiResult.Success(Unit) }
        override suspend fun removeMember(communityId: String, authId: String, successors: Map<String, String>): ApiResult<Unit> { removals += authId to successors; return ApiResult.Success(Unit) }
        override suspend fun mine(): ApiResult<List<com.project.solaria_mobile.feature.communication.domain.CommunitySummary>> = ApiResult.Success(emptyList())
    }

    private fun model(repository: FakeCommunity) = ProjectCommunityViewModel(SavedStateHandle(mapOf("id" to "p1")), repository)

    @Test fun openingAndRefreshingNeverCreateACommunity() = runTest {
        val repository = FakeCommunity()
        val model = model(repository)
        advanceUntilIdle()
        model.load()
        advanceUntilIdle()
        assertEquals(0, repository.createCalls)
        assertTrue((model.uiState.value as UiState.Content).data.community.canCreate)
    }

    @Test fun creationBlocksDoubleTapAndRefreshDuringMutation() = runTest {
        val repository = FakeCommunity()
        val model = model(repository)
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        model.create()
        runCurrent()
        model.create()
        model.load()
        assertTrue((model.uiState.value as UiState.Content).data.busy)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.createCalls)
        assertFalse((model.uiState.value as UiState.Content).data.busy)
        assertEquals("community", (model.uiState.value as UiState.Content).data.community.id)
    }

    @Test fun unauthorizedCreationNeverCallsTheApi() = runTest {
        val repository = FakeCommunity().apply { snapshot = snapshot.copy(canCreate = false) }
        val model = model(repository)
        advanceUntilIdle()
        model.create()
        advanceUntilIdle()
        assertEquals(0, repository.createCalls)
        assertEquals(ErrorKind.FORBIDDEN, (model.uiState.value as UiState.Content).data.error)
    }

    @Test fun invalidTitlesAreRejectedAndValidGroupIsTrimmedWithPartialAdmissionNotice() = runTest {
        val repository = FakeCommunity().apply { snapshot = snapshot.copy(id = "community", members = 2) }
        val model = model(repository)
        advanceUntilIdle()
        model.createGroup(" ")
        advanceUntilIdle()
        assertEquals(ErrorKind.VALIDATION, (model.uiState.value as UiState.Content).data.error)
        assertTrue(repository.groups.isEmpty())
        model.createGroup("  Instalação  ")
        advanceUntilIdle()
        assertEquals(listOf("Instalação"), repository.groups)
        assertEquals(1, (model.uiState.value as UiState.Content).data.notice?.rejected)
    }

    @Test fun onlyKnownUnjoinedPeopleCanBeAddedByAnAdministrator() = runTest {
        val repository = FakeCommunity().apply {
            snapshot = snapshot.copy(id = "community", members = 1, canManage = true, people = listOf(CommunityPerson("auth-ana", "Ana", false)))
        }
        val model = model(repository)
        advanceUntilIdle()
        model.addMember("arbitrary-auth")
        advanceUntilIdle()
        assertTrue(repository.added.isEmpty())
        model.addMember("auth-ana")
        advanceUntilIdle()
        assertEquals(listOf("auth-ana"), repository.added)
    }

    @Test fun networkFailureKeepsContentButForbiddenResponseRemovesIt() = runTest {
        val repository = FakeCommunity()
        val model = model(repository)
        advanceUntilIdle()
        repository.failure = ErrorKind.CONNECTIVITY
        model.load()
        advanceUntilIdle()
        assertEquals(ErrorKind.CONNECTIVITY, (model.uiState.value as UiState.Content).refreshError)
        repository.failure = ErrorKind.FORBIDDEN
        model.load()
        advanceUntilIdle()
        assertEquals(UiState.Error(ErrorKind.FORBIDDEN), model.uiState.value)
    }

    @Test fun restrictedGroupAcceptsOnlySelectedKnownCommunityMembers() = runTest {
        val repository = FakeCommunity().apply {
            snapshot = snapshot.copy(id = "community", members = 3, people = listOf(
                CommunityPerson("me", "Eu", true, self = true),
                CommunityPerson("ana", "Ana", true),
                CommunityPerson("outside", "Outro integrante", false),
            ))
        }
        val model = model(repository)
        advanceUntilIdle()
        model.createGroup("Equipe", setOf("outside"))
        advanceUntilIdle()
        assertTrue(repository.groups.isEmpty())
        model.createGroup("Equipe", emptySet())
        advanceUntilIdle()
        assertTrue(repository.groups.isEmpty())
        model.createGroup("Equipe", setOf("ana"))
        advanceUntilIdle()
        assertEquals(listOf(setOf("ana")), repository.invited)
    }

    private fun managed(myRole: String) = ProjectCommunity(
        id = "community", projectId = "p1", members = 4, canManage = myRole != "MEMBER", myRole = myRole,
        groups = listOf(
            com.project.solaria_mobile.feature.communication.domain.CommunityGroup("g1", "Geral", 3, true, true, managerId = "ana", participantIds = listOf("ana", "bia", "eu")),
            com.project.solaria_mobile.feature.communication.domain.CommunityGroup("g2", "Solo", 1, true, true, managerId = "ana", participantIds = listOf("ana")),
        ),
        people = listOf(
            CommunityPerson("eu", "Eu", true, self = true, role = myRole),
            CommunityPerson("ana", "Ana", true, role = "ADMIN"),
            CommunityPerson("bia", "Bia", true, role = "MEMBER"),
            CommunityPerson("dono", "Dono", true, role = "OWNER"),
        ),
    )

    @Test fun ownerRemovesAndChangesRolesButNeverTouchesAnotherOwner() {
        val owner = managed("OWNER")
        val person = { id: String -> owner.people.first { it.authId == id } }
        assertTrue(owner.canRemove(person("ana")) && owner.canRemove(person("bia")))
        assertFalse(owner.canRemove(person("dono")))
        assertTrue(owner.canChangeAdmin(person("ana")) && owner.canChangeAdmin(person("bia")))
        assertFalse(owner.canChangeAdmin(person("dono")) || owner.canChangeAdmin(person("eu")))
    }

    @Test fun administratorOnlyRemovesPlainMembersAndCannotDemoteOtherAdministrators() {
        val admin = managed("ADMIN")
        val person = { id: String -> admin.people.first { it.authId == id } }
        assertTrue(admin.canRemove(person("bia")))
        assertFalse(admin.canRemove(person("ana")))
        assertTrue(admin.canChangeAdmin(person("bia")))
        assertFalse(admin.canChangeAdmin(person("ana")))
    }

    @Test fun aPlainMemberCanOnlyLeaveByThemselves() {
        val member = managed("MEMBER")
        assertTrue(member.canRemove(member.people.first { it.self }))
        assertFalse(member.canRemove(member.people.first { it.authId == "bia" }))
        assertFalse(member.canChangeAdmin(member.people.first { it.authId == "bia" }))
    }

    @Test fun onlyGroupsLeftWithPeopleNeedASuccessor() {
        assertEquals(listOf("g1"), managed("OWNER").groupsNeedingSuccessor("ANA").map { it.id })
        assertTrue(managed("OWNER").groupsNeedingSuccessor("bia").isEmpty())
    }

    @Test fun removingWithoutTheSuccessorOfAManagedGroupNeverReachesTheServer() = runTest {
        val repository = FakeCommunity().apply { snapshot = managed("OWNER") }
        val model = model(repository)
        advanceUntilIdle()
        model.removeMember("ana")
        advanceUntilIdle()
        assertEquals(ErrorKind.VALIDATION, (model.uiState.value as UiState.Content).data.error)
        assertTrue(repository.removals.isEmpty())
        model.removeMember("ana", mapOf("g1" to "bia"))
        advanceUntilIdle()
        assertEquals(listOf("ana" to mapOf("g1" to "bia")), repository.removals)
    }

    @Test fun archivingAndRolesAreCheckedBeforeCallingTheServer() = runTest {
        val repository = FakeCommunity().apply { snapshot = managed("ADMIN") }
        val model = model(repository)
        advanceUntilIdle()
        model.setAdmin("ana", false)
        advanceUntilIdle()
        assertEquals(ErrorKind.FORBIDDEN, (model.uiState.value as UiState.Content).data.error)
        model.setAdmin("bia", true)
        advanceUntilIdle()
        model.setArchived(true)
        advanceUntilIdle()
        assertEquals(listOf("bia" to true), repository.adminCalls)
        assertEquals(listOf(true), repository.archivedCalls)
    }

    @Test fun anArchivedCommunityAcceptsNoRoleChanges() = runTest {
        val repository = FakeCommunity().apply { snapshot = managed("OWNER").copy(archived = true) }
        val model = model(repository)
        advanceUntilIdle()
        model.setAdmin("bia", true)
        advanceUntilIdle()
        assertTrue(repository.adminCalls.isEmpty())
        model.setArchived(false)
        advanceUntilIdle()
        assertEquals(listOf(false), repository.archivedCalls)
    }
}
