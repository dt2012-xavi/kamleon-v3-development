package com.kamleonapp

import com.dynatech2012.kamleonuserapp.viewmodels.verifiedAfterReload
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Whether the email counts as verified once the signed-in user is reloaded from the server. */
class VerificationReloadTest {
    @Test fun anAlreadyVerifiedEmailIsNotReloaded() = runBlocking {
        var reloads = 0
        assertTrue(verifiedAfterReload(cachedVerified = true, timeoutMs = 1_000) { reloads++; false })
        assertEquals(0, reloads)
    }

    @Test fun theServerAnswerWinsOverTheStaleCache() = runBlocking {
        // Verified from the link in the email: only the reload can tell.
        assertTrue(verifiedAfterReload(cachedVerified = false, timeoutMs = 1_000) { true })
        assertFalse(verifiedAfterReload(cachedVerified = false, timeoutMs = 1_000) { false })
    }

    @Test fun noAnswerCountsAsNotVerified() = runBlocking {
        assertFalse(verifiedAfterReload(cachedVerified = false, timeoutMs = 1_000) { null })
        assertFalse(verifiedAfterReload(cachedVerified = false, timeoutMs = 1_000) { error("offline") })
    }

    @Test fun aSlowReloadGivesUpInsteadOfHoldingTheSplash() = runBlocking {
        assertFalse(verifiedAfterReload(cachedVerified = false, timeoutMs = 50) { delay(5_000); true })
    }
}
