package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.ServiceCallRepository
import com.project.solaria_mobile.feature.home.HomeViewModel
import com.project.solaria_mobile.feature.profile.data.FakeProfileRepository
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeServiceLoadingTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun slowServiceApiDoesNotHideCachedProfileAndProjects() = runTest {
        val release = CompletableDeferred<Unit>()
        val services = object : ServiceCallRepository by FakeCommunicationRepository() {
            override fun observeCalls() = flow { release.await(); emit(ReadResult(ApiResult.Success(emptyList<com.project.solaria_mobile.feature.communication.domain.ServiceCall>()))) }
        }
        val model = HomeViewModel(FakeProjectRepository(), services, FakeProfileRepository())
        model.uiState.test {
            advanceUntilIdle()
            val before = (expectMostRecentItem() as UiState.Content).data
            assertNotNull(before.profile)
            assertTrue(before.callsLoading)
            release.complete(Unit)
            advanceUntilIdle()
            assertFalse((expectMostRecentItem() as UiState.Content).data.callsLoading)
        }
    }
}
