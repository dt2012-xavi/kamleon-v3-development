package com.dynatech2012.kamleonuserapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.views.KamleonSheetContent
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KamleonSheetContentTest {
    @get:Rule val composeRule = createComposeRule()

    private val results = mutableListOf<SheetResult>()

    private fun setSheet(spec: SheetSpec) = composeRule.setContent {
        AppTheme(useDarkTheme = false) { KamleonSheetContent(spec) { results += it } }
    }

    @Test
    fun primaryAndSecondaryReportTheirResult() {
        setSheet(SheetSpec(message = "Sure?", primary = "Yes", secondary = "No"))
        composeRule.onNodeWithTag("sheet_primary").performClick()
        composeRule.onNodeWithTag("sheet_secondary").performClick()
        composeRule.runOnIdle { assertEquals(listOf(SheetResult.PRIMARY, SheetResult.SECONDARY), results) }
    }

    @Test
    fun secondaryIsOnlyShownWhenGiven() {
        setSheet(SheetSpec(message = "Done", primary = "OK"))
        composeRule.onNodeWithTag("sheet_secondary").assertDoesNotExist()
        composeRule.onNodeWithTag("sheet_message").assertIsDisplayed()
    }

    @Test
    fun handleIsOnlyShownWhenDismissible() {
        setSheet(SheetSpec(message = "m", primary = "OK", dismissible = true))
        composeRule.onNodeWithTag("sheet_handle").assertIsDisplayed()
    }

    @Test
    fun handleIsHiddenWhenNotDismissible() {
        setSheet(SheetSpec(message = "m", primary = "OK", dismissible = false))
        composeRule.onNodeWithTag("sheet_handle").assertDoesNotExist()
    }

    @Test
    fun titleIsOptional() {
        setSheet(SheetSpec(message = "m", primary = "OK"))
        composeRule.onNodeWithTag("sheet_title").assertDoesNotExist()
    }
}
