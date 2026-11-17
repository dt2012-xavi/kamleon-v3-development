package com.dynatech2012.kamleonuserapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.OnboardingScreen
import com.dynatech2012.kamleonuserapp.fragments.OnboardingSteps
import com.ozcanalasalvar.datepicker.model.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Composable
    private fun Screen(
        step: Int,
        nextLoading: Boolean = false,
        showBack: Boolean = true,
        onBack: () -> Unit = {},
        onNext: () -> Unit = {},
        onSkip: () -> Unit = {}
    ) {
        val date = Date(java.util.Date().time)
        AppTheme(useDarkTheme = false) {
            OnboardingScreen(
                step = step, nextLoading = nextLoading, pickerKey = 0,
                birthDate = date, birthYearsRange = 1904..2012, maxBirthDate = date,
                heightValues = (140..230).map { "$it" }, weightValues = (30..130).map { "$it" },
                genderValues = listOf("Male", "Female", "Other", "Prefer not to say"),
                selectedHeight = "", selectedWeight = "", selectedGender = "",
                onBirthDateChange = { _, _, _, _ -> }, onHeightChange = {}, onWeightChange = {},
                onGenderChange = {}, onBack = onBack, onNext = onNext, onSkip = onSkip,
                showBack = showBack
            )
        }
    }

    @Test
    fun skipIsOnlyOfferedFromTheHeightStepOn() {
        var step by mutableIntStateOf(OnboardingSteps.NOTIFICATIONS)
        composeRule.setContent { Screen(step) }

        composeRule.onNodeWithTag("onboarding_skip").assertDoesNotExist()
        step = OnboardingSteps.HEIGHT
        composeRule.onNodeWithTag("onboarding_skip").assertExists()
    }

    @Test
    fun backButtonIsHiddenWhenThereIsNothingToGoBackTo() {
        composeRule.setContent { Screen(OnboardingSteps.NOTIFICATIONS, showBack = false) }
        composeRule.onNodeWithTag("onboarding_back").assertDoesNotExist()
        composeRule.onNodeWithTag("onboarding_next").assertIsDisplayed()
    }

    @Test
    fun buttonsInvokeTheirCallbacks() {
        var back = 0
        var next = 0
        var skip = 0
        composeRule.setContent {
            Screen(OnboardingSteps.WEIGHT, onBack = { back++ }, onNext = { next++ }, onSkip = { skip++ })
        }

        composeRule.onNodeWithTag("onboarding_back").performClick()
        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule.onNodeWithTag("onboarding_skip").performClick()
        composeRule.runOnIdle {
            assertEquals(1, back)
            assertEquals(1, next)
            assertEquals(1, skip)
        }
    }

    @Test
    fun nextIsInertWhileLoadingAndSkipIsIgnored() {
        var next = 0
        var skip = 0
        composeRule.setContent {
            Screen(OnboardingSteps.GENDER, nextLoading = true, onNext = { next++ }, onSkip = { skip++ })
        }

        composeRule.onNodeWithTag("onboarding_next").assertIsNotEnabled()
        composeRule.onNodeWithTag("onboarding_skip").performClick()
        composeRule.runOnIdle {
            assertEquals(0, next)
            assertEquals(0, skip)
        }
    }
}
