package com.kamleonapp

import com.dynatech2012.kamleonuserapp.network.NetworkMonitor
import com.dynatech2012.kamleonuserapp.network.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A monitor the test drives by hand. */
class FakeNetworkMonitor(initial: NetworkStatus = NetworkStatus.Online) : NetworkMonitor {
    private val state = MutableStateFlow(initial)
    override val status: StateFlow<NetworkStatus> = state

    fun set(status: NetworkStatus) {
        state.value = status
    }
}
