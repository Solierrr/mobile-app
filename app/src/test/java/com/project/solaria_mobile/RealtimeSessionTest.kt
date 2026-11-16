package com.project.solaria_mobile

import androidx.lifecycle.LifecycleOwner
import com.project.solaria_mobile.core.auth.AuthTokens
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeLifecycleBinder
import com.project.solaria_mobile.feature.communication.domain.ChatPresence
import com.project.solaria_mobile.feature.communication.label
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeSessionTest {
    private val session = MutableStateFlow<AuthTokens?>(null)
    private val mock = MutableStateFlow(false)
    private val store = mockk<TokenStore> { every { tokens } returns session; every { currentTokens() } answers { session.value } }
    private val mode = mockk<MockModeStore> {
        every { enabled } returns mock
        every { isEnabled() } answers { mock.value }
    }
    private val client = mockk<RealtimeClient>(relaxed = true)
    private val owner = mockk<LifecycleOwner>()

    @Test fun loginRefreshAndAccountSwitchReconnectWithoutRecreatingActivity() = runTest {
        val binder = RealtimeLifecycleBinder(client, mode, store, mockk(relaxed = true), backgroundScope)
        binder.startWatching()
        binder.onStart(owner)
        runCurrent()
        verify(exactly = 0) { client.connect() }
        session.value = AuthTokens("account-a-token-1", "refresh")
        runCurrent()
        verify(exactly = 1) { client.connect() }
        session.value = AuthTokens("account-a-token-2", "refresh")
        runCurrent()
        session.value = AuthTokens("account-b-token", "refresh")
        runCurrent()
        verify(exactly = 3) { client.connect() }
        session.value = null
        runCurrent()
        verify(exactly = 3) { client.connect() }
        verify(atLeast = 4) { client.disconnect() }
    }

    @Test fun mockAndBackgroundStopRealtimeAndReturningStartsCurrentSession() = runTest {
        session.value = AuthTokens("account-token", "refresh")
        val binder = RealtimeLifecycleBinder(client, mode, store, mockk(relaxed = true), backgroundScope)
        binder.startWatching()
        binder.onStart(owner)
        runCurrent()
        mock.value = true
        runCurrent()
        verify(exactly = 1) { client.connect() }
        mock.value = false
        runCurrent()
        verify(exactly = 2) { client.connect() }
        binder.onStop(owner)
        runCurrent()
        session.value = AuthTokens("new-token-in-background", "refresh")
        runCurrent()
        verify(exactly = 2) { client.connect() }
        binder.onStart(owner)
        runCurrent()
        verify(exactly = 3) { client.connect() }
    }

    @Test fun offlineRequiresKnownSnapshotAndDoesNotReplaceTyping() {
        assertNull(ChatPresence(peerOnline = false).label())
        assertEquals("Offline", ChatPresence(known = true, peerOnline = false).label())
        assertEquals("Online", ChatPresence(known = true, peerOnline = true).label())
        assertEquals("Ana digitando…", ChatPresence(known = true, peerOnline = true, typingNames = listOf("Ana")).label())
    }

    @Test fun temporaryRefreshFailureIsRetriedWhileTheSameSessionIsForeground() = runTest {
        val current = AuthTokens("expired", "refresh", java.time.Instant.ofEpochMilli(10_000).toString())
        val renewed = AuthTokens("renewed", "next-refresh")
        session.value = current
        val authenticator = mockk<com.project.solaria_mobile.core.auth.TokenAuthenticator>()
        var attempts = 0
        every { authenticator.renewIfCurrent(current) } answers {
            attempts++
            if (attempts == 1) null else { session.value = renewed; renewed }
        }
        val binder = RealtimeLifecycleBinder(client, mode, store, authenticator, backgroundScope, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler), { 0L })
        binder.startWatching()
        binder.onStart(owner)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, attempts)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, attempts)
        verify(exactly = 2) { client.connect() }
        binder.onStop(owner)
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(2, attempts)
    }

}
