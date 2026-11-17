package com.kamleonapp

import com.dynatech2012.kamleonuserapp.viewmodels.PENDING_SCAN_MAX_AGE_MS
import com.dynatech2012.kamleonuserapp.viewmodels.pendingScanFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingScanTest {
    @Test fun freshScanIsDelivered() {
        assertEquals("unit" to "session", pendingScanFor("unit", "session", savedAt = 1_000, now = 61_000))
    }

    @Test fun scanAtTheLimitIsStillDelivered() {
        assertEquals(
            "unit" to "session",
            pendingScanFor("unit", "session", savedAt = 0, now = PENDING_SCAN_MAX_AGE_MS)
        )
    }

    @Test fun expiredScanIsDropped() {
        assertNull(pendingScanFor("unit", "session", savedAt = 0, now = PENDING_SCAN_MAX_AGE_MS + 1))
    }

    @Test fun scanWithoutTimeIsDropped() {
        assertNull(pendingScanFor("unit", "session", savedAt = null, now = 1_000))
    }

    @Test fun clockGoingBackwardsDropsTheScan() {
        // elapsedRealtime restarts on reboot; a saved time in the future cannot be trusted.
        assertNull(pendingScanFor("unit", "session", savedAt = 5_000, now = 1_000))
    }

    @Test fun missingIdsAreDropped() {
        assertNull(pendingScanFor(null, "session", savedAt = 0, now = 1))
        assertNull(pendingScanFor("unit", "", savedAt = 0, now = 1))
    }
}
