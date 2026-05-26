package com.kamleonapp

import com.dynatech2012.kamleonuserapp.utils.DebugLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugLogTest {
    @Test
    fun isEnabledOnlyForDebugBuilds() {
        assertTrue(DebugLog.isEnabled(debug = true))
        assertFalse(DebugLog.isEnabled(debug = false))
    }
}
