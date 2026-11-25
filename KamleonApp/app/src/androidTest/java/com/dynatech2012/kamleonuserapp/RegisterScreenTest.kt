package com.dynatech2012.kamleonuserapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.RegisterScreen
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private var firstName by mutableStateOf("")
    private var lastName by mutableStateOf("")
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var repeat by mutableStateOf("")
    private var created = 0

    private fun setScreen(
        recovering: Boolean = false,
        error: Int? = null,
        strength: PasswordStrength? = null,
        checking: Boolean = false
    ) = composeRule.setContent {
        AppTheme(useDarkTheme = false) {
            RegisterScreen(
                firstName = firstName, lastName = lastName, email = email,
                password = password, repeatPassword = repeat,
                recovering = recovering, error = error,
                checking = checking, passwordStrength = strength,
                onFirstNameChange = { firstName = it }, onLastNameChange = { lastName = it },
                onEmailChange = { email = it }, onPasswordChange = { password = it },
                onRepeatPasswordChange = { repeat = it },
                onCreate = { created++ }, onLogin = {}
            )
        }
    }

    private fun fillValid() {
        firstName = "Ana"; lastName = "Gil"; email = "ana@example.com"
        password = "correct-horse-1"; repeat = "correct-horse-1"
    }

    @Test
    fun validFormSubmits() {
        fillValid()
        setScreen()
        composeRule.onNodeWithTag("register_create").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, created) }
    }

    @Test
    fun imeDoneOnLastFieldSubmitsWhenValid() {
        fillValid()
        setScreen()
        composeRule.onNodeWithTag("register_repeat_password").performImeAction()
        composeRule.runOnIdle { assertEquals(1, created) }
    }

    @Test
    fun emptyFormExplainsEachFieldAndFocusesTheFirst() {
        setScreen()
        composeRule.onNodeWithTag("register_first_name_error").assertDoesNotExist()

        composeRule.onNodeWithTag("register_create").performClick()
        composeRule.onNodeWithTag("register_first_name_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_last_name_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_email_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_password_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_repeat_password_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_first_name").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, created) }

        composeRule.onNodeWithTag("register_first_name").performTextInput("Ana")
        composeRule.onNodeWithTag("register_first_name_error").assertDoesNotExist()
    }

    @Test
    fun mismatchedPasswordsBlockSubmitAndFocusTheRepeatField() {
        fillValid()
        repeat = "other"
        setScreen()
        composeRule.onNodeWithTag("register_repeat_password_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_create").performClick()
        composeRule.onNodeWithTag("register_repeat_password").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, created) }
    }

    @Test
    fun recoveringHidesPasswordsAndLocksEmail() {
        firstName = "Ana"; lastName = "Gil"; email = "ana@example.com"
        setScreen(recovering = true)

        composeRule.onNodeWithTag("register_password").assertDoesNotExist()
        composeRule.onNodeWithTag("register_repeat_password").assertDoesNotExist()
        composeRule.onNodeWithTag("register_email").assertIsNotEnabled()
        composeRule.onNodeWithTag("register_create").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, created) }
    }

    @Test
    fun viewModelErrorIsShownNextToItsFieldAndFocused() {
        fillValid()
        setScreen(error = R.string.password_error_breached)
        composeRule.onNodeWithTag("register_password_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_password").assertIsFocused()
    }

    @Test
    fun shortPasswordBlocksSubmitWithTheLengthMessage() {
        fillValid()
        password = "abc"; repeat = "abc"
        setScreen()
        composeRule.onNodeWithTag("register_create").performClick()
        composeRule.onNodeWithTag("register_password_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_password").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, created) }
    }

    @Test
    fun weakPasswordBlocksSubmitUntilItIsStrongEnough() {
        fillValid()
        setScreen(strength = PasswordStrength.WEAK)
        composeRule.onNodeWithTag("register_password_strength").assertIsDisplayed()
        composeRule.onNodeWithTag("register_create").performClick()
        composeRule.onNodeWithTag("register_password_error").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, created) }
    }

    @Test
    fun strongPasswordShowsTheMeterAndSubmits() {
        fillValid()
        setScreen(strength = PasswordStrength.STRONG)
        composeRule.onNodeWithTag("register_password_strength").assertIsDisplayed()
        composeRule.onNodeWithTag("register_create").performClick()
        composeRule.runOnIdle { assertEquals(1, created) }
    }

    @Test
    fun whileCheckingThePolicyTheButtonIsInert() {
        fillValid()
        setScreen(strength = PasswordStrength.STRONG, checking = true)
        composeRule.onNodeWithTag("register_create").performClick()
        composeRule.runOnIdle { assertEquals(0, created) }
    }

    @Test
    fun sessionExpiredErrorAppearsAboveTheButton() {
        fillValid()
        setScreen(error = R.string.signup_session_expired)
        composeRule.onNodeWithTag("register_error").assertIsDisplayed()
    }

    @Test
    fun differingPasswordsExplainThemselvesWhileTyping() {
        firstName = "Ana"; lastName = "Gil"; email = "ana@example.com"
        setScreen()

        composeRule.onNodeWithTag("register_repeat_password_error").assertDoesNotExist()
        composeRule.onNodeWithTag("register_password").performTextInput("secret1")
        composeRule.onNodeWithTag("register_repeat_password").performTextInput("secret2")
        composeRule.onNodeWithTag("register_repeat_password_error").assertIsDisplayed()
        // Once both match, the warning goes.
        composeRule.onNodeWithTag("register_repeat_password").performTextReplacement("secret1")
        composeRule.onNodeWithTag("register_repeat_password_error").assertDoesNotExist()
    }

    @Test
    fun invalidEmailIsReportedOnlyAfterLeavingTheField() {
        setScreen()

        composeRule.onNodeWithTag("register_email").requestFocus()
        composeRule.onNodeWithTag("register_email").performTextInput("not-an-email")
        composeRule.onNodeWithTag("register_email_error").assertDoesNotExist()
        composeRule.onNodeWithTag("register_first_name").requestFocus()
        composeRule.onNodeWithTag("register_email_error").assertIsDisplayed()
    }
}
