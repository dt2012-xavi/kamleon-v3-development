package com.dynatech2012.kamleonuserapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.LoginScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun signInRequiresBothFieldsAndSubmitsTheirValues() {
        var submitted: Pair<String, String>? = null
        var email by mutableStateOf("")
        var password by mutableStateOf("")

        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                LoginScreen(
                    email = email,
                    password = password,
                    isLoading = false,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onSignIn = { submitted = email to password },
                    onForgotPassword = {},
                    onRegister = {}
                )
            }
        }

        composeRule.onNodeWithTag("login_sign_in").assertIsNotEnabled()
        composeRule.onNodeWithTag("login_email").performTextInput("test@example.com")
        composeRule.onNodeWithTag("login_sign_in").assertIsNotEnabled()
        composeRule.onNodeWithTag("login_password").performTextInput("secret123")
        composeRule.onNodeWithTag("login_sign_in").assertIsEnabled()
        composeRule.onNodeWithTag("login_password").performImeAction()
        composeRule.runOnIdle {
            assertEquals("test@example.com" to "secret123", submitted)
        }
    }
}
