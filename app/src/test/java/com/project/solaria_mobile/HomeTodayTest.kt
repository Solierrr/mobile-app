package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.discovery.data.FakeDiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import com.project.solaria_mobile.feature.home.HomeUiState
import com.project.solaria_mobile.feature.home.HomeViewModel
import com.project.solaria_mobile.feature.home.TodayAgenda
import com.project.solaria_mobile.feature.home.todayAgendaOf
import com.project.solaria_mobile.feature.home.todayTimeLabel
import com.project.solaria_mobile.feature.profile.data.FakeProfileRepository
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectInvitation
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeTodayTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = ZonedDateTime.of(2026, 1, 4, 10, 0, 0, 0, zone)

    private fun event(id: String, startsAt: String, status: String? = null, endsAt: String? = null) =
        ScheduleEvent(id, "p1", "Obra", "Atividade $id", startsAt, endsAt, status)

    @Test fun todayUsesTheDeviceZoneAndSkipsClosedActivities() {
        val events = listOf(
            event("tarde", "2026-01-04T18:00:00Z"),
            event("manha", "2026-01-04T12:00:00Z"),
            // 01:30 em UTC do dia 5 ainda é a noite do dia 4 em São Paulo.
            event("noite", "2026-01-05T01:30:00Z"),
            // 01:30 em UTC do dia 4 é o dia 3 em São Paulo.
            event("ontem", "2026-01-04T01:30:00Z"),
            event("feita", "2026-01-04T13:00:00Z", "COMPLETED"),
            event("cancelada", "2026-01-04T14:00:00Z", "CANCELED"),
        )

        val today = todayAgendaOf(events, now, zone)

        assertEquals(listOf("manha", "tarde", "noite"), today.events.map { it.id })
        assertEquals(0, today.more)
    }

    @Test fun extraActivitiesBecomeACountForTheFullAgenda() {
        val events = (1..5).map { event("e$it", "2026-01-04T1${it}:00:00Z") }

        val today = todayAgendaOf(events, now, zone)

        assertEquals(3, today.events.size)
        assertEquals(2, today.more)
    }

    @Test fun ongoingActivityFromAnotherDayShowsAsInProgress() {
        val date = LocalDate.of(2026, 1, 4)
        assertEquals("09:00", todayTimeLabel(event("a", "2026-01-04T12:00:00Z"), date, zone))
        assertEquals("Em curso", todayTimeLabel(event("b", "2026-01-02T12:00:00Z", endsAt = "2026-01-06T12:00:00Z"), date, zone))
    }

    @Test fun homeShowsTodayAndPendingInvitations() = runTest {
        val projects = object : ProjectRepository by FakeProjectRepository() {
            override suspend fun myInvitations(): ApiResult<List<ProjectInvitation>> = ApiResult.Success(
                listOf(
                    ProjectInvitation("i1", "p9", "Usina Sul", "eu@empresa.com", "Membro", true, ""),
                    ProjectInvitation("i2", "p8", "Usina Norte", "eu@empresa.com", "Membro", false, ""),
                ),
            )
        }

        val state = homeState(projects, FakeDiscoveryRepository())

        assertEquals(3, (state.today as TodayAgenda.Ready).events.size)
        assertEquals(1, state.pendingInvitations)
    }

    @Test fun agendaFailureOnlyAffectsTheTodaySection() = runTest {
        val failing = object : DiscoveryRepository {
            override suspend fun events(): ApiResult<List<ScheduleEvent>> = ApiResult.Failure(ErrorKind.CONNECTIVITY)
            override suspend fun savedLocations(): ApiResult<List<String>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
            override suspend fun isLocationSaved(): ApiResult<Boolean> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
            override suspend fun toggleLocationSaved(): ApiResult<Boolean> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        }

        val state = homeState(FakeProjectRepository(), failing)

        assertEquals(TodayAgenda.Unavailable, state.today)
        assertEquals(3, state.recentProjects.size)
        assertEquals(0, state.pendingInvitations)
    }

    private fun TestScope.homeState(projects: ProjectRepository, schedule: DiscoveryRepository): HomeUiState {
        val viewModel = HomeViewModel(projects, FakeCommunicationRepository(), FakeProfileRepository(), null, schedule, { zone }, { now })
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return (viewModel.uiState.value as UiState.Content).data
    }
}
