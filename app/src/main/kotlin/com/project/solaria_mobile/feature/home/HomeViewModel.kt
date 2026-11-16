package com.project.solaria_mobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.domain.ServiceCallRepository
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.home.domain.RecentCompany
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.project.solaria_mobile.feature.project.data.ProjectAccessLog
import kotlinx.coroutines.flow.flowOf
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import java.time.ZoneId
import java.time.ZonedDateTime

data class HomeUiState(
    val recentCompanies: List<RecentCompany> = emptyList(),
    val recentProjects: List<ProjectSummary> = emptyList(),
    val callsQuery: String = "",
    val recentCalls: List<ServiceCall> = emptyList(),
    val profile: UserProfile? = null,
    val callsFailure: ErrorKind? = null,
    val callsLoading: Boolean = false,
    val today: TodayAgenda = TodayAgenda.Loading,
    val pendingInvitations: Int = 0,
)

/** Estado próprio da seção de hoje: a Home continua útil mesmo quando só a agenda falha. */
sealed interface TodayAgenda {
    data object Loading : TodayAgenda
    data object Unavailable : TodayAgenda
    data class Ready(val events: List<ScheduleEvent>, val more: Int) : TodayAgenda
}

private const val RECENT_LIMIT = 4
private const val RECENT_PROJECT_LIMIT = 3
private const val RECENT_COMPANY_LIMIT = 8
private const val TODAY_LIMIT = 3

private data class HomeContent(
    val projects: List<ProjectSummary>,
    val calls: List<ServiceCall>,
    val profile: UserProfile?,
    val callsFailure: ErrorKind?,
    val callsLoading: Boolean,
)

@HiltViewModel
class HomeViewModel internal constructor(
    private val projectRepository: ProjectRepository,
    private val callsRepository: ServiceCallRepository,
    private val profileRepository: ProfileRepository,
    private val accessLog: ProjectAccessLog? = null,
    private val schedule: DiscoveryRepository? = null,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val now: () -> ZonedDateTime = { ZonedDateTime.now(zone()) },
) : ViewModel() {
    @Inject constructor(
        projectRepository: ProjectRepository,
        callsRepository: ServiceCallRepository,
        profileRepository: ProfileRepository,
        accessLog: ProjectAccessLog,
        schedule: DiscoveryRepository,
    ) : this(projectRepository, callsRepository, profileRepository, accessLog, schedule, { ZoneId.systemDefault() }, { ZonedDateTime.now() })

    private val access = accessLog?.observe() ?: flowOf(emptyMap())
    private val callsQuery = MutableStateFlow("")
    private val content = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    private val agenda = MutableStateFlow<ApiResult<List<ScheduleEvent>>?>(null)
    private val invitations = MutableStateFlow(0)
    private val loadedCalls = MutableStateFlow<com.project.solaria_mobile.core.common.ReadResult<List<ServiceCall>>?>(null)

    val uiState: StateFlow<UiState<HomeUiState>> = combine(content, callsQuery, access, agenda, invitations) { loaded, query, lastAccess, events, invited ->
        when (loaded) {
            is UiState.Content -> UiState.Content(
                HomeUiState(
                    recentCompanies = recentCompaniesOf(loaded.data.projects),
                    recentProjects = recentProjectsOf(loaded.data.projects, lastAccess),
                    callsQuery = query,
                    profile = loaded.data.profile,
                    callsFailure = loaded.data.callsFailure,
                    callsLoading = loaded.data.callsLoading,
                    today = when (events) {
                        null -> TodayAgenda.Loading
                        is ApiResult.Failure -> TodayAgenda.Unavailable
                        is ApiResult.Success -> todayAgendaOf(events.data, now(), zone())
                    },
                    pendingInvitations = invited,
                    recentCalls = loaded.data.calls.take(RECENT_LIMIT).filter {
                        it.title.contains(query, ignoreCase = true) || it.detail.contains(query, ignoreCase = true)
                    },
                ), loaded.refreshing, loaded.syncedAt, loaded.cached, loaded.refreshError,
            )
            UiState.Loading -> UiState.Loading
            UiState.Empty -> UiState.Empty
            is UiState.Error -> loaded
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private var loadJob: kotlinx.coroutines.Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val previous = content.value as? UiState.Content
            schedule?.let { source ->
                launch {
                    source.observeEvents().collect { read ->
                        // Falha ao atualizar não apaga a agenda que já estava na tela.
                        if (read.result is ApiResult.Success || agenda.value !is ApiResult.Success) agenda.value = read.result
                    }
                }
            }
            launch { invitations.value = (projectRepository.myInvitations() as? ApiResult.Success)?.data?.count { it.pending } ?: 0 }
            content.value = previous?.copy(refreshing = true) ?: UiState.Loading
            launch {
                callsRepository.observeCalls().collect { read ->
                    val old = loadedCalls.value
                    val failed = read.result as? ApiResult.Failure
                    loadedCalls.value = if (failed != null && old != null && old.result is ApiResult.Success && failed.kind in setOf(ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT, ErrorKind.SERVER, ErrorKind.RATE_LIMITED))
                        old.copy(cached = true, refreshing = false, refreshError = failed.kind) else read
                }
            }
            combine(projectRepository.observeProjects(), profileRepository.observeOwnProfile(), loadedCalls) { projects, profile, callsRead ->
                val result = projects.result
                when (result) {
                    is ApiResult.Success -> UiState.Content(
                        HomeContent(result.data, (callsRead?.result as? ApiResult.Success)?.data.orEmpty(), (profile.result as? ApiResult.Success)?.data,
                            (callsRead?.result as? ApiResult.Failure)?.kind ?: callsRead?.refreshError, callsRead == null),
                        refreshing = projects.refreshing || profile.refreshing,
                        syncedAt = projects.syncedAt,
                        cached = projects.cached,
                        refreshError = projects.refreshError ?: profile.refreshError,
                    )
                    is ApiResult.Failure -> UiState.Error(result.kind)
                }
            }.collect { content.value = it }
        }
    }

    fun onCallsQueryChange(value: String) { callsQuery.value = value }
}

internal fun recentCompaniesOf(projects: List<ProjectSummary>): List<RecentCompany> =
    projects
        .filter { it.companyId != null && it.companyName.isNotBlank() }
        .sortedByDescending { it.createdAtMillis }
        .distinctBy { it.companyId }
        .take(RECENT_COMPANY_LIMIT)
        .map { RecentCompany(id = it.companyId.orEmpty(), name = it.companyName, logoUrl = it.companyLogoUrl) }

/**
 * Projetos abertos pela pessoa vêm primeiro, do acesso mais recente ao mais antigo; os nunca abertos completam a lista
 * pela data de criação. A data de criação não substitui o último acesso.
 */
internal fun recentProjectsOf(projects: List<ProjectSummary>, lastAccess: Map<String, Long>): List<ProjectSummary> =
    projects.sortedWith(compareByDescending<ProjectSummary> { lastAccess[it.id] ?: Long.MIN_VALUE }.thenByDescending { it.createdAtMillis })
        .take(RECENT_PROJECT_LIMIT)

/**
 * Compromissos de hoje no fuso do aparelho: atividades que ainda acontecem hoje, sem as encerradas ou canceladas,
 * em ordem de horário. O que passar do limite vira contagem para abrir a agenda completa.
 */
internal fun todayAgendaOf(events: List<ScheduleEvent>, now: ZonedDateTime, zone: ZoneId): TodayAgenda.Ready {
    val today = now.withZoneSameInstant(zone).toLocalDate()
    val open = events.filter { event ->
        event.status?.uppercase() !in CLOSED_STATUSES && runCatching { event.occursOn(today, zone) }.getOrDefault(false)
    }.sortedBy { it.start(zone).toInstant() }
    return TodayAgenda.Ready(open.take(TODAY_LIMIT), (open.size - TODAY_LIMIT).coerceAtLeast(0))
}

private val CLOSED_STATUSES = setOf("COMPLETED", "CANCELED", "CANCELLED")
