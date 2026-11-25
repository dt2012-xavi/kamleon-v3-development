package com.dynatech2012.kamleonuserapp.fragments

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.application.MatTheme
import com.dynatech2012.kamleonuserapp.views.KamleonProgressBar
import com.dynatech2012.kamleonuserapp.views.KamleonRoundButton
import com.dynatech2012.kamleonuserapp.views.KamleonTextLink
import com.dynatech2012.kamleonuserapp.views.kamleonBody
import com.dynatech2012.kamleonuserapp.views.kamleonBodySmall
import com.dynatech2012.kamleonuserapp.views.kamleonBodyBold
import com.dynatech2012.kamleonuserapp.views.kamleonCaption
import com.dynatech2012.kamleonuserapp.views.kamleonTitle
import com.ozcanalasalvar.datepicker.compose.datapicker.WheelDataPicker
import com.ozcanalasalvar.datepicker.compose.datepicker.WheelDatePicker
import com.ozcanalasalvar.datepicker.model.Date as PickerDate
import java.util.Calendar

/** Steps of the post-registration profile questionnaire, in display order. */
object OnboardingSteps {
    const val BIRTH_DATE = 0
    const val NOTIFICATIONS = 1
    const val HEIGHT = 2
    const val WEIGHT = 3
    const val GENDER = 4
    const val COUNT = 5

    /** Minimum age to use the app: also the default and the latest selectable birth date. */
    const val MIN_AGE = 14
}

/**
 * Compose version of `activity_onboarding.xml`. Layout margins are the XML's literal values (back
 * button and progress bar on one row, 44dp side padding and 40dp bottom padding for the buttons,
 * 36dp + 18dp around the step content).
 *
 * The screen is stateless: the current [step] and the picker selections are owned by the fragment
 * and reported through the `on…Change` callbacks. [pickerKey] changes to rebuild the pickers.
 */
@Composable
fun OnboardingScreen(
    step: Int,
    nextLoading: Boolean,
    pickerKey: Int,
    birthDate: PickerDate,
    birthYearsRange: IntRange,
    maxBirthDate: PickerDate,
    heightValues: List<String>,
    weightValues: List<String>,
    genderValues: List<String>,
    selectedHeight: String,
    selectedWeight: String,
    selectedGender: String,
    onBirthDateChange: (day: Int, month: Int, year: Int, millis: Long) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    showBack: Boolean = true
) {
    val darkGrey = colorResource(R.color.kamleon_dark_grey)

    Box(Modifier.fillMaxSize().background(colorResource(R.color.bg_color_activity))) {
        // Back button (48dp, 16dp from the top and start) and the progress bar centered on it.
        val backTop = dimensionResource(R.dimen.nav_back_button_top)
        val backSize = dimensionResource(R.dimen.nav_back_button_size)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = backTop)
                .padding(horizontal = 86.dp)
                .align(Alignment.TopStart)
                .height(backSize)
        ) {
            KamleonProgressBar(
                progress = (step + 1f) / OnboardingSteps.COUNT,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // Hidden once the account exists and the user is on the first optional step.
        if (showBack) Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = dimensionResource(R.dimen.nav_back_button_left), top = backTop)
                .size(backSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onBack
                )
                .testTag("onboarding_back"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.icn_nav_back),
                contentDescription = null,
                contentScale = ContentScale.None
            )
        }

        Column(Modifier.fillMaxSize()) {
            // Step content: below the back button row, 36dp / 20dp padding, 18dp inside each step.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = backTop + backSize)
                    .padding(horizontal = 36.dp, vertical = 20.dp)
            ) {
                // The wheel pickers draw their labels with the default font; give them Circular.
                ProvideTextStyle(TextStyle(fontFamily = MatTheme.fontFamily)) {
                    // Fade through between steps: the old one leaves quickly, the new one comes in.
                    // Inside, use [shownStep] (not [step]) so the outgoing content keeps its own.
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "onboarding-step"
                    ) { shownStep ->
                    androidx.compose.runtime.key(pickerKey) {
                        when (shownStep) {
                            OnboardingSteps.BIRTH_DATE -> PickerStep(shownStep, R.string.onboard_birthday_title, stringResource(R.string.onboard_why_birthday, OnboardingSteps.MIN_AGE)) {
                                WheelDatePicker(
                                    offset = 1,
                                    yearsRange = birthYearsRange,
                                    startDate = birthDate,
                                    maxDate = maxBirthDate,
                                    textSize = 24,
                                    textBold = false,
                                    selectorEffectEnabled = true,
                                    darkModeEnabled = false,
                                    onDateChanged = onBirthDateChange
                                )
                            }
                            OnboardingSteps.NOTIFICATIONS -> NotificationStep(shownStep, darkGrey)
                            OnboardingSteps.HEIGHT -> PickerStep(shownStep, R.string.onboard_height_title, stringResource(R.string.onboard_why_height)) {
                                WheelDataPicker(
                                    offset = 1,
                                    valueUnit = "cm",
                                    startValue = selectedHeight,
                                    // Whole centimetres, as in Settings.
                                    showDecimal = false,
                                    values = heightValues,
                                    textSize = 28,
                                    textBold = true,
                                    darkModeEnabled = false,
                                    valueWidth = 74,
                                    onValueChanged = { value, _ -> onHeightChange(value) }
                                )
                            }
                            OnboardingSteps.WEIGHT -> PickerStep(shownStep, R.string.onboard_weight_title, stringResource(R.string.onboard_why_weight)) {
                                WheelDataPicker(
                                    offset = 1,
                                    valueUnit = "kg",
                                    startValue = selectedWeight,
                                    showDecimal = true,
                                    values = weightValues,
                                    textSize = 26,
                                    textBold = true,
                                    darkModeEnabled = false,
                                    valueWidth = 50,
                                    onValueChanged = { value, _ -> onWeightChange(value) }
                                )
                            }
                            OnboardingSteps.GENDER -> PickerStep(shownStep, R.string.onboard_gender_title, stringResource(R.string.onboard_why_gender)) {
                                WheelDataPicker(
                                    offset = 1,
                                    startValue = selectedGender,
                                    values = genderValues,
                                    textSize = 28,
                                    textBold = true,
                                    darkModeEnabled = false,
                                    valueWidth = 250,
                                    onValueChanged = { value, _ -> onGenderChange(value) }
                                )
                            }
                        }
                    }
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 44.dp).padding(bottom = 40.dp)) {
                KamleonRoundButton(
                    text = stringResource(R.string.onboard_button_next),
                    onClick = onNext,
                    enabled = true,
                    loading = nextLoading,
                    testTag = "onboarding_next"
                )
                Box(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (step <= OnboardingSteps.NOTIFICATIONS) {
                        Text(
                            text = stringResource(R.string.onboard_bottom_text),
                            style = kamleonBodySmall(),
                            color = colorResource(R.color.kamleon_secondary_grey_60)
                        )
                    } else {
                        KamleonTextLink(
                            text = stringResource(R.string.onboard_bottom_later),
                            onClick = { if (!nextLoading) onSkip() },
                            style = kamleonBodySmall(),
                            color = colorResource(R.color.kamleon_blue),
                            pressedColor = colorResource(R.color.kamleon_blue_pressed),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.testTag("onboarding_skip")
                        )
                    }
                }
            }
        }
    }
}

/** "Step X of 5" caption shown above every question. */
@Composable
private fun StepCounter(step: Int) {
    Text(
        text = stringResource(R.string.onboard_step_counter, step + 1, OnboardingSteps.COUNT),
        style = kamleonCaption(),
        color = colorResource(R.color.kamleon_secondary_grey_60),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * A question step: step counter, centered title, a short reason for asking, and the picker
 * centered in the remaining space.
 */
@Composable
private fun PickerStep(
    step: Int,
    @StringRes title: Int,
    why: String,
    picker: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(bottom = 80.dp)) {
        StepCounter(step)
        Text(
            text = stringResource(title),
            style = kamleonTitle(),
            color = colorResource(R.color.kamleon_dark_grey),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Text(
            text = why,
            style = kamleonBodySmall(),
            color = colorResource(R.color.kamleon_secondary_grey_60),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp),
            contentAlignment = Alignment.Center
        ) { picker() }
    }
}

@Composable
private fun NotificationStep(step: Int, darkGrey: androidx.compose.ui.graphics.Color) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        StepCounter(step)
        Text(
            text = stringResource(R.string.onboard_noti_title),
            style = kamleonTitle(),
            color = darkGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Text(
            text = stringResource(R.string.onboard_noti_sub1),
            style = kamleonBodyBold(),
            color = darkGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        )
        Text(
            text = stringResource(R.string.onboard_noti_sub2),
            style = kamleonBody(),
            color = darkGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun OnboardingNotificationPreview() {
    AppTheme(useDarkTheme = false) {
        OnboardingScreen(
            step = OnboardingSteps.NOTIFICATIONS, nextLoading = false, pickerKey = 0,
            birthDate = PickerDate(Calendar.getInstance().timeInMillis),
            birthYearsRange = 1904..2012, maxBirthDate = PickerDate(Calendar.getInstance().timeInMillis),
            heightValues = emptyList(), weightValues = emptyList(), genderValues = emptyList(),
            selectedHeight = "", selectedWeight = "", selectedGender = "",
            onBirthDateChange = { _, _, _, _ -> }, onHeightChange = {}, onWeightChange = {},
            onGenderChange = {}, onBack = {}, onNext = {}, onSkip = {}
        )
    }
}
