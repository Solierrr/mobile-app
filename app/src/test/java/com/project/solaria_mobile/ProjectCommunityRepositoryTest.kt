package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.core.offline.CacheEntry
import com.project.solaria_mobile.core.offline.CacheScope
import com.project.solaria_mobile.core.offline.CacheStore
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.communication.data.CommunityApi
import com.project.solaria_mobile.feature.communication.data.CommunityCreateRequest
import com.project.solaria_mobile.feature.communication.data.CommunityDto
import com.project.solaria_mobile.feature.communication.data.CommunityGroupDto
import com.project.solaria_mobile.feature.communication.data.CommunityMemberDto
import com.project.solaria_mobile.feature.communication.data.RemoteProjectCommunityRepository
import com.project.solaria_mobile.feature.project.data.ProjectApi
import com.project.solaria_mobile.feature.project.data.ProjectDto
import com.project.solaria_mobile.feature.project.data.ProjectMemberDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit

class ProjectCommunityRepositoryTest {
    private class Store : CacheStore {
        private val entries = mutableMapOf<String, CacheEntry>()
        override suspend fun get(cacheKey: String) = entries[cacheKey]
        override suspend fun put(cacheKey: String, json: String, syncedAt: Long) { entries[cacheKey] = CacheEntry(cacheKey, json, syncedAt) }
        override suspend fun remove(cacheKey: String) { entries.remove(cacheKey) }
        override suspend fun removePrefix(prefix: String) { entries.keys.removeAll { it.startsWith(prefix) } }
        override suspend fun clear() { entries.clear() }
    }

    private class Fixture {
        val api = mockk<CommunityApi>()
        val projects = mockk<ProjectApi>()
        val networkState = MutableStateFlow(true)
        val network = object : NetworkMonitor { override val isOnline = networkState }
        val mode = mockk<MockModeStore> {
            every { isEnabled() } returns false
            every { enabled } returns MutableStateFlow(false)
        }
        val executor = ApiCallExecutor(ApiErrorParser(ApiJson.instance))
        val reader = OfflineReader(Store(), network, executor, CacheScope { "account" }, { 1L }, mode)
        val token = "header." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"auth-me"}""".toByteArray()) + ".signature"
        val credentials = object : CredentialProvider { override fun accessToken() = token }
        val retrofit = mockk<Retrofit> { every { create(CommunityApi::class.java) } returns api }
        val factory = mockk<RetrofitFactory> { every { create(any(), any()) } returns retrofit }
        val repository = RemoteProjectCommunityRepository(factory, projects, credentials, executor, reader, network, mode)

        init {
            coEvery { projects.project("p1") } returns ProjectDto("p1", "Projeto", status = "OPEN", role = "OWNER")
            coEvery { projects.members("p1") } returns listOf(
                ProjectMemberDto("m1", "core-me", "Eu", authId = "auth-me"),
                ProjectMemberDto("m2", "core-ana", "Ana", authId = "auth-ana"),
            )
            coEvery { api.community("p1") } throws HttpException(Response.error<Any>(404, "{}".toResponseBody()))
        }
    }

    @Test fun missingCommunityIsAnExplicitStateAndReadingDoesNotCreateIt() = runTest {
        val f = Fixture()
        val community = (f.repository.read("p1").result as ApiResult.Success).data
        assertEquals(null, community.id)
        assertTrue(community.canCreate)
        coVerify(exactly = 0) { f.api.create(any()) }
    }

    @Test fun forbiddenCommunityIsNotPresentedAsMissing() = runTest {
        val f = Fixture()
        coEvery { f.api.community("p1") } throws HttpException(Response.error<Any>(403, "{}".toResponseBody()))
        assertEquals(ApiResult.Failure(ErrorKind.FORBIDDEN), f.repository.read("p1").result)
    }

    @Test fun metadataDoesNotMakeAdministratorParticipantOfOtherGroups() = runTest {
        val f = Fixture()
        coEvery { f.api.community("p1") } returns CommunityDto("community", "p1", listOf(CommunityMemberDto("auth-me", "OWNER")), "ACTIVE")
        coEvery { f.api.groups("community") } returns listOf(
            CommunityGroupDto("visible", "Geral", setOf("auth-me", "auth-ana")),
            CommunityGroupDto("other", "Outro grupo", setOf("auth-ana")),
        )
        val community = (f.repository.read("p1").result as ApiResult.Success).data
        assertTrue(community.canManage)
        assertTrue(community.groups.first().canOpen)
        assertFalse(community.groups.last().canOpen)
    }

    @Test fun groupIsActiveOnlyWhileTheCommunityIs() = runTest {
        val f = Fixture()
        coEvery { f.api.groups("community") } returns listOf(CommunityGroupDto("g", "Geral", setOf("auth-me")))
        for ((status, active) in listOf("ACTIVE" to true, "ARCHIVED" to false)) {
            coEvery { f.api.community("p1") } returns CommunityDto("community", "p1", listOf(CommunityMemberDto("auth-me", "OWNER")), status)
            val community = (f.repository.read("p1").result as ApiResult.Success).data
            assertEquals(active, community.groups.single().active)
        }
    }

    @Test fun creatingCommunityUsesAuthIdsFromAuthorizedCoreRoster() = runTest {
        val f = Fixture()
        val request = slot<CommunityCreateRequest>()
        coEvery { f.api.create(capture(request)) } returns CommunityDto("new", "p1", status = "ACTIVE")
        assertEquals(ApiResult.Success(Unit), f.repository.create("p1"))
        assertEquals(setOf("auth-me", "auth-ana"), request.captured.memberIds)
        assertFalse(request.captured.memberIds.contains("core-ana"))
    }

    @Test fun closedProjectCannotCreateACommunity() = runTest {
        val f = Fixture()
        coEvery { f.projects.project("p1") } returns ProjectDto("p1", "Projeto", status = "COMPLETED", role = "OWNER")
        assertEquals(ApiResult.Failure(ErrorKind.FORBIDDEN), f.repository.create("p1"))
        coVerify(exactly = 0) { f.api.create(any()) }
    }

    @Test fun offlineMutationDoesNotReachMessenger() = runTest {
        val f = Fixture()
        f.networkState.value = false
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), f.repository.createGroup("community", "Geral"))
        coVerify(exactly = 0) { f.api.createGroup(any(), any()) }
    }
}
