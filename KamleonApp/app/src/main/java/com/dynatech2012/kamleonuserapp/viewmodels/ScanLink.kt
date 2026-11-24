package com.dynatech2012.kamleonuserapp.viewmodels

import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.models.QRResponse
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

/**
 * The scan in a code read by the in-app scanner, or null when the code is not a Kamleon scan link:
 * `https://<host>/scan?unitId=…&sessionId=…`, the same link the S-Urinal's QR opens as an app link.
 * Anything else (another site, plain text, a link without both ids) is null, never an error.
 */
internal fun scanLinkFrom(code: String, host: String): QRResponse? {
    val uri = try {
        URI(code.trim())
    } catch (_: URISyntaxException) {
        return null
    }
    if (!uri.scheme.equals("https", ignoreCase = true)) return null
    if (!uri.host.equals(host, ignoreCase = true)) return null
    if (uri.path?.trimEnd('/') != "/${Constants.SCAN_ACTION}") return null
    val params = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
        val name = pair.substringBefore('=', missingDelimiterValue = "")
        if (name.isEmpty()) null else decode(name) to decode(pair.substringAfter('='))
    }.toMap()
    val unitId = params[Constants.SCAN_UNIT_ID]
    val sessionId = params[Constants.SCAN_SESSION_ID]
    if (unitId.isNullOrBlank() || sessionId.isNullOrBlank()) return null
    return QRResponse(unitId = unitId, sessionId = sessionId)
}

private fun decode(value: String): String = try {
    URLDecoder.decode(value, Charsets.UTF_8.name())
} catch (_: IllegalArgumentException) {
    value
}
