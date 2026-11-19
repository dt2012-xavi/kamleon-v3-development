package com.dynatech2012.kamleonuserapp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The system's default network as a [NetworkMonitor]. It lives as long as the app (one callback,
 * never unregistered), so it must be created once, as a singleton.
 */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val state = MutableStateFlow(currentStatus())
    override val status: StateFlow<NetworkStatus> = state

    init {
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                state.value = statusOf(capabilities)
            }

            override fun onLost(network: Network) {
                state.value = NetworkStatus.Offline
            }
        })
    }

    private fun currentStatus(): NetworkStatus {
        val network = connectivity.activeNetwork ?: return NetworkStatus.Offline
        return statusOf(connectivity.getNetworkCapabilities(network))
    }

    private fun statusOf(capabilities: NetworkCapabilities?) = networkStatusFor(
        hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    )
}
