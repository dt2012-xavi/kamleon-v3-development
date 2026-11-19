package com.kamleonapp

import com.dynatech2012.kamleonuserapp.network.NetworkStatus
import com.dynatech2012.kamleonuserapp.network.awaitOnline
import com.dynatech2012.kamleonuserapp.network.networkStatusFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorTest {
    @Test fun offlineMeansNoNetworkWithInternetAtAll() {
        // A network the system has not validated (captive portal, a network that blocks the check) can
        // still reach Firebase, so it must not count as offline.
        assertEquals(NetworkStatus.Online, networkStatusFor(hasInternet = true))
        assertEquals(NetworkStatus.Offline, networkStatusFor(hasInternet = false))
    }

    @Test fun isOnlineFollowsTheStatus() {
        val monitor = FakeNetworkMonitor(NetworkStatus.Offline)
        assertFalse(monitor.isOnline)
        monitor.set(NetworkStatus.Online)
        assertTrue(monitor.isOnline)
    }

    @Test fun awaitOnlineReturnsAtOnceWhenOnline() = runTest {
        FakeNetworkMonitor(NetworkStatus.Online).awaitOnline()
    }

    @Test fun awaitOnlineWaitsForTheNetwork() = runTest {
        val monitor = FakeNetworkMonitor(NetworkStatus.Offline)
        var done = false
        val job = launch {
            monitor.awaitOnline()
            done = true
        }
        advanceTimeBy(1_000)
        runCurrent()
        assertFalse(done)
        monitor.set(NetworkStatus.Online)
        runCurrent()
        assertTrue(done)
        job.join()
    }
}
