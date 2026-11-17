package com.dynatech2012.kamleonuserapp.viewmodels

/**
 * How long a scan link may wait for the user to log in or register before it is dropped. A
 * kiosk session does not wait for hours, so an older scan would only upload a dead session.
 */
internal const val PENDING_SCAN_MAX_AGE_MS = 10 * 60 * 1000L

/**
 * The scan to deliver, or null when it is incomplete or too old. [savedAt] and [now] come from
 * `SystemClock.elapsedRealtime()`; a saved time after [now] means the device rebooted.
 */
internal fun pendingScanFor(
    unitId: String?,
    sessionId: String?,
    savedAt: Long?,
    now: Long
): Pair<String, String>? {
    if (unitId.isNullOrEmpty() || sessionId.isNullOrEmpty() || savedAt == null) return null
    val age = now - savedAt
    return if (age in 0..PENDING_SCAN_MAX_AGE_MS) unitId to sessionId else null
}
