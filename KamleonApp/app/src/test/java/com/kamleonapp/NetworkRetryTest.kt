package com.kamleonapp

import com.dynatech2012.kamleonuserapp.network.NetworkStatus
import com.dynatech2012.kamleonuserapp.network.retryWhenOnline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkRetryTest {
    @Test fun doesNothingWhileOfflineAndRunsOnceWhenTheNetworkReturns() = runTest {
        val monitor = FakeNetworkMonitor(NetworkStatus.Offline)
        var runs = 0
        val job = launch { monitor.retryWhenOnline(pauseMs = 600) { runs++ } }
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(0, runs)
        monitor.set(NetworkStatus.Online)
        advanceTimeBy(700)
        runCurrent()
        assertEquals(1, runs)
        job.join()
    }

    @Test fun aNetworkThatDropsAgainDuringThePauseIsWaitedForAgain() = runTest {
        // Flapping connection: the retry must not fire into a network that is gone again.
        val monitor = FakeNetworkMonitor(NetworkStatus.Offline)
        var runs = 0
        val job = launch { monitor.retryWhenOnline(pauseMs = 600) { runs++ } }
        monitor.set(NetworkStatus.Online)
        advanceTimeBy(300)
        monitor.set(NetworkStatus.Offline)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(0, runs)
        monitor.set(NetworkStatus.Online)
        advanceTimeBy(700)
        runCurrent()
        assertEquals(1, runs)
        job.join()
    }

    @Test fun rapidFlappingStillRunsOnlyOnce() = runTest {
        val monitor = FakeNetworkMonitor(NetworkStatus.Offline)
        var runs = 0
        val job = launch { monitor.retryWhenOnline(pauseMs = 600) { runs++ } }
        repeat(3) {
            monitor.set(NetworkStatus.Online)
            runCurrent()
            monitor.set(NetworkStatus.Offline)
            runCurrent()
        }
        monitor.set(NetworkStatus.Online)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(1, runs)
        job.join()
    }
}
