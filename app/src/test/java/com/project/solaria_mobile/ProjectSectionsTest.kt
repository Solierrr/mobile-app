package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.project.ProjectSectionsViewModel
import com.project.solaria_mobile.feature.project.ProjectSheet
import com.project.solaria_mobile.feature.project.data.ProjectActivityDto
import com.project.solaria_mobile.feature.project.data.ProjectDto
import com.project.solaria_mobile.feature.project.data.ProjectHistoryDto
import com.project.solaria_mobile.feature.project.data.ProjectLocationDto
import com.project.solaria_mobile.feature.project.data.ProjectMemberDto
import com.project.solaria_mobile.feature.project.data.toActivity
import com.project.solaria_mobile.feature.project.data.toEntry
import com.project.solaria_mobile.feature.project.data.toLocationInfo
import com.project.solaria_mobile.feature.project.data.toMember
import com.project.solaria_mobile.feature.project.data.translateStatuses
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProjectSectionsTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class CountingRepository : ProjectDetailRepository {
        var memberCalls = 0
        var failMembers = false
        override suspend fun location(projectId: String): ApiResult<ProjectLocationInfo> = ApiResult.Success(ProjectLocationInfo("", listOf("Rua A, 10")))
        override suspend fun members(projectId: String): ApiResult<List<ProjectMember>> {
            memberCalls++
            return if (failMembers) ApiResult.Failure(ErrorKind.SERVER) else ApiResult.Success(listOf(ProjectMember("1", "u1", "Ana Lima", "Gerente", "AL")))
        }
        override suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>> = ApiResult.Success(emptyList())
        override suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>> = ApiResult.Success(emptyList())
        override suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>> = ApiResult.Success(listOf(ProjectPhoto("p1", true)))
        override suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1))
    }

    private fun viewModel(repository: ProjectDetailRepository) =
        ProjectSectionsViewModel(SavedStateHandle(mapOf("id" to "project-1")), repository)

    @Test fun photosLoadWithTheScreenAndOtherSectionsWaitForTheirSheet() = runTest {
        val repository = CountingRepository()
        val model = viewModel(repository)
        advanceUntilIdle()
        assertTrue(model.photos.value is UiState.Content)
        assertEquals(0, repository.memberCalls)
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()
        assertEquals(1, repository.memberCalls)
        assertTrue(model.members.value is UiState.Content)
    }

    @Test fun openingASheetTwiceLoadsItOnlyOnce() = runTest {
        val repository = CountingRepository()
        val model = viewModel(repository)
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()
        assertEquals(1, repository.memberCalls)
    }

    @Test fun retryReloadsAFailedSection() = runTest {
        val repository = CountingRepository().apply { failMembers = true }
        val model = viewModel(repository)
        model.open(ProjectSheet.TEAM)
        advanceUntilIdle()
        assertEquals(UiState.Error(ErrorKind.SERVER), model.members.value)
        repository.failMembers = false
        model.retry(ProjectSheet.TEAM)
        advanceUntilIdle()
        assertTrue(model.members.value is UiState.Content)
        assertEquals(2, repository.memberCalls)
    }

    @Test fun locationJoinsStreetAreaAndZipSkippingBlankParts() {
        val project = ProjectDto(
            id = "1", name = "Obra", description = " Instalação no telhado ",
            location = ProjectLocationDto(city = "Goiânia", state = "GO", neighborhood = "Centro", zipCode = "74000000", street = "Rua 5", number = "20", complement = " "),
        )
        val info = project.toLocationInfo()
        assertEquals("Instalação no telhado", info.description)
        assertEquals(listOf("Rua 5, 20", "Centro, Goiânia, GO", "CEP 74000000"), info.addressLines)
        assertEquals("Rua 5, 20, Centro, Goiânia, GO, CEP 74000000", info.copyText)
    }

    @Test fun locationWithoutAddressHasNoLines() {
        assertTrue(ProjectDto(id = "1", name = "Obra").toLocationInfo().addressLines.isEmpty())
    }

    @Test fun memberUsesTheFunctionNameBeforeTheRoleLabel() {
        assertEquals("Eletricista", ProjectMemberDto("1", "u1", "Ana Lima", "MEMBER", "Eletricista").toMember().role)
        assertEquals("Responsável pelo projeto", ProjectMemberDto("1", "u1", "Ana Lima", "OWNER").toMember().role)
        assertEquals("AL", ProjectMemberDto("1", "u1", "Ana Lima", "OWNER").toMember().initials)
        assertEquals("Participante", ProjectMemberDto("1", "u1", null, "OWNER").toMember().name)
    }

    @Test fun activityShowsKindStatusAndTime() {
        val activity = ProjectActivityDto(
            id = "1", title = "Vistoria", type = "VISIT", status = "OPEN", startsAt = "2026-10-20T12:30:00Z", onsite = true,
        ).toActivity()
        assertEquals("Vistoria", activity.title)
        assertTrue(activity.subtitle.contains("Visita"))
        assertTrue(activity.subtitle.contains("No local"))
        assertTrue(activity.subtitle.contains("Aberto"))
        assertTrue(activity.time.matches(Regex("\\d{2}:\\d{2}")))
    }

    @Test fun historyEntryUsesTheDetailAsTitleAndTheActorInTheSubtitle() {
        val entry = ProjectHistoryDto("1", "CREATED", "Projeto criado", "amanda_lacerda", "2026-10-18T10:00:00Z").toEntry()
        assertEquals("Projeto criado", entry.title)
        assertTrue(entry.detail.startsWith("amanda_lacerda · "))
        assertEquals("CREATED", ProjectHistoryDto("2", "CREATED", null, null, null).toEntry().title)
    }

    @Test fun historyTranslatesTheStatusCodesTheServerWrites() {
        assertEquals("Limpeza → Concluído", translateStatuses("Limpeza → COMPLETED"))
        assertEquals("Aberto → Em andamento", translateStatuses("OPEN → IN_PROGRESS"))
        assertEquals("Projeto criado", translateStatuses("Projeto criado"))
    }
}
