package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.offline.CacheEntry
import com.project.solaria_mobile.core.offline.CacheScope
import com.project.solaria_mobile.core.offline.CacheStore
import com.project.solaria_mobile.core.offline.OfflineReader
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.project.solaria_mobile.feature.project.data.ProjectApi
import com.project.solaria_mobile.feature.project.data.ProjectDto
import com.project.solaria_mobile.feature.project.data.RemoteProjectRepository
import kotlinx.serialization.builtins.ListSerializer
import io.mockk.coEvery
import io.mockk.mockk
import retrofit2.HttpException
import retrofit2.Response

class OfflineReaderTest {
    private class MemoryStore : CacheStore {
        val entries = mutableMapOf<String, CacheEntry>()
        override suspend fun get(cacheKey: String) = entries[cacheKey]
        override suspend fun put(cacheKey: String, json: String, syncedAt: Long) { entries[cacheKey] = CacheEntry(cacheKey, json, syncedAt) }
        override suspend fun clear() { entries.clear() }
        override suspend fun remove(cacheKey: String) { entries.remove(cacheKey) }
        override suspend fun removePrefix(prefix: String) { entries.keys.removeAll { it.startsWith(prefix) } }
    }

    private class Fixture(online: Boolean = true) {
        val store = MemoryStore()
        val connected = MutableStateFlow(online)
        var account: String? = "qa:alice"
        val reader = OfflineReader(store, object : NetworkMonitor { override val isOnline = connected }, ApiCallExecutor(ApiErrorParser(com.project.solaria_mobile.core.network.ApiJson.instance)), CacheScope { account }, { 200L })
        suspend fun seed() { store.put("qa:alice:projects", "\"saved\"", 100L) }
    }

    @Test fun cacheIsVisibleBeforeTheNetworkCompletes() = runTest {
        val f = Fixture()
        f.seed()
        val remote = CompletableDeferred<String>()
        f.reader.observe("projects", String.serializer()) { remote.await() }.test {
            val cached = awaitItem()
            assertEquals("saved", (cached.result as ApiResult.Success).data)
            assertTrue(cached.cached)
            assertTrue(cached.refreshing)
            remote.complete("fresh")
            val updated = awaitItem()
            assertEquals("fresh", (updated.result as ApiResult.Success).data)
            assertFalse(updated.refreshing)
            assertEquals(200L, updated.syncedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun invalidatingPeopleDoesNotDeleteProjectsOrAllowLateRepopulation() = runTest {
        val f = Fixture()
        f.seed()
        f.store.put("qa:alice:people:true:0:", "\"old\"", 100L)
        val response = CompletableDeferred<String>()
        f.reader.observe("people:true:0:", String.serializer()) { response.await() }.test {
            awaitItem()
            f.reader.invalidatePrefix("people:")
            response.complete("stale")
            assertEquals(ErrorKind.CONFLICT, (awaitItem().result as ApiResult.Failure).kind)
            assertTrue(f.store.entries.containsKey("qa:alice:projects"))
            assertFalse(f.store.entries.containsKey("qa:alice:people:true:0:"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun cachedReadIsIsolatedAndDoesNotWaitForTheNetwork() = runTest {
        val f = Fixture()
        f.seed()
        val saved = f.reader.cached("projects", String.serializer())!!
        assertEquals("saved", (saved.result as ApiResult.Success).data)
        assertEquals(100L, saved.syncedAt)
        assertTrue(saved.cached)
        f.account = "qa:bob"
        assertEquals(null, f.reader.cached("projects", String.serializer()))
    }

    @Test fun firstProjectVisitShowsItsSavedListSummaryBeforeDetailFailure() = runTest {
        val f = Fixture()
        f.reader.read("projects", ListSerializer(ProjectDto.serializer())) { listOf(ProjectDto("p1", "Saved project")) }
        val api = mockk<ProjectApi>()
        coEvery { api.project("p1") } throws IOException("offline")
        val network = object : NetworkMonitor { override val isOnline = f.connected }
        val repository = RemoteProjectRepository(api, ApiCallExecutor(ApiErrorParser(com.project.solaria_mobile.core.network.ApiJson.instance)), f.reader, network)
        repository.observeProject("p1").test {
            val saved = awaitItem()
            assertEquals("Saved project", (saved.result as ApiResult.Success).data.name)
            assertTrue(saved.cached)
            assertTrue(saved.refreshing)
            assertEquals(ErrorKind.CONNECTIVITY, (awaitItem().result as ApiResult.Failure).kind)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun reopeningWithNoInternetUsesPersistentStoreWithoutCallingServer() = runTest {
        val f = Fixture(false)
        f.seed()
        val reopened = OfflineReader(f.store, object : NetworkMonitor { override val isOnline = f.connected }, ApiCallExecutor(ApiErrorParser(com.project.solaria_mobile.core.network.ApiJson.instance)), CacheScope { f.account }, { 300L })
        val result = reopened.read("projects", String.serializer()) { error("Must not use network") }
        assertEquals("saved", (result as ApiResult.Success).data)
    }

    @Test fun serverFailureKeepsContentAndReportsRefreshFailure() = runTest {
        val f = Fixture()
        f.seed()
        f.reader.observe("projects", String.serializer()) { throw HttpException(Response.error<String>(503, "{}".toResponseBody())) }.test {
            awaitItem()
            val retained = awaitItem()
            assertEquals("saved", (retained.result as ApiResult.Success).data)
            assertEquals(ErrorKind.SERVER, retained.refreshError)
            assertFalse(retained.refreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun accessDeniedDoesNotFallBackToCache() = runTest {
        val f = Fixture()
        f.seed()
        val result = f.reader.read("projects", String.serializer()) { throw HttpException(Response.error<String>(403, "{}".toResponseBody())) }
        assertEquals(ErrorKind.FORBIDDEN, (result as ApiResult.Failure).kind)
        assertTrue(f.store.entries.isEmpty())
    }

    @Test fun reconnectUpdatesWithoutRestartingTheScreen() = runTest {
        val f = Fixture(false)
        f.seed()
        var calls = 0
        f.reader.observe("projects", String.serializer()) { calls++; "fresh" }.test {
            assertTrue(awaitItem().cached)
            assertEquals(0, calls)
            f.connected.value = true
            assertTrue(awaitItem().refreshing)
            assertFalse(awaitItem().cached)
            assertEquals(1, calls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun anotherAccountCannotReadThePreviousAccountsData() = runTest {
        val f = Fixture(false)
        f.seed()
        f.account = "qa:bob"
        val result = f.reader.read("projects", String.serializer()) { error("offline") }
        assertEquals(ErrorKind.CONNECTIVITY, (result as ApiResult.Failure).kind)
    }

    @Test fun clearingCacheDuringARequestPreventsLateRepopulation() = runTest {
        val f = Fixture()
        val response = CompletableDeferred<String>()
        f.reader.observe("projects", String.serializer()) { response.await() }.test {
            testScheduler.runCurrent()
            f.reader.clear()
            response.complete("late")
            testScheduler.runCurrent()
            assertTrue(f.store.entries.isEmpty())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun refreshFailureRetainsTheCurrentScreenContent() = runTest {
        val state = MutableStateFlow<UiState<String>>(UiState.Content("saved", syncedAt = 100L))
        state.load { ApiResult.Failure(ErrorKind.SERVER) }
        val result = state.value as UiState.Content
        assertEquals("saved", result.data)
        assertEquals(ErrorKind.SERVER, result.refreshError)
        assertFalse(result.refreshing)
    }

    @Test fun cacheIsNotSharedBetweenEnvironments() = runTest {
        val f = Fixture(false)
        f.seed()
        f.account = "prod:alice"
        val result = f.reader.read("projects", String.serializer()) { error("offline") }
        assertTrue(result is ApiResult.Failure)
    }
    @Test fun changingAccountDuringARequestDoesNotSaveOrEmitItsResponse() = runTest {
        val f = Fixture()
        val response = CompletableDeferred<String>()
        f.reader.observe("projects", String.serializer()) { response.await() }.test {
            testScheduler.runCurrent()
            f.account = "qa:bob"
            response.complete("alice data")
            testScheduler.runCurrent()
            assertTrue(f.store.entries.isEmpty())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

}
