package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.ConversationsViewModel
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.discovery.data.FakeDiscoveryRepository
import com.project.solaria_mobile.feature.discovery.ScheduleViewModel
import com.project.solaria_mobile.feature.discovery.MapViewModel
import com.project.solaria_mobile.feature.home.HomeViewModel
import com.project.solaria_mobile.feature.home.data.FakeHomeRepository
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.profile.ConnectionsViewModel
import com.project.solaria_mobile.feature.profile.data.FakeProfileRepository
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

    @Test fun sendingMessageUpdatesRepositoryAndClearsDraft() = runTest {
        val repository = FakeCommunicationRepository()
        val viewModel = ChatViewModel(repository)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onDraftChange("Nova mensagem")
            advanceUntilIdle()
            assertEquals("Nova mensagem", awaitItem().draft)
            viewModel.send()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("", state.draft)
            assertEquals("Nova mensagem", state.messages.last().text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun conversationsAreFilteredOutsideTheComposable() = runTest {
        val viewModel = ConversationsViewModel(FakeCommunicationRepository())
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("Bruce")
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(2, state.conversations.size)
            assertTrue(state.conversations.all { it.name.contains("Bruce") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun scheduleSelectionChangesEvents() = runTest {
        val viewModel = ScheduleViewModel(FakeDiscoveryRepository())
        viewModel.uiState.test {
            assertEquals(3, awaitItem().selectedEvents.size)
            viewModel.selectDay(5)
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().selectedEvents.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun connectionsSearchChangesVisibleList() = runTest {
        val viewModel = ConnectionsViewModel(FakeProfileRepository())
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("Ana")
            advanceUntilIdle()
            assertEquals(2, expectMostRecentItem().connections.map { it.name }.distinct().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun homeUsesSharedProjectAndCallSources() = runTest {
        val viewModel = HomeViewModel(FakeHomeRepository(), FakeProjectRepository(), FakeCommunicationRepository())
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(4, initial.recentProjects.size)
            assertEquals(4, initial.recentCalls.size)
            viewModel.onCallsQueryChange("segurança")
            advanceUntilIdle()
            assertEquals(1, expectMostRecentItem().recentCalls.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun savedLocationSurvivesNewViewModelWithinSession() = runTest {
        val repository = FakeDiscoveryRepository()
        MapViewModel(repository).toggleSaved()
        val reopened = MapViewModel(repository)
        assertTrue(reopened.uiState.value.saved)
    }
}
