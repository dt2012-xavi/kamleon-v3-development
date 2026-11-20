package com.kamleonapp

import com.dynatech2012.kamleonuserapp.viewmodels.guardedStartup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** An unexpected exception during the startup check must become a failure the screen can show, never a stuck splash. */
class StartupGuardTest {
    @Test fun aNormalResultPassesThrough() = runBlocking<Unit> {
        assertEquals(7, guardedStartup { 7 }.getOrNull())
    }

    @Test fun anUnexpectedExceptionBecomesAFailure() = runBlocking<Unit> {
        val result = guardedStartup<Int> { throw IllegalStateException("boom") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test fun cancellationIsNotSwallowed() = runBlocking<Unit> {
        // A cancelled check is a newer check taking over, not a failure to report.
        try {
            guardedStartup<Int> { throw CancellationException("newer check") }
            fail("cancellation must propagate")
        } catch (e: CancellationException) {
            assertEquals("newer check", e.message)
        }
    }
}
