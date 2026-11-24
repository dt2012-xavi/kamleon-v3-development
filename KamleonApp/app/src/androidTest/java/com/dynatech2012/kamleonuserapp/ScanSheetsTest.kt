package com.dynatech2012.kamleonuserapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.viewmodels.ScanLinkState
import com.dynatech2012.kamleonuserapp.views.CameraAccess
import com.dynatech2012.kamleonuserapp.views.CameraAccessPanel
import com.dynatech2012.kamleonuserapp.views.ScanLinkContent
import com.dynatech2012.kamleonuserapp.views.SheetResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** The scan sheet in each of its states, and the scanner's camera access panel. */
class ScanSheetsTest {
    @get:Rule val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val actions = mutableListOf<Pair<ScanLinkState, SheetResult>>()
    private var allows = 0
    private var settings = 0

    private fun setSheet(state: ScanLinkState) = composeRule.setContent {
        AppTheme(useDarkTheme = false) { ScanLinkContent(state) { s, r -> actions += s to r } }
    }

    private fun setPanel(access: CameraAccess) = composeRule.setContent {
        AppTheme(useDarkTheme = false) {
            CameraAccessPanel(access, onAllow = { allows++ }, onOpenSettings = { settings++ })
        }
    }

    private fun text(id: Int) = context.getString(id)

    @Test
    fun whileLinkingThereIsNoWayOut() {
        setSheet(ScanLinkState.Linking)
        composeRule.onNodeWithTag("scan_linking").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.scan_linking)).assertIsDisplayed()
        composeRule.onNodeWithTag("sheet_primary").assertDoesNotExist()
    }

    @Test
    fun aLinkedScanSaysWhatHappensNext() {
        setSheet(ScanLinkState.Linked)
        composeRule.onNodeWithText(text(R.string.dialog_scan_scanned)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.dialog_scan_scanned_message)).assertIsDisplayed()
        composeRule.onNodeWithTag("sheet_primary").performClick()
        composeRule.runOnIdle { assertEquals(listOf(ScanLinkState.Linked to SheetResult.PRIMARY), actions) }
    }

    @Test
    fun aFailedLinkBlamesTheConnectionWhenThatIsTheCause() {
        setSheet(ScanLinkState.Failed(IOException("offline")))
        composeRule.onNodeWithText(text(R.string.login_error_title_network)).assertIsDisplayed()
    }

    @Test
    fun aFailedLinkForAnotherReasonSaysTheCodeWasNotLinked() {
        setSheet(ScanLinkState.Failed(IllegalStateException("rejected")))
        composeRule.onNodeWithText(text(R.string.dialog_scan_failed_title)).assertIsDisplayed()
    }

    @Test
    fun anInvalidLinkOffersToScanAgainOrNot() {
        setSheet(ScanLinkState.Invalid)
        composeRule.onNodeWithText(text(R.string.scan_invalid_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.scan_again)).performClick()
        composeRule.onNodeWithTag("sheet_secondary").performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf(ScanLinkState.Invalid to SheetResult.PRIMARY, ScanLinkState.Invalid to SheetResult.SECONDARY),
                actions
            )
        }
    }

    @Test
    fun withoutPermissionThePanelAsksForTheCamera() {
        setPanel(CameraAccess.Ask)
        composeRule.onNodeWithText(text(R.string.scan_camera_denied_title)).assertIsDisplayed()
        composeRule.onNodeWithTag("camera_allow").performClick()
        composeRule.onNodeWithTag("camera_settings").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, allows) }
    }

    @Test
    fun whenRefusedForGoodThePanelOpensTheSettings() {
        setPanel(CameraAccess.Blocked)
        composeRule.onNodeWithText(text(R.string.scan_camera_blocked_message)).assertIsDisplayed()
        composeRule.onNodeWithTag("camera_settings").performClick()
        composeRule.onNodeWithTag("camera_allow").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, settings) }
    }

    @Test
    fun withoutACameraThereIsNothingToAllow() {
        setPanel(CameraAccess.NoCamera)
        composeRule.onNodeWithText(text(R.string.scan_no_camera_title)).assertIsDisplayed()
        composeRule.onNodeWithTag("camera_allow").assertDoesNotExist()
        composeRule.onNodeWithTag("camera_settings").assertDoesNotExist()
    }

    @Test
    fun withTheCameraAllowedThePanelIsNotShown() {
        setPanel(CameraAccess.Granted)
        composeRule.onNodeWithTag("camera_access").assertDoesNotExist()
    }
}
