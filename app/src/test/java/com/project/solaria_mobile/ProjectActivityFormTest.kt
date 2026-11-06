package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.project.ProjectAction
import com.project.solaria_mobile.feature.project.ProjectOperation
import com.project.solaria_mobile.feature.project.ProjectSectionsViewModel
import com.project.solaria_mobile.feature.project.ProjectSheet
import com.project.solaria_mobile.feature.project.data.toRequest
import com.project.solaria_mobile.feature.project.domain.ActivityDraft
import com.project.solaria_mobile.feature.project.domain.ActivityField
import com.project.solaria_mobile.feature.project.domain.ActivityTypeOption
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectActivityFormTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class AgendaRepository : ProjectDetailRepository {
        val saved = mutableListOf<Pair<String?, ActivityDraft>>()
        var activityLoads = 0
        override suspend fun location(projectId: String): ApiResult<ProjectLocationInfo> = ApiResult.Success(ProjectLocationInfo("", emptyList()))
        override suspend fun members(projectId: String): ApiResult<List<ProjectMember>> = ApiResult.Success(emptyList())
        override suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>> {
            activityLoads++
            return ApiResult.Success(emptyList())
        }
        override suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>> = ApiResult.Success(emptyList())
        override suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>> = ApiResult.Success(emptyList())
        override suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1))
        override suspend fun saveActivity(projectId: String, activityId: String?, draft: ActivityDraft): ApiResult<ProjectActivity> {
            saved += activityId to draft
            return ApiResult.Success(ProjectActivity(activityId ?: "nova", draft.title, "", ""))
        }
    }

    private fun sections(repository: ProjectDetailRepository) = ProjectSectionsViewModel(SavedStateHandle(mapOf("id" to "project-1")), repository)

    @Test fun draftFollowsTheServerRules() {
        assertEquals(setOf(ActivityField.TITLE), ActivityDraft(title = " ").validate())
        assertEquals(setOf(ActivityField.PERIOD), ActivityDraft(title = "Visita", start = LocalTime.of(10, 0), end = LocalTime.of(10, 0)).validate())
        assertTrue(ActivityDraft(title = "Visita", start = LocalTime.of(9, 0), end = LocalTime.of(10, 0)).validate().isEmpty())
    }

    @Test fun invalidDraftIsRefusedWithoutCallingTheServer() = runTest {
        val repository = AgendaRepository()
        val model = sections(repository)
        advanceUntilIdle()

        model.saveActivity(null, ActivityDraft(title = ""))
        advanceUntilIdle()

        assertTrue(repository.saved.isEmpty())
        assertEquals(ProjectOperation.SAVE_ACTIVITY, (model.activityAction.value as ProjectAction.Failed).operation)
    }

    @Test fun createdActivityReloadsTheAgenda() = runTest {
        val repository = AgendaRepository()
        val model = sections(repository)
        model.open(ProjectSheet.AGENDA)
        advanceUntilIdle()
        val loadsBefore = repository.activityLoads

        model.saveActivity(null, ActivityDraft(title = "Vistoria"))
        advanceUntilIdle()

        assertEquals(listOf<String?>(null), repository.saved.map { it.first })
        assertTrue(repository.activityLoads > loadsBefore)
        assertEquals(ProjectAction.Done(ProjectOperation.SAVE_ACTIVITY), model.activityAction.value)
    }

    @Test fun editingSendsTheSameActivity() = runTest {
        val repository = AgendaRepository()
        val model = sections(repository)
        advanceUntilIdle()

        model.saveActivity("a1", ActivityDraft(title = "Vistoria ajustada"))
        advanceUntilIdle()

        assertEquals(listOf<String?>("a1"), repository.saved.map { it.first })
    }

    @Test fun localDateAndTimesTravelWithTheDeviceZone() {
        val draft = ActivityDraft(title = "Reunião", type = ActivityTypeOption.MEETING, date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(14, 30), end = LocalTime.of(15, 0), assigneeIds = listOf("u1", "u1", "u2"))

        val request = draft.toRequest()

        val zone = ZoneId.systemDefault()
        assertEquals(LocalDate.of(2026, 11, 3).atTime(14, 30), OffsetDateTime.parse(request.startsAt).atZoneSameInstant(zone).toLocalDateTime())
        assertEquals("MEETING", request.type)
        assertEquals(listOf("u1", "u2"), request.assigneeIds)
    }

    @Test fun existingActivityOpensWithItsLocalTimes() {
        val zone = ZoneId.systemDefault()
        val starts = LocalDate.of(2026, 11, 5).atTime(8, 15).atZone(zone).toOffsetDateTime()
        val activity = ProjectActivity("a1", "Instalação", "", "", type = ActivityTypeOption.SERVICE, startsAt = starts, endsAt = starts.plusHours(2), assigneeIds = listOf("u3"))

        val draft = ActivityDraft.from(activity)

        assertEquals(LocalDate.of(2026, 11, 5), draft.date)
        assertEquals(LocalTime.of(8, 15), draft.start)
        assertEquals(LocalTime.of(10, 15), draft.end)
        assertEquals(ActivityTypeOption.SERVICE, draft.type)
        assertEquals(listOf("u3"), draft.assigneeIds)
    }
}
