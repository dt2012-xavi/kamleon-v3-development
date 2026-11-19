package com.kamleonapp

import com.dynatech2012.kamleonuserapp.network.NetworkStatus
import com.dynatech2012.kamleonuserapp.network.awaitOnline
import com.dynatech2012.kamleonuserapp.network.awaitOnlineFor
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

    @Test fun awaitOnlineForGivesUpAfterTheTimeout() = runTest {
        // A network that was just connected may not be validated yet: wait a moment, but not for ever.
        assertFalse(FakeNetworkMonitor(NetworkStatus.Offline).awaitOnlineFor(1_500))
        assertTrue(FakeNetworkMonitor(NetworkStatus.Online).awaitOnlineFor(1_500))
    }

    @Test fun awaitOnlineForReturnsTrueWhenTheNetworkComesBackInTime() = runTest {
        val monitor = FakeNetworkMonitor(NetworkStatus.Offline)
        var result: Boolean? = null
        launch { result = monitor.awaitOnlineFor(1_500) }
        advanceTimeBy(500)
        monitor.set(NetworkStatus.Online)
        runCurrent()
        assertEquals(true, result)
    }
}
