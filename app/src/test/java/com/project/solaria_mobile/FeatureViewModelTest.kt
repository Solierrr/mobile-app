package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.ConversationsViewModel
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.discovery.MapViewModel
import com.project.solaria_mobile.feature.discovery.ScheduleViewModel
import com.project.solaria_mobile.feature.discovery.data.FakeDiscoveryRepository
import com.project.solaria_mobile.feature.home.HomeViewModel
import com.project.solaria_mobile.feature.home.data.FakeHomeRepository
import com.project.solaria_mobile.feature.profile.ConnectionsViewModel
import com.project.solaria_mobile.feature.profile.data.FakeProfileRepository
import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import com.project.solaria_mobile.feature.project.ProjectDetailViewModel
import com.project.solaria_mobile.feature.project.ProjectListViewModel
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private suspend fun <T> ReceiveTurbine<UiState<T>>.awaitContent(): T {
        while (true) {
            val item = awaitItem()
            if (item is UiState.Content) return item.data
        }
    }

    private fun <T> ReceiveTurbine<UiState<T>>.latestContent(): T =
        (expectMostRecentItem() as UiState.Content).data

    private fun handleOf(id: String) = SavedStateHandle(mapOf("id" to id))

    private class FailingProfileRepository : ProfileRepository {
        override suspend fun ownProfile(): ApiResult<UserProfile> = ApiResult.Failure(ErrorKind.SERVER)
        override suspend fun profile(id: String): ApiResult<UserProfile> = ApiResult.Failure(ErrorKind.SERVER)
        override suspend fun connections(): ApiResult<List<Connection>> = ApiResult.Failure(ErrorKind.SERVER)
        override suspend fun isConnected(profileId: String): ApiResult<Boolean> = ApiResult.Failure(ErrorKind.SERVER)
        override suspend fun toggleConnection(profileId: String): ApiResult<Boolean> = ApiResult.Failure(ErrorKind.SERVER)
    }

    private class FailingProjectRepository : ProjectRepository {
        override suspend fun projects(): ApiResult<List<ProjectSummary>> = ApiResult.Failure(ErrorKind.CONNECTIVITY)
        override suspend fun project(id: String): ApiResult<ProjectSummary> = ApiResult.Failure(ErrorKind.CONNECTIVITY)
    }

    @Test fun sendingMessageUpdatesRepositoryAndClearsDraft() = runTest {
        val viewModel = ChatViewModel(handleOf("c1"), FakeCommunicationRepository())
        viewModel.uiState.test {
            assertEquals("Ricardo Dostoiévski", awaitContent().conversation.name)
            viewModel.onDraftChange("Nova mensagem")
            advanceUntilIdle()
            assertEquals("Nova mensagem", latestContent().draft)
            viewModel.send()
            advanceUntilIdle()
            val state = latestContent()
            assertEquals("", state.draft)
            assertEquals("Nova mensagem", state.messages.last().text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun unknownConversationShowsNotFound() = runTest {
        val viewModel = ChatViewModel(handleOf("missing"), FakeCommunicationRepository())
        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(UiState.Error(ErrorKind.NOT_FOUND), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun conversationsAreFilteredOutsideTheComposable() = runTest {
        val viewModel = ConversationsViewModel(FakeCommunicationRepository())
        viewModel.uiState.test {
            awaitContent()
            viewModel.onQueryChange("Bruce")
            advanceUntilIdle()
            val state = latestContent()
            assertEquals(2, state.conversations.size)
            assertTrue(state.conversations.all { it.name.contains("Bruce") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun scheduleSelectionChangesEvents() = runTest {
        val viewModel = ScheduleViewModel(FakeDiscoveryRepository())
        viewModel.uiState.test {
            assertEquals(3, awaitContent().selectedEvents.size)
            viewModel.selectDay(5)
            advanceUntilIdle()
            assertTrue(latestContent().selectedEvents.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun connectionsSearchChangesVisibleList() = runTest {
        val viewModel = ConnectionsViewModel(FakeProfileRepository())
        viewModel.uiState.test {
            awaitContent()
            viewModel.onQueryChange("Ana")
            advanceUntilIdle()
            assertEquals(2, latestContent().connections.map { it.name }.distinct().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun homeUsesSharedProjectAndCallSources() = runTest {
        val viewModel = HomeViewModel(FakeHomeRepository(), FakeProjectRepository(), FakeCommunicationRepository(), FakeProfileRepository())
        viewModel.uiState.test {
            val initial = awaitContent()
            assertEquals(4, initial.recentProjects.size)
            assertEquals(4, initial.recentCalls.size)
            viewModel.onCallsQueryChange("segurança")
            advanceUntilIdle()
            assertEquals(1, latestContent().recentCalls.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun homeAvatarComesFromTheOwnProfile() = runTest {
        val viewModel = HomeViewModel(FakeHomeRepository(), FakeProjectRepository(), FakeCommunicationRepository(), FakeProfileRepository())
        viewModel.uiState.test {
            assertEquals("RC", awaitContent().profile?.initials)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun homeStillLoadsWhenTheProfileFails() = runTest {
        val viewModel = HomeViewModel(FakeHomeRepository(), FakeProjectRepository(), FakeCommunicationRepository(), FailingProfileRepository())
        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals(null, content.profile)
            assertEquals(4, content.recentProjects.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun savedLocationSurvivesNewViewModelWithinSession() = runTest {
        val repository = FakeDiscoveryRepository()
        MapViewModel(repository).toggleSaved()
        advanceUntilIdle()
        val reopened = MapViewModel(repository)
        advanceUntilIdle()
        assertTrue((reopened.uiState.value as UiState.Content).data.saved)
    }

    @Test fun projectListShowsLoadFailure() = runTest {
        val viewModel = ProjectListViewModel(FailingProjectRepository())
        advanceUntilIdle()
        assertEquals(UiState.Error(ErrorKind.CONNECTIVITY), viewModel.uiState.value)
    }

    @Test fun projectDetailLoadsTheProjectFromTheRouteId() = runTest {
        val viewModel = ProjectDetailViewModel(handleOf("2"), FakeProjectRepository())
        advanceUntilIdle()
        assertEquals("Solar Tech", (viewModel.uiState.value as UiState.Content).data.name)
    }
}
