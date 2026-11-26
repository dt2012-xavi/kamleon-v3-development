package com.kamleonapp

import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.viewmodels.scanLinkFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanLinkTest {
    private val host = "dev.kamleon.app"

    @Test
    fun aKamleonScanLinkGivesItsUnitAndSession() {
        assertEquals(
            QRResponse(unitId = "1000", sessionId = "1708532818789"),
            scanLinkFrom("https://dev.kamleon.app/scan?unitId=1000&sessionId=1708532818789", host),
        )
    }

    @Test
    fun theLinkIsReadLeniently() {
        val expected = QRResponse(unitId = "u 1", sessionId = "s1")
        assertEquals(expected, scanLinkFrom("  HTTPS://Dev.Kamleon.app/scan/?sessionId=s1&unitId=u%201&x=y \n", host))
    }

    @Test
    fun codesThatAreNotAKamleonScanLinkAreIgnored() {
        listOf(
            "",
            "hello",
            "{\"unitId\": \"1000\", \"sessionId\": \"1\"}",
            "https://example.com/scan?unitId=1&sessionId=2",
            "https://kamleon.app/scan?unitId=1&sessionId=2",
            "http://dev.kamleon.app/scan?unitId=1&sessionId=2",
            "https://dev.kamleon.app/other?unitId=1&sessionId=2",
            "https://dev.kamleon.app/scan?unitId=1",
            "https://dev.kamleon.app/scan?unitId=&sessionId=2",
            "https://dev.kamleon.app/scan",
            "https://dev.kamleon.app/scan?unitId=%ZZ&sessionId=2 with spaces",
        ).forEach { code -> assertNull("code: $code", scanLinkFrom(code, host)) }
    }

    @Test
    fun idsThatCannotNameAFirestoreDocumentAreIgnored() {
        listOf(
            "https://dev.kamleon.app/scan?unitId=a%2Fb&sessionId=2",
            "https://dev.kamleon.app/scan?unitId=..&sessionId=2",
            "https://dev.kamleon.app/scan?unitId=.&sessionId=2",
            "https://dev.kamleon.app/scan?unitId=__x__&sessionId=2",
            "https://dev.kamleon.app/scan?unitId=%20%20&sessionId=2",
            "https://dev.kamleon.app/scan?unitId=1&sessionId=a%2Fb",
            "https://dev.kamleon.app/scan?unitId=${"1".repeat(257)}&sessionId=2",
        ).forEach { code -> assertNull("code: $code", scanLinkFrom(code, host)) }
    }
}
