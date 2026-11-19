package com.dynatech2012.kamleonuserapp.network

import kotlinx.coroutines.delay

/**
 * Waits for the network, then [pauseMs] more, and runs [action] once. If the network is gone again
 * when the pause ends (a flapping connection), it waits again instead of firing into nothing. Only for
 * reads: an action the user triggered must never be repeated on its own.
 */
suspend fun NetworkMonitor.retryWhenOnline(pauseMs: Long = 600L, action: suspend () -> Unit) {
    do {
        awaitOnline()
        delay(pauseMs)
    } while (!isOnline)
    action()
}
