package com.kamleonapp

import com.dynatech2012.kamleonuserapp.network.FailureKind
import com.dynatech2012.kamleonuserapp.network.NetworkTimeoutException
import com.dynatech2012.kamleonuserapp.network.NetworkUnavailableException
import com.dynatech2012.kamleonuserapp.network.failureKindOf
import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import java.io.IOException
import java.util.concurrent.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The one definition of what a failure means. Firebase's own exceptions cannot be built on the JVM,
 * so those are covered on the device (SessionRejectedTest); here a stand-in classifier is injected.
 */
class FailureKindTest {
    private val noFirebase: (Exception) -> FailureKind? = { null }

    @Test fun networkProblemsAreNetwork() {
        listOf(
            NetworkUnavailableException(),
            NetworkTimeoutException(1_000),
            TimeoutException("slow"),
            IOException("offline")
        ).forEach { assertEquals(it.javaClass.simpleName, FailureKind.Network, failureKindOf(it, noFirebase)) }
    }

    @Test fun aMissingProfileIsNotFound() {
        assertEquals(FailureKind.NotFound, failureKindOf(UserProfileMissingException(), noFirebase))
    }

    @Test fun anythingElseWithoutAFirebaseVerdictIsUnknown() {
        assertEquals(FailureKind.Unknown, failureKindOf(Exception("x"), noFirebase))
        assertEquals(FailureKind.Unknown, failureKindOf(IllegalStateException("unparseable"), noFirebase))
    }

    @Test fun theFirebaseVerdictIsUsedForWhatIsNotAPlainNetworkFailure() {
        assertEquals(FailureKind.Busy, failureKindOf(Exception("x")) { FailureKind.Busy })
        assertEquals(FailureKind.SessionRejected, failureKindOf(Exception("x")) { FailureKind.SessionRejected })
    }

    @Test fun aPlainNetworkFailureIsNeverOverriddenByTheFirebaseVerdict() {
        assertEquals(FailureKind.Network, failureKindOf(IOException("offline")) { FailureKind.Server })
    }
}
