package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ProjectCommunityViewModel
import com.project.solaria_mobile.feature.communication.domain.CommunityPerson
import com.project.solaria_mobile.feature.communication.domain.GroupCreated
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
}
