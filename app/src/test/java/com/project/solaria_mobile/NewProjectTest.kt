package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.project.NewProjectEffect
import com.project.solaria_mobile.feature.project.NewProjectViewModel
import com.project.solaria_mobile.feature.project.data.toRequest
import com.project.solaria_mobile.feature.project.domain.NewProjectDraft
import com.project.solaria_mobile.feature.project.domain.NewProjectField
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NewProjectTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class RecordingRepository(var result: ApiResult<String> = ApiResult.Success("created-1")) : ProjectRepository {
        val drafts = mutableListOf<NewProjectDraft>()
        override suspend fun projects(): ApiResult<List<ProjectSummary>> = ApiResult.Success(emptyList())
        override suspend fun project(id: String): ApiResult<ProjectSummary> = ApiResult.Failure(ErrorKind.NOT_FOUND)
        override suspend fun create(draft: NewProjectDraft): ApiResult<String> {
            drafts += draft
            return result
        }
    }

    @Test fun aNameIsRequiredAndTheRestIsOptional() {
        assertEquals(setOf(NewProjectField.NAME), NewProjectDraft().validate())
        assertEquals(setOf(NewProjectField.NAME), NewProjectDraft(name = "  ab ").validate())
        assertTrue(NewProjectDraft(name = "Instalação no telhado").validate().isEmpty())
    }

    @Test fun theStateAndZipFollowTheServerRules() {
        assertEquals(setOf(NewProjectField.STATE), NewProjectDraft(name = "Obra nova", state = "XX").validate())
        assertTrue(NewProjectDraft(name = "Obra nova", state = "go").validate().isEmpty())
        assertEquals(setOf(NewProjectField.ZIP), NewProjectDraft(name = "Obra nova", zip = "7400").validate())
        assertTrue(NewProjectDraft(name = "Obra nova", zip = "74000-140").validate().isEmpty())
    }

    @Test fun theRequestOmitsBlankFieldsAndNormalizesTheLocation() {
        val request = NewProjectDraft(name = " Obra nova ", description = " ", state = "go", city = "Goiânia", zip = "74000-140").toRequest()
        assertEquals("Obra nova", request.name)
        assertNull(request.description)
        assertEquals("GO", request.location?.state)
        assertEquals("Goiânia", request.location?.city)
        assertEquals("74000140", request.location?.zipCode)
        assertNull(request.location?.street)
    }

    @Test fun aDraftWithoutLocationSendsNoLocation() {
        assertNull(NewProjectDraft(name = "Obra nova").toRequest().location)
    }

    @Test fun submitWithInvalidDataShowsTheErrorsAndDoesNotCallTheServer() = runTest {
        val repository = RecordingRepository()
        val model = NewProjectViewModel(repository, object : com.project.solaria_mobile.core.network.NetworkMonitor { override val isOnline = kotlinx.coroutines.flow.flowOf(true) })
        model.submit()
        advanceUntilIdle()
        assertEquals(setOf(NewProjectField.NAME), model.uiState.value.errors)
        assertTrue(repository.drafts.isEmpty())
    }

    @Test fun editingClearsTheErrors() = runTest {
        val model = NewProjectViewModel(RecordingRepository(), object : com.project.solaria_mobile.core.network.NetworkMonitor { override val isOnline = kotlinx.coroutines.flow.flowOf(true) })
        model.submit()
        model.update { it.copy(name = "Obra nova") }
        assertTrue(model.uiState.value.errors.isEmpty())
    }

    @Test fun aValidSubmitCreatesTheProjectAndEmitsItsId() = runTest {
        val repository = RecordingRepository()
        val model = NewProjectViewModel(repository, object : com.project.solaria_mobile.core.network.NetworkMonitor { override val isOnline = kotlinx.coroutines.flow.flowOf(true) })
        model.update { it.copy(name = "Obra nova") }
        model.effectFlow.test {
            model.submit()
            advanceUntilIdle()
            assertEquals(NewProjectEffect.Created("created-1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repository.drafts.size)
        assertFalse(model.uiState.value.submitting)
    }

    @Test fun aServerFailureKeepsTheFormAndShowsTheError() = runTest {
        val repository = RecordingRepository(ApiResult.Failure(ErrorKind.VALIDATION))
        val model = NewProjectViewModel(repository, object : com.project.solaria_mobile.core.network.NetworkMonitor { override val isOnline = kotlinx.coroutines.flow.flowOf(true) })
        model.update { it.copy(name = "Obra nova", city = "Goiânia") }
        model.submit()
        advanceUntilIdle()
        assertEquals(ErrorKind.VALIDATION, model.uiState.value.failure)
        assertFalse(model.uiState.value.submitting)
        assertEquals("Goiânia", model.uiState.value.draft.city)
    }
    @Test fun offlineSubmissionIsBlockedWithoutLosingTheDraft() = runTest {
        val network = object : com.project.solaria_mobile.core.network.NetworkMonitor {
            override val isOnline = kotlinx.coroutines.flow.MutableStateFlow(false)
        }
        val repository = RecordingRepository()
        val model = NewProjectViewModel(repository, network)
        advanceUntilIdle()
        model.update { it.copy(name = "Obra no telhado") }
        model.submit()
        advanceUntilIdle()
        assertTrue(repository.drafts.isEmpty())
        assertEquals("Obra no telhado", model.uiState.value.draft.name)
        assertEquals(ErrorKind.CONNECTIVITY, model.uiState.value.failure)
    }

}
