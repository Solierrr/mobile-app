package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.offline.CacheEntry
import com.project.solaria_mobile.core.offline.CacheStore
import com.project.solaria_mobile.core.offline.OfflineBannerState
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.core.offline.ageLabel
import com.project.solaria_mobile.core.offline.bannerText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class FakeNetwork(online: Boolean) : NetworkMonitor {
    val state = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = state
}

class MemoryStore : CacheStore {
    val entries = mutableMapOf<String, CacheEntry>()
    override suspend fun get(cacheKey: String): CacheEntry? = entries[cacheKey]
    override suspend fun put(cacheKey: String, json: String, syncedAt: Long) {
        entries[cacheKey] = CacheEntry(cacheKey, json, syncedAt)
    }
    override suspend fun clear() = entries.clear()
}

class OfflineTest {
    private val store = MemoryStore()
    private val network = FakeNetwork(online = true)
    private var now = 1_000L
    private val serializer = ListSerializer(String.serializer())

    private fun reader(limit: Long = 10_000) =
        OfflineReader(store, network, ApiCallExecutor(ApiErrorParser(ApiJson.instance)), { now }, limit)

    @Test fun anOnlineReadIsSavedForLater() = runTest {
        val reader = reader()
        val result = reader.read("projects", serializer) { listOf("a", "b") }
        assertEquals(ApiResult.Success(listOf("a", "b")), result)
        assertEquals(1_000L, store.entries.getValue("projects").syncedAt)
        assertFalse(reader.status.value.stale)
        assertEquals(1_000L, reader.status.value.lastSyncedAt)
    }

    @Test fun withoutInternetTheSavedReadIsReturnedWithoutCallingTheServer() = runTest {
        val reader = reader()
        reader.read("projects", serializer) { listOf("a") }
        network.state.value = false
        now = 5_000L
        var called = false
        val result = reader.read("projects", serializer) { called = true; listOf("novo") }
        assertEquals(ApiResult.Success(listOf("a")), result)
        assertFalse(called)
        assertTrue(reader.status.value.stale)
        assertEquals(1_000L, reader.status.value.lastSyncedAt)
    }

    @Test fun withoutInternetAndNothingSavedItFailsAsConnectivity() = runTest {
        network.state.value = false
        val result = reader().read("projects", serializer) { listOf("a") }
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), result)
    }

    @Test fun aNetworkFailureFallsBackToTheSavedRead() = runTest {
        val reader = reader()
        reader.read("projects", serializer) { listOf("a") }
        val result = reader.read("projects", serializer) { throw IOException("sem rede") }
        assertEquals(ApiResult.Success(listOf("a")), result)
        assertTrue(reader.status.value.stale)
    }

    @Test fun aNetworkFailureWithNothingSavedStaysAFailure() = runTest {
        val result = reader().read("projects", serializer) { throw IOException("sem rede") }
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), result)
    }

    @Test fun aSlowServerFallsBackToTheSavedRead() = runTest {
        val reader = reader(limit = 50)
        reader.read("projects", serializer) { listOf("a") }
        val result = reader.read("projects", serializer) { delay(10_000); listOf("lento") }
        assertEquals(ApiResult.Success(listOf("a")), result)
    }

    @Test fun serverErrorsAreNeverHiddenByTheSavedRead() = runTest {
        val reader = reader()
        reader.read("projects", serializer) { listOf("a") }
        val result = reader.read("projects", serializer) { throw retrofit2.HttpException(forbidden()) }
        assertEquals(ApiResult.Failure(ErrorKind.FORBIDDEN), result)
        assertFalse(reader.status.value.stale)
    }

    @Test fun clearingRemovesEverythingSaved() = runTest {
        val reader = reader()
        reader.read("projects", serializer) { listOf("a") }
        reader.clear()
        assertNull(store.get("projects"))
        assertEquals(null, reader.status.value.lastSyncedAt)
        network.state.value = false
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), reader.read("projects", serializer) { listOf("a") })
    }

    @Test fun savedReadsAreKeptPerKey() = runTest {
        val reader = reader()
        reader.read("project:1", serializer) { listOf("um") }
        reader.read("project:2", serializer) { listOf("dois") }
        network.state.value = false
        assertEquals(ApiResult.Success(listOf("dois")), reader.read("project:2", serializer) { emptyList() })
        assertEquals(ApiResult.Success(listOf("um")), reader.read("project:1", serializer) { emptyList() })
    }

    @Test fun theAgeIsShownInPlainWords() {
        assertEquals("agora", ageLabel(0, 30_000))
        assertEquals("há 5 min", ageLabel(0, 5 * 60_000L))
        assertEquals("há 3 h", ageLabel(0, 3 * 3_600_000L))
        assertEquals("há 1 dia", ageLabel(0, 30 * 3_600_000L))
        assertEquals("há 4 dias", ageLabel(0, 4 * 86_400_000L))
        assertEquals("agora", ageLabel(10, 0))
    }

    @Test fun theBannerSaysWhyAndHowOldTheDataIs() {
        val offline = OfflineBannerState(offline = true, stale = true, lastSyncedAt = 0)
        assertTrue(offline.visible)
        assertEquals("Sem internet. Mostrando os dados guardados. Atualizado há 5 min.", bannerText(offline, 5 * 60_000L))
        val slowServer = OfflineBannerState(offline = false, stale = true, lastSyncedAt = null)
        assertEquals("Mostrando os dados guardados.", bannerText(slowServer, 0))
        assertFalse(OfflineBannerState().visible)
    }

    private fun forbidden() = retrofit2.Response.error<Any>(
        403,
        "{}".toResponseBody(null),
    )
}
