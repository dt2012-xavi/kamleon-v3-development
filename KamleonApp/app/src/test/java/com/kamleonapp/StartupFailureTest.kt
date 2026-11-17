package com.kamleonapp

import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel.StartupState
import com.dynatech2012.kamleonuserapp.viewmodels.startupStateFor
import java.io.IOException
import java.util.concurrent.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the startup check does when the profile cannot be read: retry, or log in again. Firebase's own
 * exceptions need real Android classes to be built, so here the "server refused the user" test is a
 * stand-in; the real classification is covered by the instrumented SessionRejectedTest.
 */
class StartupFailureTest {
    private class Refused : Exception("refused")

    private fun stateAfter(failure: Exception, authenticated: Boolean = true, verified: Boolean = true) =
        startupStateFor(
            authenticated = authenticated,
            verified = verified,
            profile = Response.Failure(failure),
            sessionRejected = { it is Refused }
        )

    @Test fun aRefusedUserGoesToLoginInsteadOfRetryingForEver() {
        assertEquals(StartupState.NeedsLogin, stateAfter(Refused()))
    }

    @Test fun anythingElseIsRetryable() {
        assertEquals(StartupState.RetryableError, stateAfter(TimeoutException("slow")))
        assertEquals(StartupState.RetryableError, stateAfter(IOException("offline")))
        assertEquals(StartupState.RetryableError, stateAfter(Exception("unknown")))
    }

    @Test fun aMissingProfileIsStillRecoveredNotTreatedAsRefusal() {
        assertEquals(StartupState.NeedsProfile, stateAfter(UserProfileMissingException()))
    }

    @Test fun signedOutStaysOnLogin() {
        assertEquals(StartupState.NeedsLogin, stateAfter(Refused(), authenticated = false))
        assertEquals(StartupState.NeedsLogin, startupStateFor(false, false, null))
    }

    @Test fun theDefaultClassifierTreatsPlainErrorsAsRetryable() {
        // No Firebase classes involved: these are what a bad network produces.
        listOf(TimeoutException("slow"), IOException("offline"), Exception("unknown")).forEach {
            assertEquals(
                StartupState.RetryableError,
                startupStateFor(authenticated = true, verified = true, profile = Response.Failure(it))
            )
        }
    }
}
