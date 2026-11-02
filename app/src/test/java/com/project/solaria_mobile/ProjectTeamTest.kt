package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.project.ProjectAction
import com.project.solaria_mobile.feature.project.ProjectListViewModel
import com.project.solaria_mobile.feature.project.ProjectOperation
import com.project.solaria_mobile.feature.project.ProjectSectionsViewModel
import com.project.solaria_mobile.feature.project.ProjectSheet
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.ProjectInvitation
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectRoleOption
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import com.project.solaria_mobile.feature.project.message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectTeamTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class TeamRepository(private val inviteFailure: ErrorKind? = null) : ProjectDetailRepository {
        val members = mutableListOf(ProjectMember("m1", "u1", "Ana Lima", "Gerente", "AL", owner = true), ProjectMember("m2", "u2", "Bruno Reis", "Membro", "BR"))
        val invited = mutableListOf<String>()
        var memberLoads = 0
        override suspend fun location(projectId: String): ApiResult<ProjectLocationInfo> = ApiResult.Success(ProjectLocationInfo("", emptyList()))
        override suspend fun members(projectId: String): ApiResult<List<ProjectMember>> {
            memberLoads++
            return ApiResult.Success(members.toList())
        }
        override suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>> = ApiResult.Success(emptyList())
        override suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>> = ApiResult.Success(emptyList())
        override suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>> = ApiResult.Success(emptyList())
        override suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1))
        override suspend fun invitations(projectId: String): ApiResult<List<ProjectInvitation>> =
            ApiResult.Success(invited.map { ProjectInvitation("i-$it", projectId, "Projeto", it, "Membro", true, "") })
        override suspend fun invite(projectId: String, email: String, role: ProjectRoleOption, functionName: String): ApiResult<Unit> {
            if (inviteFailure != null) return ApiResult.Failure(inviteFailure)
            invited += email
            return ApiResult.Success(Unit)
        }
        override suspend fun removeMember(projectId: String, userId: String): ApiResult<Unit> {
            members.removeAll { it.userId == userId }
            return ApiResult.Success(Unit)
        }
    }

    private fun sections(repository: ProjectDetailRepository) = ProjectSectionsViewModel(SavedStateHandle(mapOf("id" to "project-1")), repository)

    @Test fun confirmedInviteAppearsAsPendingAndReloadsTheTeam() = runTest {
        val repository = TeamRepository()
        val model = sections(repository)
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()
        val loadsBefore = repository.memberLoads

        model.invite("nova@empresa.com", ProjectRoleOption.MEMBER, "")
        advanceUntilIdle()

        assertEquals(ProjectAction.Done(ProjectOperation.INVITE), model.teamAction.value)
        assertTrue(repository.memberLoads > loadsBefore)
        assertEquals(listOf("nova@empresa.com"), model.invitations.value.map { it.email })
    }

    @Test fun refusedInviteExplainsTheLikelyCause() = runTest {
        val model = sections(TeamRepository(inviteFailure = ErrorKind.VALIDATION))
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()

        model.invite("ja@participa.com", ProjectRoleOption.MEMBER, "")
        advanceUntilIdle()

        val failed = model.teamAction.value as ProjectAction.Failed
        assertEquals("Confira o e-mail. A pessoa pode já participar do projeto ou ter um convite pendente.", failed.message(TestStrings.resources))
    }

    @Test fun removedMemberLeavesTheTeam() = runTest {
        val model = sections(TeamRepository())
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()

        model.removeMember("u2")
        advanceUntilIdle()

        assertEquals(listOf("u1"), (model.members.value as UiState.Content).data.map { it.userId })
    }

    private class InvitedRepository(private val failure: ErrorKind? = null) : ProjectRepository by FakeProjectRepository() {
        var projectLoads = 0
        val answers = mutableListOf<Pair<String, Boolean>>()
        override suspend fun projects(): ApiResult<List<ProjectSummary>> {
            projectLoads++
            return ApiResult.Success(emptyList())
        }
        // Como no servidor, convite já respondido deixa de aparecer entre os pendentes.
        override suspend fun myInvitations(): ApiResult<List<ProjectInvitation>> =
            ApiResult.Success(listOf(ProjectInvitation("i1", "p9", "Usina Sul", "eu@empresa.com", "Membro", true, "")).filterNot { pending -> answers.any { it.first == pending.id } })
        override suspend fun respondInvitation(id: String, accept: Boolean): ApiResult<Unit> {
            if (failure != null) return ApiResult.Failure(failure)
            answers += id to accept
            return ApiResult.Success(Unit)
        }
    }

    @Test fun acceptingAnInvitationRemovesItAndReloadsTheProjects() = runTest {
        val repository = InvitedRepository()
        val model = ProjectListViewModel(repository)
        advanceUntilIdle()
        assertEquals(listOf("i1"), model.invitations.value.map { it.id })

        model.respond("i1", accept = true)
        advanceUntilIdle()

        assertEquals(listOf("i1" to true), repository.answers)
        assertTrue(model.invitations.value.isEmpty())
        assertEquals(null, model.invitationAction.value)
    }

    @Test fun expiredInvitationKeepsTheCardAndExplains() = runTest {
        val model = ProjectListViewModel(InvitedRepository(failure = ErrorKind.VALIDATION))
        advanceUntilIdle()

        model.respond("i1", accept = true)
        advanceUntilIdle()

        assertEquals(listOf("i1"), model.invitations.value.map { it.id })
        assertEquals("O convite expirou ou já foi respondido.", (model.invitationAction.value as ProjectAction.Failed).message(TestStrings.resources))
    }
}
