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
import androidx.compose.ui.test.requestFocus
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.RegisterScreen
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

    private fun setScreen(recovering: Boolean = false, error: Int? = null) = composeRule.setContent {
        AppTheme(useDarkTheme = false) {
            RegisterScreen(
                firstName = firstName, lastName = lastName, email = email,
                password = password, repeatPassword = repeat,
                recovering = recovering, error = error,
                onFirstNameChange = { firstName = it }, onLastNameChange = { lastName = it },
                onEmailChange = { email = it }, onPasswordChange = { password = it },
                onRepeatPasswordChange = { repeat = it },
                onCreate = { created++ }, onLogin = {}
            )
        }
    }

    private fun fillValid() {
        firstName = "Ana"; lastName = "Gil"; email = "ana@example.com"
        password = "secret1"; repeat = "secret1"
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
        password = "abc"; repeat = "abc"
        setScreen(error = R.string.signup_error_password)
        composeRule.onNodeWithTag("register_password_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_password").assertIsFocused()
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
        composeRule.onNodeWithTag("register_repeat_password").performTextInput("1")
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
