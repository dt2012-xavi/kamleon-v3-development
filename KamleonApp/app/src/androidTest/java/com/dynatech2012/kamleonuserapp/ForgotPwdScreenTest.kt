package com.dynatech2012.kamleonuserapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.ForgotPwdScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ForgotPwdScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun sendNeedsAnEmailAndSubmitsIt() {
        var sent: String? = null
        var email by mutableStateOf("")

        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                ForgotPwdScreen(
                    email = email,
                    onEmailChange = { email = it },
                    onSend = { sent = email },
                    onClose = {},
                    onContactSupport = {}
                )
            }
        }

        composeRule.onNodeWithTag("forgot_send").assertIsNotEnabled()
        composeRule.onNodeWithTag("forgot_email").performTextInput("test@example.com")
        composeRule.onNodeWithTag("forgot_send").assertIsEnabled()
        composeRule.onNodeWithTag("forgot_send").performClick()
        composeRule.runOnIdle { assertEquals("test@example.com", sent) }
    }

    @Test
    fun closeAndContactInvokeTheirCallbacks() {
        var closed = 0
        var contacted = 0

        composeRule.setContent {
            AppTheme(useDarkTheme = false) {
                ForgotPwdScreen(
                    email = "",
                    onEmailChange = {},
                    onSend = {},
                    onClose = { closed++ },
                    onContactSupport = { contacted++ }
                )
            }
        }

        composeRule.onNodeWithTag("forgot_close").performClick()
        composeRule.onNodeWithTag("forgot_contact").performClick()
        composeRule.runOnIdle {
            assertEquals(1, closed)
            assertEquals(1, contacted)
        }
    }
}
