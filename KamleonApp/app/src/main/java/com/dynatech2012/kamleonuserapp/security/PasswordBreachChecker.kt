package com.dynatech2012.kamleonuserapp.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

fun interface PasswordBreachChecker {
    /** True if the password appears in known breaches. Throws if the check cannot be completed. */
    suspend fun isBreached(password: String): Boolean
}

/**
 * Have I Been Pwned "Pwned Passwords" range API with k-anonymity: only the first 5 hex characters
 * of the SHA-1 leave the device; the rest is matched locally. `Add-Padding` is deliberately false
 * (same as the web app). The whole call is bounded by [TIMEOUT_MS]. Throws on timeout, network
 * error or a non-2xx response; the caller decides to fail open.
 */
class HibpPasswordBreachChecker(
    private val baseUrl: String = "https://api.pwnedpasswords.com/range/",
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) : PasswordBreachChecker {

    override suspend fun isBreached(password: String): Boolean {
        val hash = sha1Hex(password)
        val prefix = hash.substring(0, PREFIX_LENGTH)
        val suffix = hash.substring(PREFIX_LENGTH)
        val body = withContext(Dispatchers.IO) {
            val connection = openConnection(URL(baseUrl + prefix))
            // Overall deadline: disconnecting makes any blocked connect/read fail immediately.
            val watchdog = launch {
                delay(TIMEOUT_MS)
                connection.disconnect()
            }
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT_MS.toInt()
                connection.readTimeout = TIMEOUT_MS.toInt()
                connection.setRequestProperty("Add-Padding", "false")
                val code = connection.responseCode
                if (code !in 200..299) throw IOException("HIBP responded $code")
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                watchdog.cancel()
                connection.disconnect()
            }
        }
        return body.lineSequence().any { line ->
            val separator = line.indexOf(':')
            separator > 0 && line.substring(0, separator).equals(suffix, ignoreCase = true) &&
                (line.substring(separator + 1).trim().toLongOrNull() ?: 0L) > 0L
        }
    }

    internal companion object {
        const val TIMEOUT_MS = 1500L
        private const val PREFIX_LENGTH = 5

        fun sha1Hex(value: String): String =
            MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02X".format(it) }
    }
}
