package com.kamleonapp.activities

import android.content.Intent
import android.text.Layout.Alignment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.widget.AppCompatSpinner
import com.kamleonapp.R
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityOnboardingBinding

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {
    public enum class OnBoardingStep(val step: Int) {
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

    private fun spinnerViewFor(step: OnBoardingStep): AppCompatSpinner {
        if (step == OnBoardingStep.Height) {
            return binding.spinnerHeight
        } else if (step == OnBoardingStep.Weight) {
            return binding.spinnerWeight
        } else if (step == OnBoardingStep.Gender) {
            return binding.spinnerGender
        }

        return binding.spinnerHeight
    }

    override fun initView() {
        binding.progressStep.max = OnBoardingStep.values().size
        updateUI()

        setupSpinners()
    }

    override fun initEvent() {
        binding.btnNext.setOnClickListener {
            if (state.step == OnBoardingStep.values().size - 1) {
                startActivity(Intent(this, AnalyticActivity::class.java))
            } else {
                state = OnBoardingStep.values()[state.step + 1]
            }

            updateUI()
        }

        binding.btnNavBack.setOnClickListener {
            if (state.step == 0) {
                finish()
            } else {
                state = OnBoardingStep.values()[state.step - 1]
            }

            updateUI()
        }
    }

    private fun updateUI() {
        binding.progressStep.progress = state.step + 1
        for (stepCase in OnBoardingStep.values()) {
            containerLayoutFor(stepCase).visibility = if (state == stepCase) View.VISIBLE else View.GONE
        }

        binding.tvBottomDesc.visibility = if (state.step < 2) View.VISIBLE else View.INVISIBLE
    }

    private fun spinnerDataSource(step: OnBoardingStep) : List<String> {
        val  aryRet = ArrayList<String>()
        if (step == OnBoardingStep.Height) {
            for (height in 150 .. 190) {
                aryRet.add("$height cm")
            }
        } else if (step == OnBoardingStep.Weight) {
            for (weight in 40 .. 120) {
                aryRet.add("$weight kg")
            }
        } else if (step == OnBoardingStep.Gender) {
            val arrRes = resources.getStringArray(R.array.onboard_gender_values)
            aryRet.addAll(arrRes)
        }

        return aryRet
    }

    private fun setupSpinners() {
        for (onboardState in OnBoardingStep.values()) {
            if (onboardState.step <= 2) { continue }
            val spinner = spinnerViewFor(onboardState)

            Log.e("SPINNER", "Datasource size = " + spinnerDataSource(onboardState).size)
            var aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerDataSource(onboardState))
            aa?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinner?.adapter = aa
//        spinner?.onItemSelectedListener = this
//            spinner?.textAlignment = Te
            spinner?.gravity = Gravity.CENTER
        }
    }
}