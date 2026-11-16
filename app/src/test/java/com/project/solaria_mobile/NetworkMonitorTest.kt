package com.project.solaria_mobile

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.cash.turbine.test
import com.project.solaria_mobile.core.network.ConnectivityManagerNetworkMonitor
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NetworkMonitorTest {
    private val manager = mockk<ConnectivityManager>()
    private val context = mockk<Context>()
    private val callback = slot<ConnectivityManager.NetworkCallback>()
    private val network = mockk<Network>()

    private fun monitor(initialOnline: Boolean): ConnectivityManagerNetworkMonitor {
        every { context.getSystemService(ConnectivityManager::class.java) } returns manager
        every { manager.activeNetwork } returns if (initialOnline) network else null
        every { manager.getNetworkCapabilities(network) } returns capabilities(true)
        every { manager.registerDefaultNetworkCallback(capture(callback)) } just Runs
        every { manager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs
        return ConnectivityManagerNetworkMonitor(context)
    }

    private fun capabilities(validated: Boolean) = mockk<NetworkCapabilities> {
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns validated
    }

    @Test fun lostDefaultNetworkIsOfflineEvenWhenSynchronousSnapshotIsStale() = runTest {
        monitor(true).isOnline.test {
            assertTrue(awaitItem())
            callback.captured.onLost(network)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun restoredNetworkUsesCapabilitiesDeliveredByCallback() = runTest {
        monitor(false).isOnline.test {
            assertFalse(awaitItem())
            callback.captured.onCapabilitiesChanged(network, capabilities(true))
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun networkWithoutValidatedInternetIsOffline() = runTest {
        monitor(true).isOnline.test {
            assertTrue(awaitItem())
            callback.captured.onCapabilitiesChanged(network, capabilities(false))
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
