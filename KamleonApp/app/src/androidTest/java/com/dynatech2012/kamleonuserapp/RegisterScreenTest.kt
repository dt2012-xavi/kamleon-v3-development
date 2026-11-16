package com.dynatech2012.kamleonuserapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.RegisterScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun createNeedsAllFieldsAndMatchingPasswords() {
        var firstName by mutableStateOf("")
        var lastName by mutableStateOf("")
        var email by mutableStateOf("")
        var password by mutableStateOf("")
        var repeat by mutableStateOf("")
        var created = 0

        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                RegisterScreen(
                    firstName = firstName, lastName = lastName, email = email,
                    password = password, repeatPassword = repeat,
                    recovering = false, error = null,
                    onFirstNameChange = { firstName = it }, onLastNameChange = { lastName = it },
                    onEmailChange = { email = it }, onPasswordChange = { password = it },
                    onRepeatPasswordChange = { repeat = it },
                    onCreate = { created++ }, onLogin = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_create").assertIsNotEnabled()
        composeRule.onNodeWithTag("register_first_name").performTextInput("Ana")
        composeRule.onNodeWithTag("register_last_name").performTextInput("Gil")
        composeRule.onNodeWithTag("register_email").performTextInput("ana@example.com")
        composeRule.onNodeWithTag("register_password").performTextInput("secret1")
        composeRule.onNodeWithTag("register_repeat_password").performTextInput("other")
        composeRule.onNodeWithTag("register_create").assertIsNotEnabled()
        composeRule.onNodeWithTag("register_repeat_password").performTextInput("1")
        composeRule.onNodeWithTag("register_create").assertIsNotEnabled()
    }

    @Test
    fun matchingPasswordsEnableCreate() {
        var password by mutableStateOf("secret1")
        var repeat by mutableStateOf("")
        var created = 0

        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                RegisterScreen(
                    firstName = "Ana", lastName = "Gil", email = "ana@example.com",
                    password = password, repeatPassword = repeat,
                    recovering = false, error = null,
                    onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {},
                    onPasswordChange = { password = it }, onRepeatPasswordChange = { repeat = it },
                    onCreate = { created++ }, onLogin = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_create").assertIsNotEnabled()
        composeRule.onNodeWithTag("register_repeat_password").performTextInput("secret1")
        composeRule.onNodeWithTag("register_create").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, created) }
    }

    @Test
    fun recoveringHidesPasswordsAndLocksEmail() {
        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                RegisterScreen(
                    firstName = "Ana", lastName = "Gil", email = "ana@example.com",
                    password = "", repeatPassword = "",
                    recovering = true, error = null,
                    onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {},
                    onPasswordChange = {}, onRepeatPasswordChange = {},
                    onCreate = {}, onLogin = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_password").assertDoesNotExist()
        composeRule.onNodeWithTag("register_repeat_password").assertDoesNotExist()
        composeRule.onNodeWithTag("register_email").assertIsNotEnabled()
        composeRule.onNodeWithTag("register_create").assertIsEnabled()
    }

    @Test
    fun errorMessageIsShown() {
        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                RegisterScreen(
                    firstName = "", lastName = "", email = "", password = "", repeatPassword = "",
                    recovering = false, error = R.string.signup_error_password,
                    onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {},
                    onPasswordChange = {}, onRepeatPasswordChange = {},
                    onCreate = {}, onLogin = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_error").assertIsDisplayed()
    }

    @Test
    fun differingPasswordsExplainThemselvesWhileTyping() {
        var password by mutableStateOf("")
        var repeat by mutableStateOf("")

        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                RegisterScreen(
                    firstName = "Ana", lastName = "Gil", email = "ana@example.com",
                    password = password, repeatPassword = repeat,
                    recovering = false, error = null,
                    onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {},
                    onPasswordChange = { password = it }, onRepeatPasswordChange = { repeat = it },
                    onCreate = {}, onLogin = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_error").assertDoesNotExist()
        composeRule.onNodeWithTag("register_password").performTextInput("secret1")
        composeRule.onNodeWithTag("register_repeat_password").performTextInput("secret2")
        composeRule.onNodeWithTag("register_error").assertIsDisplayed()
        composeRule.onNodeWithTag("register_repeat_password").performTextInput("1")
        composeRule.onNodeWithTag("register_error").assertDoesNotExist()
    }

    @Test
    fun invalidEmailIsReportedOnlyAfterLeavingTheField() {
        var email by mutableStateOf("")

        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                RegisterScreen(
                    firstName = "", lastName = "", email = email, password = "", repeatPassword = "",
                    recovering = false, error = null,
                    onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = { email = it },
                    onPasswordChange = {}, onRepeatPasswordChange = {},
                    onCreate = {}, onLogin = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_email").requestFocus()
        composeRule.onNodeWithTag("register_email").performTextInput("not-an-email")
        composeRule.onNodeWithTag("register_error").assertDoesNotExist()
        composeRule.onNodeWithTag("register_first_name").requestFocus()
        composeRule.onNodeWithTag("register_error").assertIsDisplayed()
    }
}
