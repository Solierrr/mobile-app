package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.core.offline.CacheIdentity
import com.project.solaria_mobile.core.offline.CacheScope
import kotlinx.coroutines.flow.distinctUntilChanged
import retrofit2.http.Header
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class GoogleConnection(val configured: Boolean = false, val connected: Boolean = false, @SerialName("reconnect_required") val reconnectRequired: Boolean = false)
@Serializable
data class GoogleAuthorization(@SerialName("authorization_url") val url: String)
@Serializable
data class GoogleEvent(@SerialName("event_id") val id: String, val title: String? = null, val start: String? = null, val end: String? = null, val status: String? = null, @SerialName("html_link") val link: String? = null) {
    fun toSchedule(zone: ZoneId): ScheduleEvent? {
        if (status == "cancelled") return null
        val begin = start?.calendarMoment(zone) ?: return null
        return ScheduleEvent("google:$id", "", "Google Agenda", title?.takeIf { it.isNotBlank() } ?: "Evento sem título", begin, end?.calendarMoment(zone), source = "google", externalUrl = link, allDay = start.length == 10)
    }
}
@Serializable
data class GoogleEvents(val events: List<GoogleEvent>, @SerialName("next_page_token") val next: String? = null)
@Serializable
data class CalendarExport(@SerialName("source_id") val sourceId: String, val title: String, val start: String, val end: String, @SerialName("time_zone") val zone: String, val description: String)

interface GoogleCalendarApi {
    @GET("v1/calendar/me/connection") suspend fun connection(@Header("Authorization") authorization: String): GoogleConnection
    @POST("v1/calendar/me/connection") suspend fun connect(@Header("Authorization") authorization: String): GoogleAuthorization
    @DELETE("v1/calendar/me/connection") suspend fun disconnect(@Header("Authorization") authorization: String)
    @GET("v1/calendar/me/events") suspend fun events(@Query("from") start: String, @Query("to") end: String, @Query("page_token") page: String?, @Header("Authorization") authorization: String): GoogleEvents
    @POST("v1/calendar/me/events") suspend fun export(@Body body: CalendarExport, @Header("Authorization") authorization: String): GoogleEvent
}

@Singleton
class GoogleCalendarRepository internal constructor(private val api: GoogleCalendarApi?, private val executor: ApiCallExecutor, private val reader: OfflineReader, private val network: NetworkMonitor, private val mock: MockModeStore, private val tokens: TokenStore, private val identity: CacheScope) {
    @Inject constructor(factory: RetrofitFactory, executor: ApiCallExecutor, reader: OfflineReader, network: NetworkMonitor, mock: MockModeStore, tokens: TokenStore, identity: CacheIdentity) : this(
        BuildConfig.GOOGLE_REGISTRY_BASE_URL.takeIf { it.isNotBlank() }?.let { factory.create(it).create(GoogleCalendarApi::class.java) }, executor, reader, network, mock, tokens, identity,
    )
    val sessions = kotlinx.coroutines.flow.combine(tokens.tokens, mock.enabled) { _, _ -> identity.current() to mock.isEnabled() }.distinctUntilChanged()
    private var mockIdentity: String? = null
    private var mockConnected = false
    private val mockExports = mutableMapOf<String, ScheduleEvent>()

    private fun resetMock() {
        val actor = identity.current()
        if (actor != mockIdentity) { mockIdentity = actor; mockConnected = false; mockExports.clear() }
    }
    suspend fun connection(): ApiResult<GoogleConnection> {
        val session = capture()
        if (mock.isEnabled()) { resetMock(); return ApiResult.Success(GoogleConnection(true, mockConnected)) }
        val service = api ?: return ApiResult.Success(GoogleConnection())
        return scopedRead(session, "google-calendar:connection", GoogleConnection.serializer()) { service.connection(session.authorization) }
    }
    suspend fun connect(): ApiResult<String?> {
        val session = capture()
        if (mock.isEnabled()) { resetMock(); mockConnected = true; return ApiResult.Success(null) }
        val service = api ?: return ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        if (!network.isOnline.first()) return ApiResult.Failure(ErrorKind.CONNECTIVITY)
        return guarded(session) { service.connect(session.authorization).url }
    }
    suspend fun disconnect(): ApiResult<Unit> {
        val session = capture()
        if (mock.isEnabled()) { resetMock(); mockConnected = false; mockExports.clear(); return ApiResult.Success(Unit) }
        val service = api ?: return ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        if (!network.isOnline.first()) return ApiResult.Failure(ErrorKind.CONNECTIVITY)
        return guarded(session) { service.disconnect(session.authorization); if (current(session)) reader.invalidatePrefix("google-calendar:") }
    }
    suspend fun events(month: LocalDate): ApiResult<List<ScheduleEvent>> {
        val session = capture()
        val zone = ZoneId.systemDefault()
        val first = month.withDayOfMonth(1).let { it.minusDays((it.dayOfWeek.value % 7).toLong()) }
        val last = first.plusDays(42)
        if (mock.isEnabled()) { resetMock(); return ApiResult.Success(if (mockConnected) mockExports.values.filter { event -> (0L..41L).any { event.occursOn(first.plusDays(it), zone) } } else emptyList()) }
        val service = api ?: return ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        return scopedRead(session, "google-calendar:$first", ListSerializer(ScheduleEvent.serializer())) {
            val result = mutableListOf<ScheduleEvent>()
            val seen = mutableSetOf<String>()
            var page: String? = null
            do {
                val response = service.events(first.atStartOfDay(zone).toOffsetDateTime().toString(), last.atStartOfDay(zone).toOffsetDateTime().toString(), page, session.authorization)
                result += response.events.mapNotNull { it.toSchedule(zone) }
                page = response.next?.takeIf { it.isNotBlank() }
                check(page == null || seen.add(page))
            } while (page != null)
            result.distinctBy { it.id }
        }
    }
    suspend fun export(event: ScheduleEvent, end: OffsetDateTime): ApiResult<Unit> {
        val session = capture()
        val start = event.start(ZoneId.systemDefault()).toOffsetDateTime()
        if (event.source != "solaria" || !end.isAfter(start)) return ApiResult.Failure(ErrorKind.VALIDATION)
        if (mock.isEnabled()) {
            resetMock()
            if (!mockConnected) return ApiResult.Failure(ErrorKind.CONFLICT)
            mockExports[event.id] = event.copy(id = "google:${event.id}", projectId = "", projectName = "Google Agenda", endsAt = end.toString(), source = "google")
            return ApiResult.Success(Unit)
        }
        val service = api ?: return ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
        if (!network.isOnline.first()) return ApiResult.Failure(ErrorKind.CONNECTIVITY)
        return guarded(session) {
            service.export(CalendarExport("${event.projectId}/${event.id}", event.title, start.toString(), end.toString(), ZoneId.systemDefault().id, "Atividade do projeto ${event.projectName} no Solaria"), session.authorization)
            if (current(session)) reader.invalidatePrefix("google-calendar:")
        }
    }
    private data class Session(val actor: String?, val authorization: String, val mock: Boolean)
    private fun capture() = Session(identity.current(), "Bearer ${tokens.currentTokens()?.accessToken.orEmpty()}", mock.isEnabled())
    private fun current(session: Session) = session.actor != null && session.actor == identity.current() && session.mock == mock.isEnabled()
    private suspend fun <T> scopedRead(session: Session, key: String, serializer: kotlinx.serialization.KSerializer<T>, call: suspend () -> T): ApiResult<T> {
        if (!current(session)) return ApiResult.Failure(ErrorKind.UNAUTHORIZED)
        val result = reader.read(key, serializer) { check(current(session)); call().also { check(current(session)) } }
        return if (current(session)) result else ApiResult.Failure(ErrorKind.UNAUTHORIZED)
    }
    private suspend fun <T> guarded(session: Session, call: suspend () -> T): ApiResult<T> {
        if (!current(session)) return ApiResult.Failure(ErrorKind.UNAUTHORIZED)
        val result = executor.execute { check(current(session)); call() }
        return if (current(session)) result else ApiResult.Failure(ErrorKind.UNAUTHORIZED)
    }
}

private fun String.calendarMoment(zone: ZoneId): String? = runCatching {
    if (length == 10) LocalDate.parse(this).atStartOfDay(zone).toOffsetDateTime().toString() else OffsetDateTime.parse(this).toString()
}.getOrNull()
