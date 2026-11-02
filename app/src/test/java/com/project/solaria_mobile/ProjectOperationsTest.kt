package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.project.ProjectAction
import com.project.solaria_mobile.feature.project.ProjectDetailViewModel
import com.project.solaria_mobile.feature.project.ProjectOperation
import com.project.solaria_mobile.feature.project.ProjectSectionsViewModel
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import com.project.solaria_mobile.feature.project.domain.ProjectHistoryEntry
import com.project.solaria_mobile.feature.project.domain.ProjectLocationInfo
import com.project.solaria_mobile.feature.project.domain.ProjectMember
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectStatusOption
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
class ProjectOperationsTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class StatusRepository(private val failure: ErrorKind? = null) : ProjectRepository by FakeProjectRepository() {
        val requested = mutableListOf<ProjectStatusOption>()
        var archived = false

        override suspend fun changeStatus(id: String, status: ProjectStatusOption): ApiResult<ProjectSummary> {
            requested += status
            return failure?.let { ApiResult.Failure(it) }
                ?: ApiResult.Success(ProjectSummary(id, "Solar Tech", "Fortaleza, CE", status.label, ""))
        }

        override suspend fun archive(id: String): ApiResult<Unit> {
            if (failure != null) return ApiResult.Failure(failure)
            archived = true
            return ApiResult.Success(Unit)
        }
    }

    private fun detail(repository: ProjectRepository) = ProjectDetailViewModel(SavedStateHandle(mapOf("id" to "2")), repository)

    @Test fun newStatusAppearsOnlyAfterTheServerConfirms() = runTest {
        val repository = StatusRepository()
        val model = detail(repository)
        advanceUntilIdle()

        model.changeStatus(ProjectStatusOption.IN_PROGRESS)
        advanceUntilIdle()

        assertEquals(listOf(ProjectStatusOption.IN_PROGRESS), repository.requested)
        assertEquals("Em andamento", (model.uiState.value as UiState.Content).data.status)
        assertEquals(null, model.action.value)
    }

    @Test fun closingWithPendingWorkKeepsTheStatusAndExplainsWhy() = runTest {
        val model = detail(StatusRepository(failure = ErrorKind.VALIDATION))
        advanceUntilIdle()
        val before = (model.uiState.value as UiState.Content).data.status

        model.changeStatus(ProjectStatusOption.COMPLETED)
        advanceUntilIdle()

        val failed = model.action.value as ProjectAction.Failed
        assertEquals(before, (model.uiState.value as UiState.Content).data.status)
        assertEquals("Conclua ou cancele as atividades e serviços pendentes antes de encerrar o projeto.", failed.message(TestStrings.resources))
    }

    @Test fun archivingSignalsTheScreenToLeave() = runTest {
        val repository = StatusRepository()
        val model = detail(repository)
        advanceUntilIdle()

        model.archive()
        advanceUntilIdle()

        assertTrue(repository.archived)
        assertEquals(ProjectAction.Archived, model.action.value)
    }

    @Test fun forbiddenArchiveIsNotTreatedAsSuccess() = runTest {
        val model = detail(StatusRepository(failure = ErrorKind.FORBIDDEN))
        advanceUntilIdle()

        model.archive()
        advanceUntilIdle()

        val failed = model.action.value as ProjectAction.Failed
        assertEquals(ProjectOperation.ARCHIVE, failed.operation)
        assertEquals("Só quem gerencia o projeto pode fazer isso.", failed.message(TestStrings.resources))
    }

    private class PhotoRepository(private val uploadFailure: ErrorKind? = null) : ProjectDetailRepository {
        var photoLoads = 0
        val stored = mutableListOf(ProjectPhoto("p1", true))
        override suspend fun location(projectId: String): ApiResult<ProjectLocationInfo> = ApiResult.Success(ProjectLocationInfo("", emptyList()))
        override suspend fun members(projectId: String): ApiResult<List<ProjectMember>> = ApiResult.Success(emptyList())
        override suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>> = ApiResult.Success(emptyList())
        override suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>> = ApiResult.Success(emptyList())
        override suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>> {
            photoLoads++
            return ApiResult.Success(stored.toList())
        }
        override suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1))
        override suspend fun uploadPhoto(projectId: String, image: ByteArray, mime: String): ApiResult<ProjectPhoto> {
            if (uploadFailure != null) return ApiResult.Failure(uploadFailure)
            return ProjectPhoto("p2", false).also { stored += it }.let { ApiResult.Success(it) }
        }
        override suspend fun removePhoto(projectId: String, photoId: String): ApiResult<Unit> {
            stored.removeAll { it.id == photoId }
            return ApiResult.Success(Unit)
        }
    }

    private fun sections(repository: ProjectDetailRepository) = ProjectSectionsViewModel(SavedStateHandle(mapOf("id" to "project-1")), repository)

    @Test fun uploadedPhotoAppearsAfterTheGalleryReloads() = runTest {
        val repository = PhotoRepository()
        val model = sections(repository)
        advanceUntilIdle()
        val loadsBefore = repository.photoLoads

        model.uploadPhoto(byteArrayOf(1, 2, 3), "image/jpeg")
        advanceUntilIdle()

        assertTrue(repository.photoLoads > loadsBefore)
        assertEquals(listOf("p1", "p2"), (model.photos.value as UiState.Content).data.map { it.id })
        assertEquals(null, model.photoAction.value)
    }

    @Test fun rejectedImageExplainsTheAcceptedFormats() = runTest {
        val model = sections(PhotoRepository(uploadFailure = ErrorKind.VALIDATION))
        advanceUntilIdle()

        model.uploadPhoto(byteArrayOf(1), "image/png")
        advanceUntilIdle()

        val failed = model.photoAction.value as ProjectAction.Failed
        assertEquals("Use uma imagem JPG ou PNG de até 8 MB. Cada projeto aceita até 30 imagens.", failed.message(TestStrings.resources))
    }

    private class ActivityRepository(private val failure: ErrorKind? = null) : ProjectDetailRepository {
        override suspend fun location(projectId: String): ApiResult<ProjectLocationInfo> = ApiResult.Success(ProjectLocationInfo("", emptyList()))
        override suspend fun members(projectId: String): ApiResult<List<ProjectMember>> = ApiResult.Success(emptyList())
        override suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>> = ApiResult.Success(emptyList())
        override suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>> = ApiResult.Success(emptyList())
        override suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1))
        var activityLoads = 0
        var status = ProjectStatusOption.OPEN
        override suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>> {
            activityLoads++
            return ApiResult.Success(listOf(ProjectActivity("a1", "Visita técnica", "Visita", "10:00", status)))
        }
        override suspend fun changeActivityStatus(projectId: String, activityId: String, status: ProjectStatusOption): ApiResult<ProjectActivity> {
            if (failure != null) return ApiResult.Failure(failure)
            this.status = status
            return ApiResult.Success(ProjectActivity(activityId, "Visita técnica", "Visita", "10:00", status))
        }
    }

    @Test fun activityStatusChangesOnlyAfterTheServerConfirms() = runTest {
        val repository = ActivityRepository()
        val model = sections(repository)
        model.open(com.project.solaria_mobile.feature.project.ProjectSheet.AGENDA)
        advanceUntilIdle()
        val loadsBefore = repository.activityLoads

        model.changeActivityStatus("a1", ProjectStatusOption.IN_PROGRESS)
        advanceUntilIdle()

        assertTrue(repository.activityLoads > loadsBefore)
        assertEquals(ProjectStatusOption.IN_PROGRESS, (model.activities.value as UiState.Content).data.single().status)
        assertEquals(null, model.activityAction.value)
    }

    @Test fun refusedActivityChangeIsNotTreatedAsSuccess() = runTest {
        val model = sections(ActivityRepository(failure = ErrorKind.FORBIDDEN))
        model.open(com.project.solaria_mobile.feature.project.ProjectSheet.AGENDA)
        advanceUntilIdle()

        model.changeActivityStatus("a1", ProjectStatusOption.COMPLETED)
        advanceUntilIdle()

        assertEquals(ProjectStatusOption.OPEN, (model.activities.value as UiState.Content).data.single().status)
        assertEquals("Você não pode alterar esta atividade.", (model.activityAction.value as ProjectAction.Failed).message(TestStrings.resources))
    }

    @Test fun removedPhotoLeavesTheGallery() = runTest {
        val repository = PhotoRepository()
        val model = sections(repository)
        advanceUntilIdle()

        model.removePhoto("p1")
        advanceUntilIdle()

        assertTrue(model.photos.value !is UiState.Content || (model.photos.value as UiState.Content).data.none { it.id == "p1" })
    }
}
