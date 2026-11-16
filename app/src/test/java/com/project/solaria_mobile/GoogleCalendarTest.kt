package com.project.solaria_mobile

import com.project.solaria_mobile.core.auth.AuthTokens
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.offline.CacheEntry
import com.project.solaria_mobile.core.offline.CacheScope
import com.project.solaria_mobile.core.offline.CacheStore
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.discovery.data.GoogleCalendarApi
import com.project.solaria_mobile.feature.discovery.data.GoogleCalendarRepository
import com.project.solaria_mobile.feature.discovery.data.GoogleConnection
import com.project.solaria_mobile.feature.discovery.data.GoogleEvent
import com.project.solaria_mobile.feature.discovery.data.GoogleEvents
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleCalendarTest {
    private class MemoryStore : CacheStore {
        val entries = mutableMapOf<String, CacheEntry>()
        override suspend fun get(cacheKey: String) = entries[cacheKey]
        override suspend fun put(cacheKey: String, json: String, syncedAt: Long) { entries[cacheKey] = CacheEntry(cacheKey, json, syncedAt) }
        override suspend fun clear() { entries.clear() }
        override suspend fun remove(cacheKey: String) { entries.remove(cacheKey) }
        override suspend fun removePrefix(prefix: String) { entries.keys.removeAll { it.startsWith(prefix) } }
    }
    private class Fixture {
        val online = MutableStateFlow(true)
        val enabled = MutableStateFlow(false)
        val auth = MutableStateFlow<AuthTokens?>(AuthTokens("alice", "refresh"))
        val tokens = mockk<TokenStore> { every { tokens } returns auth; every { currentTokens() } answers { auth.value } }
        val mock = mockk<MockModeStore> { every { enabled } returns this@Fixture.enabled; every { isEnabled() } answers { enabled.value } }
        val network = object : NetworkMonitor { override val isOnline = online }
        val api = mockk<GoogleCalendarApi>()
        val executor = ApiCallExecutor(ApiErrorParser(ApiJson.instance))
        val reader = OfflineReader(MemoryStore(), network, executor, CacheScope { auth.value?.accessToken }, { 1L })
        val repository = GoogleCalendarRepository(api, executor, reader, network, mock, tokens, CacheScope { auth.value?.accessToken })
    }

    @Test fun allDayEventEndIsExclusiveAndCancelledEventsAreSkipped() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val event = GoogleEvent("g1", start = "2026-10-30", end = "2026-10-31").toSchedule(zone)!!
        assertTrue(event.allDay)
        assertTrue(event.occursOn(LocalDate.of(2026, 10, 30), zone))
        assertFalse(event.occursOn(LocalDate.of(2026, 10, 31), zone))
        assertEquals("google:g1", event.id)
        assertNull(GoogleEvent("g1", start = "bad").toSchedule(zone))
        assertNull(GoogleEvent("g1", start = "2026-10-30", status = "cancelled").toSchedule(zone))
    }

    @Test fun paginatedEventsRemainReadableOfflineAndNeverLeakToAnotherAccount() = runTest {
        val f = Fixture()
        coEvery { f.api.connection(any()) } returns GoogleConnection(true, true)
        coEvery { f.api.events(any(), any(), null, any()) } returns GoogleEvents(listOf(GoogleEvent("a", start = "2026-10-30")), "page2")
        coEvery { f.api.events(any(), any(), "page2", any()) } returns GoogleEvents(listOf(GoogleEvent("b", start = "2026-10-31")))
        assertEquals(ApiResult.Success(GoogleConnection(true, true)), f.repository.connection())
        val date = LocalDate.of(2026, 10, 1)
        val loaded = f.repository.events(date)
        assertEquals(2, (loaded as ApiResult.Success).data.size)
        f.online.value = false
        assertEquals(loaded, f.repository.events(date))
        assertEquals(ApiResult.Success(GoogleConnection(true, true)), f.repository.connection())
        f.auth.value = AuthTokens("bob", "refresh")
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), f.repository.events(date))
        coVerify(exactly = 2) { f.api.events(any(), any(), any(), any()) }
    }

    @Test fun repeatedPageTokenFailsInsteadOfLoopingOrCachingPartialAgenda() = runTest {
        val f = Fixture()
        coEvery { f.api.events(any(), any(), any(), any()) } returns GoogleEvents(emptyList(), "same-page")
        assertTrue(f.repository.events(LocalDate.of(2026, 10, 1)) is ApiResult.Failure)
        coVerify(exactly = 2) { f.api.events(any(), any(), any(), any()) }
    }

    @Test fun mockConnectionAndExportNeverCallGoogleAndCannotCrossAccounts() = runTest {
        val f = Fixture()
        f.enabled.value = true
        assertEquals(ApiResult.Success(null), f.repository.connect())
        val event = ScheduleEvent("activity", "project", "Obra", "Visita", "2026-10-30T12:00:00Z")
        assertEquals(ApiResult.Failure(ErrorKind.VALIDATION), f.repository.export(event, OffsetDateTime.parse(event.startsAt)))
        assertEquals(ApiResult.Success(Unit), f.repository.export(event, OffsetDateTime.parse("2026-10-30T13:00:00Z")))
        assertEquals(1, (f.repository.events(LocalDate.of(2026, 10, 1)) as ApiResult.Success).data.size)
        f.auth.value = AuthTokens("bob", "refresh")
        assertEquals(ApiResult.Success(GoogleConnection(true, false)), f.repository.connection())
        assertEquals(emptyList<ScheduleEvent>(), (f.repository.events(LocalDate.of(2026, 10, 1)) as ApiResult.Success).data)
        coVerify(exactly = 0) { f.api.connection(any()) }
        coVerify(exactly = 0) { f.api.export(any(), any()) }
    }

    @Test fun exportsNeedInternetAndUseTheActivityIdentity() = runTest {
        val f = Fixture()
        val event = ScheduleEvent("activity", "project", "Obra", "Visita", "2026-10-30T12:00:00Z")
        f.online.value = false
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), f.repository.export(event, OffsetDateTime.parse("2026-10-30T13:00:00Z")))
        f.online.value = true
        coEvery { f.api.export(any(), any()) } returns GoogleEvent("exported")
        assertEquals(ApiResult.Success(Unit), f.repository.export(event, OffsetDateTime.parse("2026-10-30T13:00:00Z")))
        coVerify { f.api.export(match { it.sourceId == "project/activity" && it.end == "2026-10-30T13:00Z" }, any()) }
    }

    @Test fun accountSwitchWhileWaitingForNetworkCannotExportAsNewAccount() = runTest {
        val f = Fixture()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val delayed = object : NetworkMonitor {
            override val isOnline = kotlinx.coroutines.flow.flow { entered.complete(Unit); gate.await(); emit(true) }
        }
        val repo = GoogleCalendarRepository(f.api, f.executor, f.reader, delayed, f.mock, f.tokens, CacheScope { f.auth.value?.accessToken })
        val event = ScheduleEvent("a", "p", "Obra", "Visita", "2026-10-30T12:00:00Z")
        val result = async { repo.export(event, OffsetDateTime.parse("2026-10-30T13:00:00Z")) }
        entered.await()
        f.auth.value = AuthTokens("bob", "refresh")
        gate.complete(Unit)
        assertEquals(ApiResult.Failure(ErrorKind.UNAUTHORIZED), result.await())
        coVerify(exactly = 0) { f.api.export(any(), any()) }
    }

}
