package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.databinding.ActivityOnboardingBinding
import com.dynatech2012.kamleonuserapp.extensions.addYears
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import com.ozcanalasalvar.datepicker.view.datapicker.DataPicker
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.GregorianCalendar

@AndroidEntryPoint
class OnboardingFragment : BaseFragment<ActivityOnboardingBinding>() {
    private val viewModel: AuthViewModel by activityViewModels()

    enum class OnBoardingStep(val step: Int) {
        BirthDate(0),
        Notification(1),

        //Location(1),
        Height(2),
        Weight(3),
        Gender(4);
    }

    override fun setBinding(): ActivityOnboardingBinding =
        ActivityOnboardingBinding.inflate(layoutInflater)

    private var state: OnBoardingStep = OnBoardingStep.BirthDate
    private fun containerLayoutFor(step: OnBoardingStep): LinearLayout {
        val layouts = arrayOf(
            binding.layoutBirthday,
            binding.layoutNotification,
            //binding.layoutLocation,
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

        val fourteenYearsAgo = Date().addYears(-14).time
        binding.datePicker.setDate(fourteenYearsAgo)
        binding.datePicker.setMaxxDate(fourteenYearsAgo)

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
                    activity?.supportFragmentManager?.setFragmentResult(
                        Constants.GRANT_NOTIFICATION,
                        Bundle().apply {
                            putBoolean(Constants.GRANT_NOTIFICATION_BUNDLE, true)
                        })
                }
                /*
                OnBoardingStep.Location -> {
                    activity?.supportFragmentManager?.setFragmentResult(Constants.GRANT_LOCATION, Bundle().apply {
                        putBoolean(Constants.GRANT_LOCATION_BUNDLE, true)
                    })
                }*/
                OnBoardingStep.BirthDate -> {
                    val dateSelected = binding.datePicker.getDateSelected()
                    //val localDate = LocalDate.of(dateSelected.year, dateSelected.month, dateSelected.day)
                    val date = GregorianCalendar(
                        dateSelected.year,
                        dateSelected.month - 1,
                        dateSelected.day
                    ).time
                    viewModel.birthday = date
                    viewModel.signup()
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
            }
            checkIfGoNextStep()
        }

        binding.btnNavBack.setOnClickListener {
            Log.i(TAG, "Back button clicked with step: ${state.step}")
            if (state.step == 0) {
                findNavController().popBackStack()
            } else {
                state = OnBoardingStep.values()[state.step - 1]
            }

            updateUI()
        }

        viewModel.uiState.observe(this, this::startActivity)

    }

    private fun startActivity(state: Int) {
        Log.d(
            PrivacyFragment.TAG,
            "register Callback privacy - $state"
        ) //TODO display user errors and navigate to register fragment to display them and the user to fix them
        if (state == 3) {
            checkIfGoNextStep()
        } else if (state == -1) {
            //show snackbar with error
            Snackbar.make(binding.root, R.string.signup_error_server_error, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun checkIfGoNextStep() {
        when (state.step) {
            OnBoardingStep.values().size - 1 -> {
                binding.btnNext.text = ""
                binding.pbOnboardNext.visibility = View.VISIBLE
                viewModel.finishSignup()
            }

            OnBoardingStep.Notification.step/*, OnBoardingStep.Location.step*/ -> {
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

        if (state.step == 0 || state.step == 1) {
            //binding.progressStep.visibility = View.INVISIBLE
            binding.tvBottomDesc.text = getString(R.string.onboard_bottom_text)
            binding.tvBottomDesc.visibility = View.VISIBLE
            binding.tvBottomDesc.isClickable = false
            binding.tvBottomDesc.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.kamleon_secondary_grey_60
                )
            )
        } else if (state.step > 1) {
            binding.tvBottomDesc.text = getString(R.string.onboard_bottom_later)
            binding.tvBottomDesc.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.kamleon_blue
                )
            )
            binding.tvBottomDesc.isClickable = true
            binding.tvBottomDesc.setOnClickListener {
                binding.tvBottomDesc.visibility = View.INVISIBLE
                binding.pbOnboardSkip.visibility = View.VISIBLE
                binding.btnNext.isEnabled = false
                binding.tvBottomDesc.isEnabled = false
                if (state.step == 2 || state.step == 3) {
                    if(state.step == 2) {
                        viewModel.height = 0f
                    } else {
                        viewModel.weight = 0f
                    }
                    checkIfGoNextStep()
                } else {
                    viewModel.finishSignup()
                }
            }
            binding.tvBottomDesc.visibility = View.VISIBLE
        }
        binding.progressStep.progress = state.step + 1

        for (stepCase in OnBoardingStep.values()) {
            containerLayoutFor(stepCase).visibility =
                if (state == stepCase) View.VISIBLE else View.GONE
        }

    }

    private fun spinnerDataSource(step: OnBoardingStep): ArrayList<String> {
        val aryRet = ArrayList<String>()
        when (step) {
            OnBoardingStep.Height -> {
                for (height in 140..230) {
                    aryRet.add("$height")
                }
            }

            OnBoardingStep.Weight -> {
                for (weight in 30..130) {
                    aryRet.add("$weight")
                }
            }

            OnBoardingStep.Gender -> {
                val arrRes = Gender.values().map { it.raw }
                aryRet.addAll(arrRes)
            }

            else -> {}
        }
        Log.d(TAG, "spinner data source: $aryRet")

        return aryRet
    }

    private fun setupSpinners() {
        for (onboardState in OnBoardingStep.values()) {
            if (onboardState.step <= OnBoardingStep.BirthDate.step) {
                continue
            }
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
        Log.d(TAG, "login step state received $state")
        when (state) {
            4 -> {
                goNextStep(); updateUI()
            }

            5 -> {
                binding.pbOnboardNext.visibility = View.GONE
                binding.pbOnboardSkip.visibility = View.GONE
                binding.btnNext.text = getString(R.string.onboard_button_next)
                binding.tvBottomDesc.visibility = View.VISIBLE
                showVerificationDialog()
            }

            else -> {}
        }
    }

    private fun showVerificationDialog() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_dialog_email_verification, null)

        dialog.setView(dialogView)
        dialog.setCancelable(false)

        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_desc).text =
            getString(R.string.dialog_verify_desc, viewModel.email)
        val logoutDialog = dialog.show()
        logoutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        Log.d(TAG, "login step show verif dialog")
        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_ok).setOnClickListener {
            Log.d(AuthViewModel.TAG, "login step verif dialog ok")
            // Not necessary. Only if navigation fails
            binding.btnNext.isEnabled = true
            binding.tvBottomDesc.isEnabled = true

            logoutDialog.dismiss()
            // Go to login
            findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
        }
        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_resend).setOnClickListener {
            logoutDialog.dismiss()
            viewModel.sendVerificationEmail()
        }
    }

    companion object {
        val TAG = PrivacyFragment::class.simpleName
    }
}