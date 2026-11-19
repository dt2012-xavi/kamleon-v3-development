package com.dynatech2012.kamleonuserapp.network

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

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

/**
 * Offline only when there is no network with Internet capability at all (airplane mode, no Wi-Fi and
 * no data). The system's "validated" flag is deliberately NOT required: a captive portal or a network
 * that blocks Android's connectivity check never gets it, yet Firebase may work, and calling those
 * users offline would lock them out.
 */
internal fun networkStatusFor(hasInternet: Boolean): NetworkStatus =
    if (hasInternet) NetworkStatus.Online else NetworkStatus.Offline
