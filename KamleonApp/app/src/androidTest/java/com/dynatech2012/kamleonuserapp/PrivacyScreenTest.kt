package com.dynatech2012.kamleonuserapp

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.PrivacyScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Ticks an item through its own toggle action: a tap on the item's centre can land on one of the
 * links in its text (Terms, consent information), which opens the page instead of ticking it.
 */
private fun SemanticsNodeInteraction.toggle() = performScrollTo().performSemanticsAction(SemanticsActions.OnClick)

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
        composeRule.onNodeWithTag("privacy_item_policy").toggle()
        composeRule.onNodeWithTag("privacy_item_terms").toggle()
        composeRule.onNodeWithTag("privacy_next").assertIsNotEnabled()
        composeRule.onNodeWithTag("privacy_item_consent").toggle()
        composeRule.onNodeWithTag("privacy_next").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, next) }
    }

    @Test
    fun acceptAllEnablesNextAndItemsCanBeUnchecked() {
        setScreen()

        composeRule.onNodeWithTag("privacy_accept_all").performClick()
        composeRule.onNodeWithTag("privacy_next").assertIsEnabled()
        composeRule.onNodeWithTag("privacy_item_terms").toggle()
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
