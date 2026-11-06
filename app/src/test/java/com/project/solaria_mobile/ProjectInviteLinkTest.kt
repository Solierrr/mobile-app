package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.project.ProjectAction
import com.project.solaria_mobile.feature.project.ProjectOperation
import com.project.solaria_mobile.feature.project.ProjectSectionsViewModel
import com.project.solaria_mobile.feature.project.ProjectSheet
import com.project.solaria_mobile.feature.project.domain.INVITE_LINK_ROLES
import com.project.solaria_mobile.feature.project.domain.InviteLinkValidity
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.ProjectInviteLink
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import com.project.solaria_mobile.feature.project.domain.ProjectRoleOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectInviteLinkTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class LinkRepository(private val createFailure: ErrorKind? = null) : ProjectDetailRepository {
        val links = mutableListOf<ProjectInviteLink>()
        val created = mutableListOf<Pair<ProjectRoleOption, InviteLinkValidity>>()
        override suspend fun location(projectId: String): ApiResult<ProjectLocationInfo> = ApiResult.Success(ProjectLocationInfo("", emptyList()))
        override suspend fun members(projectId: String): ApiResult<List<ProjectMember>> = ApiResult.Success(emptyList())
        override suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>> = ApiResult.Success(emptyList())
        override suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>> = ApiResult.Success(emptyList())
        override suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>> = ApiResult.Success(emptyList())
        override suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1))
        // Como no servidor, a listagem nunca traz o endereço do link.
        override suspend fun inviteLinks(projectId: String): ApiResult<List<ProjectInviteLink>> = ApiResult.Success(links.map { it.copy(url = null) })
        override suspend fun createInviteLink(projectId: String, role: ProjectRoleOption, validity: InviteLinkValidity): ApiResult<ProjectInviteLink> {
            if (createFailure != null) return ApiResult.Failure(createFailure)
            created += role to validity
            val link = ProjectInviteLink("l${links.size + 1}", role.label, true, "Válido até 07/11/2026", 0, "https://example.invalid/convite/token-${links.size + 1}")
            links += link
            return ApiResult.Success(link)
        }
        override suspend fun revokeInviteLink(projectId: String, linkId: String): ApiResult<Unit> {
            links.removeAll { it.id == linkId }
            return ApiResult.Success(Unit)
        }
    }

    private fun sections(repository: ProjectDetailRepository) = ProjectSectionsViewModel(SavedStateHandle(mapOf("id" to "project-1")), repository)

    @Test fun createdLinkShowsItsAddressOnceAndIsListedWithoutIt() = runTest {
        val repository = LinkRepository()
        val model = sections(repository)
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()

        model.createInviteLink(ProjectRoleOption.VIEWER, InviteLinkValidity.MONTH)
        advanceUntilIdle()

        assertEquals(ProjectAction.Done(ProjectOperation.CREATE_LINK), model.teamAction.value)
        assertEquals(listOf(ProjectRoleOption.VIEWER to InviteLinkValidity.MONTH), repository.created)
        assertEquals("https://example.invalid/convite/token-1", model.createdLink.value?.url)
        assertEquals(listOf(null), model.inviteLinks.value.map { it.url })

        model.clearCreatedLink()
        assertNull(model.createdLink.value)
        assertEquals(1, model.inviteLinks.value.size)
    }

    @Test fun revokedLinkLeavesTheActiveList() = runTest {
        val repository = LinkRepository()
        val model = sections(repository)
        model.open(ProjectSheet.TEAM)
        model.createInviteLink(ProjectRoleOption.MEMBER, InviteLinkValidity.WEEK)
        advanceUntilIdle()

        model.revokeInviteLink("l1")
        advanceUntilIdle()

        assertEquals(ProjectAction.Done(ProjectOperation.REVOKE_LINK), model.teamAction.value)
        assertTrue(model.inviteLinks.value.isEmpty())
        assertNull(model.createdLink.value)
    }

    @Test fun refusedCreationKeepsNoLinkAndReportsTheOperation() = runTest {
        val model = sections(LinkRepository(createFailure = ErrorKind.FORBIDDEN))
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()

        model.createInviteLink(ProjectRoleOption.MEMBER, InviteLinkValidity.WEEK)
        advanceUntilIdle()

        assertEquals(ProjectAction.Failed(ErrorKind.FORBIDDEN, ProjectOperation.CREATE_LINK), model.teamAction.value)
        assertNull(model.createdLink.value)
    }

    @Test fun managerIsNeverOfferedForALink() {
        assertFalse(ProjectRoleOption.MANAGER in INVITE_LINK_ROLES)
        assertNotNull(INVITE_LINK_ROLES.firstOrNull { it == ProjectRoleOption.MEMBER })
        assertEquals(InviteLinkValidity.WEEK.days, 7)
    }
}
