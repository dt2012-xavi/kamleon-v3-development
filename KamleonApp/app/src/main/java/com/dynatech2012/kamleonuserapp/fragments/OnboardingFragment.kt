package com.dynatech2012.kamleonuserapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.InitActivity
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.extensions.addYears
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.google.android.material.snackbar.Snackbar
import com.ozcanalasalvar.datepicker.model.Date as PickerDate
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

/**
 * Post-registration questionnaire: birth date, notifications, height, weight and gender. The
 * account is created when the birth date step is confirmed; the profile is completed on the last
 * step (or when it is skipped).
 */
@AndroidEntryPoint
class OnboardingFragment : Fragment() {
    private val viewModel: AuthViewModel by activityViewModels()

    private var step by mutableIntStateOf(OnboardingSteps.BIRTH_DATE)
    private var nextLoading by mutableStateOf(false)
    private var pickerKey by mutableIntStateOf(0)

    // Users must meet the minimum age: that is both the default and the latest allowed date.
    private val maxBirthDate = PickerDate(Date().addYears(-OnboardingSteps.MIN_AGE).time)
    private val birthYearsRange =
        1904..Calendar.getInstance().apply { time = Date().addYears(-OnboardingSteps.MIN_AGE) }.get(Calendar.YEAR)

    // Latest picker selections. Plain fields: they are only read when a step is (re)composed or
    // when "Next" is pressed.
    private var birthDate = maxBirthDate
    // Start at typical values rather than at the ends of the wheels, so pressing Next without
    // touching them does not record 140 cm / 30 kg. Gender starts on "Prefer not to say" so that
    // nothing is recorded about the user that they did not choose.
    private var selectedHeight = DEFAULT_HEIGHT
    private var selectedWeight = DEFAULT_WEIGHT
    private var selectedGender = DEFAULT_GENDER

    private val heightValues = (140..230).map { "$it" }
    private val weightValues = (30..130).map { "$it" }
    private val genderValues = Gender.values().map { it.raw }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            step = it.getInt(KEY_STEP, OnboardingSteps.BIRTH_DATE)
            if (it.containsKey(KEY_BIRTH_MILLIS)) birthDate = PickerDate(it.getLong(KEY_BIRTH_MILLIS))
            selectedHeight = it.getString(KEY_HEIGHT, DEFAULT_HEIGHT)
            selectedWeight = it.getString(KEY_WEIGHT, DEFAULT_WEIGHT)
            selectedGender = it.getString(KEY_GENDER, DEFAULT_GENDER)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STEP, step)
        outState.putLong(KEY_BIRTH_MILLIS, birthDateMillis())
        outState.putString(KEY_HEIGHT, selectedHeight)
        outState.putString(KEY_WEIGHT, selectedWeight)
        outState.putString(KEY_GENDER, selectedGender)
    }

    private fun birthDateMillis(): Long =
        GregorianCalendar(birthDate.year, birthDate.month - 1, birthDate.day).timeInMillis

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            AppTheme(useDarkTheme = false) {
                OnboardingScreen(
                    step = step,
                    nextLoading = nextLoading,
                    showBack = canGoBack,
                    pickerKey = pickerKey,
                    birthDate = birthDate,
                    birthYearsRange = birthYearsRange,
                    maxBirthDate = maxBirthDate,
                    heightValues = heightValues,
                    weightValues = weightValues,
                    genderValues = genderValues,
                    selectedHeight = selectedHeight,
                    selectedWeight = selectedWeight,
                    selectedGender = selectedGender,
                    onBirthDateChange = { _, _, _, millis -> birthDate = PickerDate(millis) },
                    onHeightChange = { selectedHeight = it },
                    onWeightChange = { selectedWeight = it },
                    onGenderChange = { selectedGender = it },
                    onBack = ::onBack,
                    onNext = ::onNext,
                    onSkip = ::onSkip
                )
            }
        }
        onKamleonSheetResult("onboarding_verify") {
            when (it) {
                SheetResult.PRIMARY -> {
                    Timber.d("Verification sheet acknowledged")
                    step = OnboardingSteps.BIRTH_DATE
                    resetView()
                    findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
                }
                SheetResult.SECONDARY -> viewModel.sendVerificationEmail()
                SheetResult.DISMISSED -> Unit
            }
        }
        viewModel.uiState.observe(viewLifecycleOwner, this::onStateReceived)

        // The system Back button goes to the previous step; from the first step it leaves as usual.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // The account or profile is being saved, or it already exists and there is
                    // nothing to go back to: the system Back button does nothing.
                    if (nextLoading || !canGoBack) return
                    if (step > OnboardingSteps.BIRTH_DATE) {
                        step -= 1
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    /**
     * Once the account exists the registration steps (Register, Privacy, birth date) are behind the
     * user: Back is only available between the optional steps that follow.
     */
    private val canGoBack: Boolean
        get() = !(viewModel.profileCreated && step <= OnboardingSteps.NOTIFICATIONS)

    private fun onBack() {
        Timber.d("Onboarding back requested at step: %s", step)
        if (nextLoading || !canGoBack) return
        if (step == OnboardingSteps.BIRTH_DATE) {
            findNavController().popBackStack()
        } else {
            step -= 1
        }
    }

    private fun onNext() {
        if (nextLoading) return
        when (step) {
            OnboardingSteps.NOTIFICATIONS -> {
                // The system dialog is up until state 4 arrives: keep the button inert meanwhile.
                nextLoading = true
                activity?.supportFragmentManager?.setFragmentResult(
                    Constants.GRANT_NOTIFICATION,
                    Bundle().apply { putBoolean(Constants.GRANT_NOTIFICATION_BUNDLE, true) }
                )
            }

            OnboardingSteps.BIRTH_DATE -> {
                viewModel.birthday = Date(birthDateMillis())
                // The account is created here. Stay on this step (with the button spinning) until
                // the result arrives: state 2 moves on, an error leaves the user here to retry.
                nextLoading = true
                // Coming back to this step after the account exists: only the date changes.
                if (viewModel.profileCreated) viewModel.updateBirthDate() else viewModel.signup()
                return
            }

            OnboardingSteps.HEIGHT -> {
                Timber.d("Height step completed")
                selectedHeight.toFloatOrNull()?.let { viewModel.height = it }
            }

            OnboardingSteps.WEIGHT -> {
                Timber.d("Weight step completed")
                selectedWeight.toFloatOrNull()?.let { viewModel.weight = it }
            }

            OnboardingSteps.GENDER -> {
                Timber.d("Gender step completed")
                viewModel.gender = Gender.fromRaw(selectedGender)
            }
        }
        checkIfGoNextStep()
    }

    private fun onSkip() {
        Timber.d("Skipping onboarding step: %s", step)
        when (step) {
            // Optional data: skipping leaves it empty (null), not a made-up 0.
            OnboardingSteps.HEIGHT -> viewModel.height = null
            OnboardingSteps.WEIGHT -> viewModel.weight = null
            OnboardingSteps.GENDER -> viewModel.gender = Gender.none
        }
        checkIfGoNextStep()
    }

    private fun checkIfGoNextStep() {
        when (step) {
            OnboardingSteps.COUNT - 1 -> {
                Timber.d("Finishing registration profile")
                nextLoading = true
                viewModel.finishSignup()
            }

            // Wait for the permission result (state 4) before moving on.
            OnboardingSteps.NOTIFICATIONS -> Timber.d("Awaiting notification permission result")

            else -> {
                Timber.d("Advancing onboarding step")
                goNextStep()
            }
        }
    }

    private fun goNextStep() {
        step += 1
    }

    private fun resetView() {
        birthDate = maxBirthDate
        selectedHeight = DEFAULT_HEIGHT
        selectedWeight = DEFAULT_WEIGHT
        selectedGender = DEFAULT_GENDER
        pickerKey += 1
        viewModel.gender = Gender.none
        viewModel.height = null
        viewModel.weight = null
        viewModel.birthday = Date()
    }

    private fun onStateReceived(state: Int) {
        Timber.d("Onboarding observed registration state: %s", state)
        when (state) {
            // "Verification email sent". It must not move the questionnaire along: it can arrive
            // while the user is on any step. After a resend from the final dialog it re-runs the
            // finishing step, which shows the confirmation dialog again.
            3 -> if (step == OnboardingSteps.COUNT - 1) checkIfGoNextStep()

            // Profile document created: the account exists, so the questionnaire can continue.
            2 -> if (step == OnboardingSteps.BIRTH_DATE && nextLoading) {
                nextLoading = false
                goNextStep()
            }

            // The LiveData replays its last value when the view is recreated, so each state is
            // only acted on while the step that is waiting for it is on screen.
            4 -> if (step == OnboardingSteps.NOTIFICATIONS) {
                nextLoading = false
                goNextStep()
            }

            5 -> if (nextLoading) {
                nextLoading = false
                showVerificationDialog()
            }

            7 -> (requireActivity() as InitActivity).openMain()

            -7 -> {
                if (findNavController().currentDestination?.id == R.id.onboardingFragment) {
                    findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
                }
            }

            -1, -6 -> {
                nextLoading = false
                Snackbar.make(
                    requireView(),
                    if (state == -6) R.string.startup_network_error else R.string.signup_error_server_error,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showVerificationDialog() {
        Timber.d("Showing verification email sheet")
        childFragmentManager.showKamleonSheet(
            "onboarding_verify",
            SheetSpec(
                title = getString(R.string.dialog_verify_title),
                message = getString(R.string.dialog_verify_desc, viewModel.email),
                primary = getString(R.string.dialog_ok),
                secondary = getText(R.string.dialog_verify_resend),
                dismissible = false
            )
        )
    }

    private companion object {
        // Same "value.decimal" format the wheel pickers report.
        const val DEFAULT_HEIGHT = "170.0"
        const val DEFAULT_WEIGHT = "70.0"
        val DEFAULT_GENDER = Gender.none.raw
        const val KEY_STEP = "onboarding_step"
        const val KEY_BIRTH_MILLIS = "onboarding_birth_millis"
        const val KEY_HEIGHT = "onboarding_height"
        const val KEY_WEIGHT = "onboarding_weight"
        const val KEY_GENDER = "onboarding_gender"
    }
}
