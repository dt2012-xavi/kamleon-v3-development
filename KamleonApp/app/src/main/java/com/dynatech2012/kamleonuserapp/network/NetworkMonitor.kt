package com.dynatech2012.kamleonuserapp.network

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

enum class NetworkStatus { Online, Offline }

/**
 * What the system says about the network. It is a hint, not the truth: Firestore keeps its own
 * connection state, and a captive portal or a VPN can make the system and the SDK disagree. Whoever
 * uses it must still let a user's explicit retry make the real call.
 */
interface NetworkMonitor {
    val status: StateFlow<NetworkStatus>
    val isOnline: Boolean get() = status.value == NetworkStatus.Online
}

/** Returns at once when already online, otherwise suspends until the network is back. */
suspend fun NetworkMonitor.awaitOnline() {
    status.first { it == NetworkStatus.Online }
}

/** Waits up to [timeoutMs] for the network: true if it is online by then. */
suspend fun NetworkMonitor.awaitOnlineFor(timeoutMs: Long): Boolean =
    withTimeoutOrNull(timeoutMs) { awaitOnline(); true } ?: false

/** Online means the default network reaches the Internet and the system has validated it. */
internal fun networkStatusFor(hasInternet: Boolean, validated: Boolean): NetworkStatus =
    if (hasInternet && validated) NetworkStatus.Online else NetworkStatus.Offline
