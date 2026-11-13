package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.drawable.toDrawable
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

    // Users must be at least 14 years old: that is both the default and the latest allowed date.
    private val maxBirthDate = PickerDate(Date().addYears(-14).time)
    private val birthYearsRange = 1904..Calendar.getInstance().apply { time = Date().addYears(-14) }.get(Calendar.YEAR)

    // Latest picker selections. Plain fields: they are only read when a step is (re)composed or
    // when "Next" is pressed.
    private var birthDate = maxBirthDate
    private var selectedHeight = ""
    private var selectedWeight = ""
    private var selectedGender = ""

    private val heightValues = (140..230).map { "$it" }
    private val weightValues = (30..130).map { "$it" }
    private val genderValues = Gender.values().map { it.raw }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { step = it.getInt(KEY_STEP, OnboardingSteps.BIRTH_DATE) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STEP, step)
    }

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
        viewModel.uiState.observe(viewLifecycleOwner, this::onStateReceived)
    }

    private fun onBack() {
        Timber.d("Onboarding back requested at step: %s", step)
        if (step == OnboardingSteps.BIRTH_DATE) {
            findNavController().popBackStack()
        } else {
            step -= 1
        }
    }

    private fun onNext() {
        when (step) {
            OnboardingSteps.NOTIFICATIONS -> {
                activity?.supportFragmentManager?.setFragmentResult(
                    Constants.GRANT_NOTIFICATION,
                    Bundle().apply { putBoolean(Constants.GRANT_NOTIFICATION_BUNDLE, true) }
                )
            }

            OnboardingSteps.BIRTH_DATE -> {
                viewModel.birthday = GregorianCalendar(birthDate.year, birthDate.month - 1, birthDate.day).time
                viewModel.signup()
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
            OnboardingSteps.HEIGHT -> viewModel.height = 0f
            OnboardingSteps.WEIGHT -> viewModel.weight = 0f
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
        selectedHeight = ""
        selectedWeight = ""
        selectedGender = ""
        pickerKey += 1
        viewModel.gender = Gender.none
        viewModel.height = 0f
        viewModel.weight = 0f
        viewModel.birthday = Date()
    }

    private fun onStateReceived(state: Int) {
        Timber.d("Onboarding observed registration state: %s", state)
        when (state) {
            3 -> checkIfGoNextStep()

            4 -> goNextStep()

            5 -> {
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
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_email_verification, null)
        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_desc).text =
            getString(R.string.dialog_verify_desc, viewModel.email)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .show()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        Timber.d("Showing verification email dialog")
        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_ok).setOnClickListener {
            Timber.d("Verification dialog acknowledged")
            step = OnboardingSteps.BIRTH_DATE
            resetView()
            dialog.dismiss()
            findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
        }
        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_resend).setOnClickListener {
            dialog.dismiss()
            viewModel.sendVerificationEmail()
        }
    }

    private companion object {
        const val KEY_STEP = "onboarding_step"
    }
}
