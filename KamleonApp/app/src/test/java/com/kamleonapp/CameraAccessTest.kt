package com.kamleonapp

import com.dynatech2012.kamleonuserapp.views.CameraAccess
import com.dynatech2012.kamleonuserapp.views.cameraAccessOf
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraAccessTest {
    @Test
    fun noCameraWinsOverEverythingElse() =
        assertEquals(CameraAccess.NoCamera, cameraAccessOf(hasCamera = false, granted = true, askedBefore = true, showRationale = true))

    @Test
    fun grantedMeansTheScannerCanStart() =
        assertEquals(CameraAccess.Granted, cameraAccessOf(hasCamera = true, granted = true, askedBefore = false, showRationale = false))

    @Test
    fun neverAskedIsAskNotBlocked() =
        // Android reports no rationale before the first question, just like after a refusal for good.
        assertEquals(CameraAccess.Ask, cameraAccessOf(hasCamera = true, granted = false, askedBefore = false, showRationale = false))

    @Test
    fun refusedOnceCanStillBeAskedAgain() =
        assertEquals(CameraAccess.Ask, cameraAccessOf(hasCamera = true, granted = false, askedBefore = true, showRationale = true))

    @Test
    fun refusedForGoodOnlyTheSettingsCanAllowIt() =
        assertEquals(CameraAccess.Blocked, cameraAccessOf(hasCamera = true, granted = false, askedBefore = true, showRationale = false))
}
