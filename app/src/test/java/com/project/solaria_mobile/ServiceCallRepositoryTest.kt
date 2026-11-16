package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.core.offline.CacheIdentity
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.communication.data.RemoteServiceCallRepository
import com.project.solaria_mobile.feature.communication.data.ServiceCallApi
import com.project.solaria_mobile.feature.communication.data.ServiceCallDto
import com.project.solaria_mobile.feature.communication.domain.ServiceDecision
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit

class ServiceCallRepositoryTest {
    private class Fixture {
        var account: String? = "first-account"
        var simulated = false
        val online = MutableStateFlow(true)
        var connectivity: Flow<Boolean> = online
        val api = mockk<ServiceCallApi>()
        val reader = mockk<OfflineReader>()
        val identity = mockk<CacheIdentity> { every { current() } answers { account } }
        val mode = mockk<MockModeStore> { every { isEnabled() } answers { simulated } }
        val network = object : NetworkMonitor { override val isOnline get() = connectivity }
        val retrofit = mockk<Retrofit> { every { create(ServiceCallApi::class.java) } returns api }
        val factory = mockk<RetrofitFactory> { every { create(any(), any()) } returns retrofit }
        val repository = RemoteServiceCallRepository(factory, ApiCallExecutor(ApiErrorParser(ApiJson.instance)), reader, network, mode, identity)
        init { coEvery { reader.invalidatePrefix(any()) } returns Unit }
    }

    @Test fun accountChangeDuringWriteDoesNotReturnPrivateDataOrInvalidateNextAccountCache() = runTest {
        val f = Fixture()
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        coEvery { f.api.accept("s") } coAnswers {
            started.complete(Unit); finish.await()
            ServiceCallDto("s", "private-project", "Primeira conta", "Revisão", "IN_PROGRESS")
        }
        val result = async { f.repository.change("s", ServiceDecision.ACCEPT) }
        started.await()
        f.account = "second-account"
        finish.complete(Unit)
        assertEquals(ApiResult.Failure(ErrorKind.CONFLICT), result.await())
        coVerify(exactly = 0) { f.reader.invalidatePrefix(any()) }
    }

    @Test fun mockModeChangeDuringWriteAlsoRejectsTheOldResponse() = runTest {
        val f = Fixture()
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        coEvery { f.api.accept("s") } coAnswers {
            started.complete(Unit); finish.await()
            ServiceCallDto("s", "project", "Projeto", "Revisão", "IN_PROGRESS")
        }
        val result = async { f.repository.change("s", ServiceDecision.ACCEPT) }
        started.await()
        f.simulated = true
        finish.complete(Unit)
        assertEquals(ApiResult.Failure(ErrorKind.CONFLICT), result.await())
    }

    @Test fun offlineMutationIsBlockedWithoutCallingCore() = runTest {
        val f = Fixture()
        f.online.value = false
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), f.repository.change("s", ServiceDecision.ACCEPT))
        coVerify(exactly = 0) { f.api.accept(any()) }
    }

    @Test fun successfulWriteInvalidatesServiceListAndDetail() = runTest {
        val f = Fixture()
        coEvery { f.api.complete("s") } returns ServiceCallDto("s", "project", "Projeto", "Revisão", "COMPLETED")
        val result = f.repository.change("s", ServiceDecision.COMPLETE) as ApiResult.Success
        assertEquals("COMPLETED", result.data.status)
        coVerify(exactly = 1) { f.reader.invalidatePrefix("services:") }
    }

    @Test fun accountChangeWhileWaitingConnectivityPreventsSendingMutation() = runTest {
        val f = Fixture()
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        f.connectivity = flow { started.complete(Unit); finish.await(); emit(true) }
        val result = async { f.repository.change("s", ServiceDecision.ACCEPT) }
        started.await()
        f.account = "second-account"
        finish.complete(Unit)
        assertEquals(ApiResult.Failure(ErrorKind.CONFLICT), result.await())
        coVerify(exactly = 0) { f.api.accept(any()) }
    }
}
