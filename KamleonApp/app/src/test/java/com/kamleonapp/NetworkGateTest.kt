package com.kamleonapp

import com.dynatech2012.kamleonuserapp.network.CallKind
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.dynatech2012.kamleonuserapp.network.NetworkStatus
import com.dynatech2012.kamleonuserapp.network.NetworkTimeoutException
import com.dynatech2012.kamleonuserapp.network.NetworkUnavailableException
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.ResponseNullable
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkGateTest {
    private val monitor = FakeNetworkMonitor()
    private val gate = NetworkGate(monitor)

    @After fun uproot() = Timber.uprootAll()

    @Test fun defaultTimeoutsComeFromTheCallKind() {
        assertEquals(10_000L, CallKind.Read.defaultTimeoutMs)
        assertEquals(15_000L, CallKind.Action.defaultTimeoutMs)
    }

    @Test fun offlineFailsAtOnceWithoutCallingEvenForReads() = runTest {
        monitor.set(NetworkStatus.Offline)
        CallKind.entries.forEach { kind ->
            var calls = 0
            val result = gate.safeCall(kind) { calls++; 1 }
            assertTrue(kind.name, result is Response.Failure && result.exception is NetworkUnavailableException)
            assertEquals(kind.name, 0, calls)
        }
    }

    @Test fun anExplicitRetryCallsEvenWhenTheMonitorSaysOffline() = runTest {
        // A false Offline (VPN, captive portal) must never stop the user from trying for real.
        monitor.set(NetworkStatus.Offline)
        var calls = 0
        val result = gate.safeCall(CallKind.Read, trustMonitor = false) { calls++; 7 }
        assertEquals(Response.Success(7), result)
        assertEquals(1, calls)
    }

    @Test fun onlineReturnsTheValue() = runTest {
        assertEquals(Response.Success("ok"), gate.safeCall(CallKind.Action) { "ok" })
    }

    @Test fun aThrownExceptionComesBackAsAFailureOfTheSameKind() = runTest {
        // Coroutines may hand back a copy of the exception (stack trace recovery): compare type and message.
        val result = gate.safeCall<Int>(CallKind.Read) { throw IOException("x") }
        assertTrue(result is Response.Failure)
        val failure = (result as Response.Failure).exception
        assertTrue(failure is IOException)
        assertEquals("x", failure.message)
    }

    @Test fun aCallThatNeverAnswersTimesOutAndTheNextCallStillWorks() = runTest {
        val hung = gate.safeCall(CallKind.Action, timeoutMs = 50) { delay(5_000); 1 }
        assertTrue(hung is Response.Failure && hung.exception is NetworkTimeoutException)
        assertEquals(Response.Success(2), gate.safeCall(CallKind.Action, timeoutMs = 50) { 2 })
    }

    @Test fun cancellingTheCallerIsNotAFailure() = runTest {
        var result: Response<Int>? = null
        val job = launch { result = gate.safeCall(CallKind.Read) { delay(10_000); 1 } }
        runCurrent()
        job.cancelAndJoin()
        assertNull(result)
    }

    @Test fun aCancellationInsideTheCallIsRethrown() = runTest {
        try {
            gate.safeCall<Int>(CallKind.Read) { throw CancellationException("stop") }
            fail("the cancellation must not become a failure")
        } catch (expected: CancellationException) {
            assertEquals("stop", expected.message)
        }
    }

    @Test fun theLogsHoldTheKindAndTheClassButNeverTheMessage() = runTest {
        val logs = mutableListOf<String>()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                logs += message + (t?.toString() ?: "")
            }
        })
        gate.safeCall<Int>(CallKind.Action) { throw IOException("secret@mail.com") }
        val joined = logs.joinToString("\n")
        assertTrue(joined, "IOException" in joined)
        assertTrue(joined, "Action" in joined)
        assertTrue(joined, "secret@mail.com" !in joined)
    }

    @Test fun theNullableVariantFollowsTheSameRules() = runTest {
        assertEquals(ResponseNullable.Success<Nothing>(), gate.safeCallNullable(CallKind.Action) { })
        monitor.set(NetworkStatus.Offline)
        val offline = gate.safeCallNullable(CallKind.Action) { }
        assertTrue(offline is ResponseNullable.Failure && offline.exception is NetworkUnavailableException)
        monitor.set(NetworkStatus.Online)
        val failed = gate.safeCallNullable(CallKind.Action) { throw IOException("x") }
        assertTrue(failed is ResponseNullable.Failure && failed.exception is IOException)
    }
}
