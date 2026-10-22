package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ConversationsViewModel
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.profile.CompanyViewModel
import com.project.solaria_mobile.feature.profile.ConnectionsViewModel
import com.project.solaria_mobile.feature.profile.data.FakeProfileRepository
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextualStateTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun failedProfileLookupIsNotAbsenceOfCompany() = runTest {
        val profiles = object : ProfileRepository by FakeProfileRepository() {
            override fun observeOwnProfile() = flowOf(ReadResult(ApiResult.Failure(ErrorKind.CONNECTIVITY)))
        }
        val model = CompanyViewModel(SavedStateHandle(), FakeProjectRepository(), profiles)
        advanceUntilIdle()
        assertEquals(UiState.Error(ErrorKind.CONNECTIVITY), model.uiState.value)
    }

    @Test fun confirmedIndependentAccountHasNoCompany() = runTest {
        val model = CompanyViewModel(SavedStateHandle(), FakeProjectRepository(), FakeProfileRepository())
        advanceUntilIdle()
        assertEquals(UiState.Empty, model.uiState.value)
    }

    @Test fun companyWithoutMatchingProjectsIsNotReportedAsMissing() = runTest {
        val model = CompanyViewModel(SavedStateHandle(mapOf("id" to "company-without-projects")), FakeProjectRepository(), FakeProfileRepository())
        advanceUntilIdle()
        val company = (model.uiState.value as UiState.Content).data
        assertEquals("company-without-projects", company.id)
        assertFalse(company.detailsAvailable)
        assertTrue(company.projects.isEmpty())
    }

    @Test fun projectLookupFailureIsNotAnEmptyCompany() = runTest {
        val projects = object : ProjectRepository by FakeProjectRepository() {
            override fun observeProjects() = flowOf(ReadResult(ApiResult.Failure(ErrorKind.TIMEOUT)))
        }
        val model = CompanyViewModel(SavedStateHandle(mapOf("id" to "c1")), projects, FakeProfileRepository())
        advanceUntilIdle()
        assertEquals(UiState.Error(ErrorKind.TIMEOUT), model.uiState.value)
    }

    @Test fun cachedProfileWithoutCompanyWaitsForRefreshBeforeDeciding() = runTest {
        val own = (FakeProfileRepository().ownProfile() as ApiResult.Success).data
        val profiles = object : ProfileRepository by FakeProfileRepository() {
            override fun observeOwnProfile() = flowOf(
                ReadResult(ApiResult.Success(own), cached = true, refreshing = true),
                ReadResult(ApiResult.Failure(ErrorKind.SERVER)),
            )
        }
        val model = CompanyViewModel(SavedStateHandle(), FakeProjectRepository(), profiles)
        advanceUntilIdle()
        assertEquals(UiState.Error(ErrorKind.SERVER), model.uiState.value)
    }

    @Test fun emptyConversationListStillProvidesSearchState() = runTest {
        val repository = object : com.project.solaria_mobile.feature.communication.domain.CommunicationRepository by FakeCommunicationRepository() {
            override suspend fun conversations() = ApiResult.Success(emptyList<com.project.solaria_mobile.feature.communication.domain.Conversation>())
        }
        val model = ConversationsViewModel(repository)
        model.uiState.test {
            advanceUntilIdle()
            assertTrue(expectMostRecentItem() is UiState.Content)
            model.onQueryChange("Ana")
            advanceUntilIdle()
            val content = (expectMostRecentItem() as UiState.Content).data
            assertEquals("Ana", content.query)
            assertTrue(content.conversations.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun clearingUnmatchedConnectionSearchRestoresResults() = runTest {
        val model = ConnectionsViewModel(FakeProfileRepository(), object : com.project.solaria_mobile.core.network.NetworkMonitor { override val isOnline = kotlinx.coroutines.flow.MutableStateFlow(true) })
        model.uiState.test {
            advanceUntilIdle()
            val initial = (expectMostRecentItem() as UiState.Content).data.connections.size
            model.onQueryChange("no-such-person")
            advanceUntilIdle()
            assertTrue((expectMostRecentItem() as UiState.Content).data.connections.isEmpty())
            model.onQueryChange("")
            advanceUntilIdle()
            assertEquals(initial, (expectMostRecentItem() as UiState.Content).data.connections.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun homeSheetKeepsUnsupportedCallsDistinctFromEmptyCalls() = runTest {
        val repository = object : com.project.solaria_mobile.feature.communication.domain.CommunicationRepository by FakeCommunicationRepository() {
            override suspend fun calls() = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        }
        val model = com.project.solaria_mobile.feature.home.HomeViewModel(FakeProjectRepository(), repository, FakeProfileRepository())
        model.uiState.test {
            advanceUntilIdle()
            val content = (expectMostRecentItem() as UiState.Content).data
            assertEquals(ErrorKind.NOT_SUPPORTED, content.callsFailure)
            assertTrue(content.recentCalls.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
