package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.feature.communication.CallDetailsViewModel
import com.project.solaria_mobile.feature.communication.data.ServiceCallDto
import com.project.solaria_mobile.feature.communication.data.toCall
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.domain.ServiceCallIcon
import com.project.solaria_mobile.feature.communication.domain.ServiceCallRepository
import com.project.solaria_mobile.feature.communication.domain.ServiceDecision
import java.time.OffsetDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceCallTest {
    @get:Rule val main = MainDispatcherRule()

    private class FakeServices : ServiceCallRepository {
        var item = ServiceCall("s", "Revisão", "Projeto", ServiceCallIcon.PHONE, status = "OPEN", canManage = true, hasExecutors = true)
        var failure: ErrorKind? = null
        var gate: CompletableDeferred<Unit>? = null
        var changes = 0
        override suspend fun calls() = ApiResult.Success(listOf(item))
        override suspend fun call(id: String) = ApiResult.Success(item)
        override suspend fun change(id: String, decision: ServiceDecision, scheduledAt: OffsetDateTime?): ApiResult<ServiceCall> {
            changes++
            gate?.await()
            failure?.let { return ApiResult.Failure(it) }
            item = when (decision) {
                ServiceDecision.ACCEPT -> item.copy(status = "IN_PROGRESS")
                ServiceDecision.COMPLETE -> item.copy(status = "COMPLETED")
                ServiceDecision.CANCEL -> item.copy(status = "CANCELED")
                ServiceDecision.SCHEDULE -> item.copy(scheduledAt = scheduledAt)
            }
            return ApiResult.Success(item)
        }
    }

    private fun model(repository: FakeServices) = CallDetailsViewModel(SavedStateHandle(mapOf("id" to "s")), repository)

    @Test fun viewerAndAgendaOwnedServicesNeverReachMutationEndpoint() = runTest {
        val repository = FakeServices().apply { item = item.copy(canManage = false) }
        val model = model(repository)
        advanceUntilIdle()
        model.change(ServiceDecision.ACCEPT)
        advanceUntilIdle()
        assertEquals(0, repository.changes)
        assertEquals(ErrorKind.FORBIDDEN, model.action.value.error)
        repository.item = repository.item.copy(canManage = true, managedByAgenda = true)
        model.load()
        advanceUntilIdle()
        model.change(ServiceDecision.ACCEPT)
        advanceUntilIdle()
        assertEquals(0, repository.changes)
    }

    @Test fun acceptanceBlocksDoubleTapAndReloadUntilWriteFinishes() = runTest {
        val repository = FakeServices()
        val model = model(repository)
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        model.change(ServiceDecision.ACCEPT)
        runCurrent()
        model.change(ServiceDecision.ACCEPT)
        model.load()
        assertTrue(model.action.value.busy)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.changes)
        assertFalse(model.action.value.busy)
        assertEquals("IN_PROGRESS", (model.uiState.value as UiState.Content).data.status)
        assertEquals(R.string.cd_servico_aceito, model.action.value.message)
    }

    @Test fun transientFailureKeepsOriginalServiceButForbiddenClearsContent() = runTest {
        val repository = FakeServices().apply { failure = ErrorKind.CONNECTIVITY }
        val model = model(repository)
        advanceUntilIdle()
        model.change(ServiceDecision.ACCEPT)
        advanceUntilIdle()
        assertEquals("OPEN", (model.uiState.value as UiState.Content).data.status)
        assertEquals(ErrorKind.CONNECTIVITY, model.action.value.error)
        repository.failure = ErrorKind.FORBIDDEN
        model.change(ServiceDecision.ACCEPT)
        advanceUntilIdle()
        assertEquals(UiState.Error(ErrorKind.FORBIDDEN), model.uiState.value)
    }

    @Test fun invalidStateMissingExecutorAndPastDateDoNotMakeRequests() = runTest {
        val repository = FakeServices().apply { item = item.copy(hasExecutors = false) }
        val model = model(repository)
        advanceUntilIdle()
        model.change(ServiceDecision.ACCEPT)
        advanceUntilIdle()
        model.change(ServiceDecision.COMPLETE)
        advanceUntilIdle()
        model.change(ServiceDecision.SCHEDULE, OffsetDateTime.now().minusDays(1))
        advanceUntilIdle()
        assertEquals(0, repository.changes)
        assertEquals(ErrorKind.VALIDATION, model.action.value.error)
    }

    @Test fun mappingKeepsProjectAndRealTimestampWithoutInventingAddress() {
        val dto = ServiceCallDto("s", "p", "Projeto", "Revisão", "OPEN", scheduledDate = "2030-04-20T12:30:00-03:00")
        val item = dto.toCall()
        assertEquals("p", item.projectId)
        assertEquals(OffsetDateTime.parse(dto.scheduledDate), item.scheduledAt)
        assertEquals("Aberto", item.badge)
        assertEquals("", item.address)
        assertNull(item.endedAt)
    }

    @Test fun observationIsRestoredAfterTransientWriteFailure() = runTest {
        val base = FakeServices().apply { failure = ErrorKind.CONNECTIVITY }
        val changes = MutableSharedFlow<ServiceCall>()
        var observations = 0
        val repository = object : ServiceCallRepository by base {
            override fun observeCall(id: String) = flow {
                observations++
                emit(ReadResult(base.call(id)))
                changes.collect { emit(ReadResult(ApiResult.Success(it))) }
            }
        }
        val model = CallDetailsViewModel(SavedStateHandle(mapOf("id" to "s")), repository)
        advanceUntilIdle()
        model.change(ServiceDecision.ACCEPT)
        advanceUntilIdle()
        assertEquals(2, observations)
        changes.emit(base.item.copy(status = "IN_PROGRESS"))
        advanceUntilIdle()
        assertEquals("IN_PROGRESS", (model.uiState.value as UiState.Content).data.status)
        assertEquals(ErrorKind.CONNECTIVITY, model.action.value.error)
    }
}
