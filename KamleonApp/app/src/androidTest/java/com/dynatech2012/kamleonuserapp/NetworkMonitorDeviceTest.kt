package com.dynatech2012.kamleonuserapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dynatech2012.kamleonuserapp.network.AndroidNetworkMonitor
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The real monitor against the device's network: run it with the phone connected to the Internet. */
@RunWith(AndroidJUnit4::class)
class NetworkMonitorDeviceTest {
    @Test fun reportsOnlineWhenTheDeviceIsConnected() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(AndroidNetworkMonitor(context).isOnline)
    }
}
