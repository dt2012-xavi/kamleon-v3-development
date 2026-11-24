package com.dynatech2012.kamleonuserapp.viewmodels

import com.dynatech2012.kamleonuserapp.models.QRResponse

/**
 * Decides what to do with each code the scanner reads, which arrive once per camera frame. While
 * [open], the first Kamleon scan link closes the gate and is linked (one upload per scan); other
 * codes are reported at most once every [foreignNoticeIntervalMs] so the notice doesn't flicker.
 */
internal class ScanGate(
    private val host: String,
    private val foreignNoticeIntervalMs: Long = 3_000L,
) {
    sealed interface Decision {
        /** Not scanning, or a notice was given too recently. */
        data object Ignore : Decision
        /** A code that is not a Kamleon scan link. */
        data object Foreign : Decision
        /** A Kamleon scan to link; the gate is now closed. */
        data class Link(val scan: QRResponse) : Decision
    }

    var open: Boolean = false
    private var lastForeignNoticeAt: Long? = null

    fun onCode(code: String, now: Long): Decision {
        if (!open) return Decision.Ignore
        val scan = scanLinkFrom(code, host)
        if (scan != null) {
            open = false
            return Decision.Link(scan)
        }
        val last = lastForeignNoticeAt
        if (last != null && now - last < foreignNoticeIntervalMs) return Decision.Ignore
        lastForeignNoticeAt = now
        return Decision.Foreign
    }
}
