package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.project.ProjectDetailViewModel
import com.project.solaria_mobile.feature.project.ProjectListViewModel
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelStartupTest {
    @get:Rule val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @Test fun projectListCanReceiveInvitationsBeforeConstructorReturns() = runTest {
        val model = ProjectListViewModel(FakeProjectRepository())
        assertTrue(model.uiState.value is UiState.Content)
        assertTrue(model.invitations.value.isEmpty())
        model.viewModelScope.cancel()
    }

    @Test fun detailRefreshCancelsTheCollectorStartedDuringConstruction() = runTest {
        val source = FakeProjectRepository()
        val project = (source.project("1") as ApiResult.Success).data
        var collectors = 0
        val repository = object : ProjectRepository by source {
            override fun observeProject(id: String) = flow {
                collectors++
                try {
                    emit(ReadResult(ApiResult.Success(project)))
                    awaitCancellation()
                } finally {
                    collectors--
                }
            }
        }
        val model = ProjectDetailViewModel(SavedStateHandle(mapOf("id" to "1")), repository)
        assertEquals(1, collectors)
        model.load()
        assertEquals(1, collectors)
        model.viewModelScope.cancel()
        assertEquals(0, collectors)
    }
}
