package com.dynatech2012.kamleonuserapp.security

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class HibpPasswordBreachCheckerTest {
    private class FakeConnection(
        url: URL,
        private val code: Int,
        private val body: String,
        private val failWith: IOException?
    ) : HttpURLConnection(url) {
        val headers = mutableMapOf<String, String>()
        override fun setRequestProperty(key: String, value: String) { headers[key] = value }
        override fun getResponseCode(): Int = failWith?.let { throw it } ?: code
        override fun getInputStream(): InputStream = ByteArrayInputStream(body.toByteArray())
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy() = false
    }

    private var connection: FakeConnection? = null

    private fun checker(code: Int = 200, body: String = "", failWith: IOException? = null) =
        HibpPasswordBreachChecker(openConnection = { url ->
            FakeConnection(url, code, body, failWith).also { connection = it }
        })

    // SHA-1("password") = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
    private val passwordSuffix = "1E4C9B93F3F0682250B6CF8331B7EE68FD8"

    @Test fun sha1IsUppercaseHex() {
        assertEquals("5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8", HibpPasswordBreachChecker.sha1Hex("password"))
    }

    @Test fun onlyThePrefixLeavesTheDeviceAndPaddingIsOff() = runBlocking {
        checker(body = "").isBreached("password")
        val used = connection!!
        assertTrue(used.url.toString().endsWith("/range/5BAA6"))
        assertFalse(used.url.toString().contains(passwordSuffix))
        assertEquals("false", used.headers["Add-Padding"])
    }

    @Test fun matchingSuffixMeansBreached() = runBlocking {
        val body = "0018A45C4D1DEF81644B54AB7F969B88D65:1\r\n$passwordSuffix:10434004\r\n"
        assertTrue(checker(body = body).isBreached("password"))
    }

    @Test fun missingSuffixMeansNotBreached() = runBlocking {
        assertFalse(checker(body = "0018A45C4D1DEF81644B54AB7F969B88D65:1\r\n").isBreached("password"))
    }

    @Test fun nonSuccessResponseThrows() {
        try {
            runBlocking { checker(code = 503).isBreached("password") }
            fail("expected IOException")
        } catch (_: IOException) {
        }
    }

    @Test fun networkErrorThrows() {
        try {
            runBlocking { checker(failWith = IOException("timeout")).isBreached("password") }
            fail("expected IOException")
        } catch (_: IOException) {
        }
    }
}
