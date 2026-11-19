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
    @Test fun onlineNeedsInternetAndAValidatedNetwork() {
        assertEquals(NetworkStatus.Online, networkStatusFor(hasInternet = true, validated = true))
        assertEquals(NetworkStatus.Offline, networkStatusFor(hasInternet = true, validated = false))
        assertEquals(NetworkStatus.Offline, networkStatusFor(hasInternet = false, validated = true))
        assertEquals(NetworkStatus.Offline, networkStatusFor(hasInternet = false, validated = false))
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
