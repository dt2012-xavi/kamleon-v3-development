package com.dynatech2012.kamleonuserapp

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.PrivacyScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrivacyScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun setScreen(onNext: () -> Unit = {}, onBack: () -> Unit = {}) {
        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                PrivacyScreen(onBack = onBack, onNext = onNext, onOpenUrl = {}, onEmail = {})
            }
        }
    }

    @Test
    fun nextNeedsAllThreeItems() {
        var next = 0
        setScreen(onNext = { next++ })

        composeRule.onNodeWithTag("privacy_next").assertIsNotEnabled()
        composeRule.onNodeWithTag("privacy_item_policy").performClick()
        composeRule.onNodeWithTag("privacy_item_terms").performClick()
        composeRule.onNodeWithTag("privacy_next").assertIsNotEnabled()
        composeRule.onNodeWithTag("privacy_item_consent").performClick()
        composeRule.onNodeWithTag("privacy_next").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, next) }
    }

    @Test
    fun acceptAllEnablesNextAndItemsCanBeUnchecked() {
        setScreen()

        composeRule.onNodeWithTag("privacy_accept_all").performClick()
        composeRule.onNodeWithTag("privacy_next").assertIsEnabled()
        composeRule.onNodeWithTag("privacy_item_terms").performClick()
        composeRule.onNodeWithTag("privacy_next").assertIsNotEnabled()
    }

    @Test
    fun backInvokesItsCallback() {
        var back = 0
        setScreen(onBack = { back++ })

        composeRule.onNodeWithTag("privacy_back").performClick()
        composeRule.runOnIdle { assertEquals(1, back) }
    }
}
