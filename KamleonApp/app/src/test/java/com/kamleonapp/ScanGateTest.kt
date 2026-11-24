package com.kamleonapp

import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.viewmodels.ScanGate
import com.dynatech2012.kamleonuserapp.viewmodels.ScanGate.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ScanGateTest {
    private val link = "https://dev.kamleon.app/scan?unitId=1000&sessionId=42"
    private fun gate() = ScanGate("dev.kamleon.app", foreignNoticeIntervalMs = 3_000L)

    @Test
    fun nothingHappensWhileClosed() {
        val gate = gate()
        assertEquals(Decision.Ignore, gate.onCode(link, 0))
        assertEquals(Decision.Ignore, gate.onCode("https://example.com", 0))
    }

    @Test
    fun aKamleonLinkIsLinkedOnceEvenIfEveryFrameReadsIt() {
        val gate = gate().apply { open = true }
        assertEquals(Decision.Link(QRResponse(unitId = "1000", sessionId = "42")), gate.onCode(link, 0))
        assertFalse(gate.open)
        // The next frames still see the code: no second upload until the screen reopens the gate.
        assertEquals(Decision.Ignore, gate.onCode(link, 33))
        assertEquals(Decision.Ignore, gate.onCode(link, 66))
    }

    @Test
    fun aForeignCodeIsReportedAtMostOnceEveryInterval() {
        val gate = gate().apply { open = true }
        assertEquals(Decision.Foreign, gate.onCode("https://example.com", 1_000))
        assertEquals(Decision.Ignore, gate.onCode("https://example.com", 1_033))
        assertEquals(Decision.Ignore, gate.onCode("plain text", 3_999))
        assertEquals(Decision.Foreign, gate.onCode("https://example.com", 4_000))
        // A foreign code does not stop the scanner: a Kamleon link right after it is linked.
        assertEquals(Decision.Link(QRResponse(unitId = "1000", sessionId = "42")), gate.onCode(link, 4_010))
    }
}
