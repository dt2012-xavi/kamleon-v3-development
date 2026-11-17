package com.dynatech2012.kamleonuserapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.PwdScreen
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PwdScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private var current by mutableStateOf("")
    private var new by mutableStateOf("")
    private var confirm by mutableStateOf("")
    private var saved = 0
    private var back = 0

    private fun setScreen(
        currentError: Int? = null,
        newError: Int? = null,
        strength: PasswordStrength? = null,
        checking: Boolean = false
    ) = composeRule.setContent {
        AppTheme(useDarkTheme = false) {
            PwdScreen(
                currentPassword = current, newPassword = new, confirmPassword = confirm,
                currentError = currentError, newError = newError,
                checking = checking, passwordStrength = strength,
                onCurrentPasswordChange = { current = it }, onNewPasswordChange = { new = it },
                onConfirmPasswordChange = { confirm = it },
                onSave = { saved++ }, onForgotPassword = {}, onBack = { back++ }
            )
        }
    }

    private fun fillValid() {
        current = "old-secret-1"; new = "correct-horse-1"; confirm = "correct-horse-1"
    }

    @Test
    fun validFormSaves() {
        fillValid()
        setScreen(strength = PasswordStrength.STRONG)
        composeRule.onNodeWithTag("pwd_save").performClick()
        composeRule.runOnIdle { assertEquals(1, saved) }
    }

    @Test
    fun imeDoneOnConfirmSavesWhenValid() {
        fillValid()
        setScreen(strength = PasswordStrength.STRONG)
        composeRule.onNodeWithTag("pwd_confirm").performImeAction()
        composeRule.runOnIdle { assertEquals(1, saved) }
    }

    @Test
    fun emptyFormExplainsEachFieldAndFocusesTheFirst() {
        setScreen()
        composeRule.onNodeWithTag("pwd_current_error").assertDoesNotExist()
        composeRule.onNodeWithTag("pwd_save").performClick()
        composeRule.onNodeWithTag("pwd_current_error").assertIsDisplayed()
        composeRule.onNodeWithTag("pwd_new_error").assertIsDisplayed()
        composeRule.onNodeWithTag("pwd_confirm_error").assertIsDisplayed()
        composeRule.onNodeWithTag("pwd_current").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, saved) }
    }

    @Test
    fun sameAsCurrentBlocksSaveAndFocusesTheNewField() {
        current = "same-pass-1"; new = "same-pass-1"; confirm = "same-pass-1"
        setScreen(strength = PasswordStrength.STRONG)
        composeRule.onNodeWithTag("pwd_new_error").assertIsDisplayed()
        composeRule.onNodeWithTag("pwd_save").performClick()
        composeRule.onNodeWithTag("pwd_new").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, saved) }
    }

    @Test
    fun mismatchExplainsItselfWhileTyping() {
        current = "old-secret-1"
        setScreen()
        composeRule.onNodeWithTag("pwd_new").performTextInput("secret-123")
        composeRule.onNodeWithTag("pwd_confirm").performTextInput("secret-12")
        composeRule.onNodeWithTag("pwd_confirm_error").assertIsDisplayed()
        composeRule.onNodeWithTag("pwd_confirm").performTextInput("3")
        composeRule.onNodeWithTag("pwd_confirm_error").assertDoesNotExist()
    }

    @Test
    fun weakPasswordShowsTheMeterAndBlocksSave() {
        fillValid()
        setScreen(strength = PasswordStrength.WEAK)
        composeRule.onNodeWithTag("pwd_new_strength").assertIsDisplayed()
        composeRule.onNodeWithTag("pwd_save").performClick()
        composeRule.onNodeWithTag("pwd_new_error").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, saved) }
    }

    @Test
    fun wrongCurrentPasswordIsShownNextToItAndFocused() {
        fillValid()
        setScreen(currentError = R.string.login_alert_description_wrong_pass)
        composeRule.onNodeWithTag("pwd_current_error").assertIsDisplayed()
        composeRule.onNodeWithTag("pwd_current").assertIsFocused()
    }

    @Test
    fun whileCheckingTheButtonIsInert() {
        fillValid()
        setScreen(strength = PasswordStrength.STRONG, checking = true)
        composeRule.onNodeWithTag("pwd_save").performClick()
        composeRule.runOnIdle { assertEquals(0, saved) }
    }

    @Test
    fun backButtonNavigatesBack() {
        setScreen()
        composeRule.onNodeWithTag("pwd_back").performClick()
        composeRule.runOnIdle { assertEquals(1, back) }
    }
}
