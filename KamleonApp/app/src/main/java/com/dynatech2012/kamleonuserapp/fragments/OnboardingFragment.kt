package com.dynatech2012.kamleonuserapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import com.dynatech2012.kamleonuserapp.activities.MainActivity
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.databinding.ActivityOnboardingBinding
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.ozcanalasalvar.datepicker.view.datapicker.DataPicker
import dagger.hilt.android.AndroidEntryPoint
import java.util.GregorianCalendar

@AndroidEntryPoint
class OnboardingFragment : BaseFragment<ActivityOnboardingBinding>() {
    private val viewModel: AuthViewModel by activityViewModels()

    enum class OnBoardingStep(val step: Int) {
        Notification(0),
        Location(1),
        BirthDate(2),
        Height(3),
        Weight(4),
        Gender(5);
    }

    override fun setBinding(): ActivityOnboardingBinding = ActivityOnboardingBinding.inflate(layoutInflater)

    private var state: OnBoardingStep = OnBoardingStep.Notification
    private fun containerLayoutFor(step: OnBoardingStep): LinearLayout {
        val layouts = arrayOf(
            binding.layoutNotification,
            binding.layoutLocation,
            binding.layoutBirthday,
            binding.layoutHeight,
            binding.layoutWeight,
            binding.layoutGender,
        )

        return layouts[step.step]
    }

    private fun spinnerViewFor(step: OnBoardingStep): DataPicker {
        return when (step) {
            OnBoardingStep.Height -> {
                binding.heightPicker
            }
            OnBoardingStep.Weight -> {
                binding.weightPicker
            }
            OnBoardingStep.Gender -> {
                binding.genderPicker
            }
            else -> binding.heightPicker
        }
    }

    override fun initView() {
        binding.progressStep.max = OnBoardingStep.values().size
        binding.weightPicker.setValueUnit("kg")
        binding.weightPicker.setValueWidth(50)
        binding.weightPicker.setShowDecimal(true)

        binding.heightPicker.setValueUnit("cm")
        binding.heightPicker.setValueWidth(74)
        binding.heightPicker.setShowDecimal(true)

        updateUI()
        setupSpinners()
        initObservers()
    }

    override fun initEvent() {
        binding.btnNext.setOnClickListener {
            when (state) {
                OnBoardingStep.Notification -> {
                    activity?.supportFragmentManager?.setFragmentResult(Constants.GRANT_NOTIFICATION, Bundle().apply {
                        putBoolean(Constants.GRANT_NOTIFICATION_BUNDLE, true)
                    })
                }
                OnBoardingStep.Location -> {
                    activity?.supportFragmentManager?.setFragmentResult(Constants.GRANT_LOCATION, Bundle().apply {
                        putBoolean(Constants.GRANT_LOCATION_BUNDLE, true)
                    })
                }
                OnBoardingStep.BirthDate -> {
                    val dateSelected = binding.datePicker.getDateSelected()
                    //val localDate = LocalDate.of(dateSelected.year, dateSelected.month, dateSelected.day)
                    val date = GregorianCalendar(dateSelected.year, dateSelected.month - 1, dateSelected.day).time
                    viewModel.birthday = date
                }
                OnBoardingStep.Height -> {
                    Log.d(TAG, "picker height: ${binding.heightPicker.getSelectedValue()}")
                    binding.heightPicker.getSelectedValue()?.let {
                        viewModel.height = it.toFloat()
                    }
                }
                OnBoardingStep.Weight -> {
                    Log.d(TAG, "picker weight: ${binding.weightPicker.getSelectedValue()}")
                    binding.weightPicker.getSelectedValue()?.let {
                        viewModel.weight = it.toFloat()
                    }
                }
                OnBoardingStep.Gender -> {
                    Log.d(TAG, "picker gender: ${binding.genderPicker.getSelectedValue()}")
                    val genderPicked = binding.genderPicker.getSelectedValue()
                    val gender = Gender.fromRaw(genderPicked)
                    viewModel.gender = gender
                }
                else -> {}
            }
            checkIfGoNextStep()
        }

        binding.btnNavBack.setOnClickListener {
            if (state.step == 0) {
                activity?.finish()
            } else {
                state = OnBoardingStep.values()[state.step - 1]
            }

            updateUI()
        }
    }

    private fun checkIfGoNextStep() {
        when (state.step) {
            OnBoardingStep.values().size - 1 -> {
                viewModel.finishSignup()
            }
            OnBoardingStep.Notification.step, OnBoardingStep.Location.step -> {
                // Do nothing
            }
            else -> {
                goNextStep()
            }
        }
        updateUI()
    }
    private fun goNextStep() {
        state = OnBoardingStep.values()[state.step + 1]
    }

    private fun updateUI() {
        binding.progressStep.progress = state.step + 1
        for (stepCase in OnBoardingStep.values()) {
            containerLayoutFor(stepCase).visibility = if (state == stepCase) View.VISIBLE else View.GONE
        }

        binding.tvBottomDesc.visibility = if (state.step < 2) View.VISIBLE else View.INVISIBLE
    }

    private fun spinnerDataSource(step: OnBoardingStep) : ArrayList<String> {
        val aryRet = ArrayList<String>()
        when (step) {
            OnBoardingStep.Height -> {
                for (height in 160 .. 190) {
                    aryRet.add("$height")
                }
            }
            OnBoardingStep.Weight -> {
                for (weight in 45 .. 120) {
                    aryRet.add("$weight")
                }
            }
            OnBoardingStep.Gender -> {
                val arrRes = Gender.values().map { it.raw }
                aryRet.addAll(arrRes)
            }
            else -> {}
        }

        return aryRet
    }

    private fun setupSpinners() {
        for (onboardState in OnBoardingStep.values()) {
            if (onboardState.step <= 2) { continue }
            val picker = spinnerViewFor(onboardState)

            Log.e("SPINNER", "Datasource size = " + spinnerDataSource(onboardState).size)
            picker.setValues(spinnerDataSource(onboardState))
        }
    }

    private fun initObservers() {
        viewModel.uiState.observe(this, this::onStateReceived)
    }

    private fun onStateReceived(state: Int) {
        Log.d(TAG, "Callback new state received: $state")
        when (state) {
            3, 4 -> { goNextStep(); updateUI() }
            5 -> startActivity(Intent(requireContext(), MainActivity::class.java))
            else -> {}
        }
    }

    companion object {
        val TAG = PrivacyFragment::class.simpleName
    }
}