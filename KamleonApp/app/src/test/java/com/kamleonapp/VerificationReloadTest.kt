package com.kamleonapp

import com.dynatech2012.kamleonuserapp.viewmodels.verifiedAfterReload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether the email counts as verified once the signed-in user is reloaded from the server. The time
 * limit and the errors belong to the network layer (safeCall): reload answers null when it has no answer.
 */
class VerificationReloadTest {
    @Test fun anAlreadyVerifiedEmailIsNotReloaded() = runBlocking {
        var reloads = 0
        assertTrue(verifiedAfterReload(cachedVerified = true) { reloads++; false })
        assertEquals(0, reloads)
    }

    @Test fun theServerAnswerWinsOverTheStaleCache() = runBlocking {
        // Verified from the link in the email: only the reload can tell.
        assertTrue(verifiedAfterReload(cachedVerified = false) { true })
        assertFalse(verifiedAfterReload(cachedVerified = false) { false })
    }

    @Test fun noAnswerCountsAsNotVerified() = runBlocking {
        assertFalse(verifiedAfterReload(cachedVerified = false) { null })
    }
}
