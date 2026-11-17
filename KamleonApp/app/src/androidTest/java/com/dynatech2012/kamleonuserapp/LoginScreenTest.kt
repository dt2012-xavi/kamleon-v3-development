package com.dynatech2012.kamleonuserapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.LoginScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var submissions = mutableListOf<Pair<String, String>>()

    private fun setScreen() = composeRule.setContent {
        AppTheme(useDarkTheme = false) {
            LoginScreen(
                email = email,
                password = password,
                isLoading = false,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onSignIn = { submissions += email to password },
                onForgotPassword = {},
                onRegister = {}
            )
        }
    }

    @Test
    fun signInSubmitsBothValues() {
        setScreen()
        composeRule.onNodeWithTag("login_email").performTextInput("test@example.com")
        composeRule.onNodeWithTag("login_password").performTextInput("secret123")
        composeRule.onNodeWithTag("login_sign_in").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(listOf("test@example.com" to "secret123"), submissions) }
    }

    @Test
    fun blankFieldsAreExplainedNextToTheFieldAndFocused() {
        setScreen()
        composeRule.onNodeWithTag("login_email_error").assertDoesNotExist()

        composeRule.onNodeWithTag("login_sign_in").performClick()
        composeRule.onNodeWithTag("login_email_error").assertIsDisplayed()
        composeRule.onNodeWithTag("login_email").assertIsFocused()

        composeRule.onNodeWithTag("login_email").performTextInput("test@example.com")
        composeRule.onNodeWithTag("login_email_error").assertDoesNotExist()
        composeRule.onNodeWithTag("login_sign_in").performClick()
        composeRule.onNodeWithTag("login_password_error").assertIsDisplayed()
        composeRule.onNodeWithTag("login_password").assertIsFocused()
        composeRule.runOnIdle { assertEquals(emptyList<Pair<String, String>>(), submissions) }
    }

    @Test
    fun imeDoneSubmitsWhenValid() {
        setScreen()
        composeRule.onNodeWithTag("login_email").performTextInput("test@example.com")
        composeRule.onNodeWithTag("login_password").performTextInput("secret123")
        composeRule.onNodeWithTag("login_password").performImeAction()
        composeRule.runOnIdle { assertEquals(listOf("test@example.com" to "secret123"), submissions) }
    }
}
