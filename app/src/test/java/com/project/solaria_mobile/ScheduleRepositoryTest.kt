package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.offline.CacheEntry
import com.project.solaria_mobile.core.offline.CacheScope
import com.project.solaria_mobile.core.offline.CacheStore
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.discovery.data.RemoteDiscoveryRepository
import com.project.solaria_mobile.feature.project.data.ProjectActivityDto
import com.project.solaria_mobile.feature.project.data.ProjectApi
import com.project.solaria_mobile.feature.project.data.ProjectDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

class ScheduleRepositoryTest {
    private class MemoryStore : CacheStore {
        val entries = mutableMapOf<String, CacheEntry>()
        override suspend fun get(cacheKey: String) = entries[cacheKey]
        override suspend fun put(cacheKey: String, json: String, syncedAt: Long) { entries[cacheKey] = CacheEntry(cacheKey, json, syncedAt) }
        override suspend fun clear() { entries.clear() }
        override suspend fun remove(cacheKey: String) { entries.remove(cacheKey) }
        override suspend fun removePrefix(prefix: String) { entries.keys.removeAll { it.startsWith(prefix) } }
    }

    private class Fixture {
        val store = MemoryStore()
        val online = MutableStateFlow(true)
        var account = "qa:alice"
        val api = mockk<ProjectApi>()
        val reader = OfflineReader(store, object : NetworkMonitor { override val isOnline = online }, ApiCallExecutor(ApiErrorParser(ApiJson.instance)), CacheScope { account }, { 200L })
        val repository = RemoteDiscoveryRepository(api, reader)
        val activity = ProjectActivityDto("a1", title = "Visita", startsAt = "2026-10-26T12:00:00Z")

        init {
            coEvery { api.projects() } returns listOf(ProjectDto("p1", "Obra"))
            coEvery { api.activities("p1") } returns listOf(activity)
        }
    }

    @Test fun offlineReadSurvivesNewRepositoryInstanceAndIsIsolatedByAccountAndEnvironment() = runTest {
        val f = Fixture()
        val fresh = f.repository.events() as ApiResult.Success
        f.online.value = false
        val restarted = RemoteDiscoveryRepository(f.api, f.reader)
        assertEquals(fresh, restarted.events())
        coVerify(exactly = 1) { f.api.projects() }
        coVerify(exactly = 1) { f.api.activities("p1") }
        for (account in listOf("qa:bob", "prod:alice")) {
            f.account = account
            assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), restarted.events())
        }
    }

    @Test fun partialFailureRetainsCompleteCacheWithoutSavingPartialCalendar() = runTest {
        val f = Fixture()
        val original = (f.repository.events() as ApiResult.Success).data
        coEvery { f.api.projects() } returns listOf(ProjectDto("p1", "Obra"), ProjectDto("p2", "Outra obra"))
        coEvery { f.api.activities("p1") } returns emptyList()
        coEvery { f.api.activities("p2") } throws IOException()
        f.repository.observeEvents().test {
            assertTrue(awaitItem().refreshing)
            val result = awaitItem()
            assertEquals(original, (result.result as ApiResult.Success).data)
            assertEquals(ErrorKind.CONNECTIVITY, result.refreshError)
            assertTrue(result.cached)
            cancelAndIgnoreRemainingEvents()
        }
        f.online.value = false
        assertEquals(original, (f.repository.events() as ApiResult.Success).data)
    }

    @Test fun uncachedPartialFailureIsAnErrorRatherThanAnEmptyCalendar() = runTest {
        val f = Fixture()
        coEvery { f.api.activities("p1") } throws IOException()
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), f.repository.events())
        assertTrue(f.store.entries.isEmpty())
    }

    @Test fun emptyProjectListIsAValidEmptyCalendar() = runTest {
        val f = Fixture()
        coEvery { f.api.projects() } returns emptyList()
        assertEquals(ApiResult.Success(emptyList<Any>()), f.repository.events())
        coVerify(exactly = 0) { f.api.activities(any()) }
    }

    @Test fun connectionRecoveryReplacesSavedEventsWithServerTruth() = runTest {
        val f = Fixture()
        f.repository.events()
        f.online.value = false
        f.repository.observeEvents().test {
            assertTrue(awaitItem().cached)
            coEvery { f.api.activities("p1") } returns emptyList()
            f.online.value = true
            assertTrue(awaitItem().refreshing)
            val updated = awaitItem()
            assertEquals(ApiResult.Success(emptyList<Any>()), updated.result)
            assertFalse(updated.cached)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun permissionRevocationClearsPreviouslySavedSchedule() = runTest {
        val f = Fixture()
        f.repository.events()
        coEvery { f.api.activities("p1") } throws HttpException(Response.error<Any>(403, "{}".toResponseBody()))
        assertEquals(ApiResult.Failure(ErrorKind.FORBIDDEN), f.repository.events())
        f.online.value = false
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), f.repository.events())
    }

    @Test fun projectRequestsAreBoundedAndDuplicateProjectsAreNotReadTwice() = runTest {
        val f = Fixture()
        val projects = (1..9).map { ProjectDto("p$it", "Obra $it") }
        var active = 0
        var maximum = 0
        coEvery { f.api.projects() } returns projects + projects.first()
        coEvery { f.api.activities(any()) } coAnswers {
            active++
            maximum = maxOf(maximum, active)
            delay(100)
            active--
            listOf(f.activity)
        }
        val result = f.repository.events() as ApiResult.Success
        assertEquals(9, result.data.size)
        assertEquals(4, maximum)
        coVerify(exactly = 1) { f.api.activities("p1") }
    }
}
