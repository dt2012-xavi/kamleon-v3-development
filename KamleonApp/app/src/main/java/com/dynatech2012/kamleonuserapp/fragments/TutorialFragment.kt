package com.dynatech2012.kamleonuserapp.fragments

import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.FragmentTutorialBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TutorialFragment : BaseFragment<FragmentTutorialBinding>() {
    override fun setBinding(): FragmentTutorialBinding = FragmentTutorialBinding.inflate(layoutInflater)

    enum class OnBoardingStep(val step: Int) {
        Hydration(0),
        Recommendation(1),
        Metrics(2),
        GraphicTime(3),
        GraphicStreak(4),
        Scanner(5),
    }
    private var state: OnBoardingStep = OnBoardingStep.Hydration

    override fun initView() {
        binding.tvTutorialStep.text = getString(R.string.onboard_button_step, state.step + 1)
    }

    override fun initEvent() {
        binding.btnTutorialBack.setOnClickListener {
            if (state.step > 0) {
                state = OnBoardingStep.values()[state.step - 1]
                updateUI()
            }
        }
        binding.btnTutorialNext.setOnClickListener {
            checkIfGoNextStep()
        }
        binding.btnTutorialSkip.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            Log.d(TAG, "tutorial finished")
            navController.navigate(R.id.action_tutorialFragment_to_tabFragment)
        }
    }

    private fun checkIfGoNextStep() {
        when (state.step) {
            OnBoardingStep.values().size - 1 -> {
                // back to home
                val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
                val navController = navHostFragment.navController
                Log.d(TAG, "tutorial finished")
                navController.navigate(R.id.action_tutorialFragment_to_tabFragment)
            }
            else -> {
                goNextStep()
                updateUI()
            }
        }
    }
    private fun goNextStep() {
        state = OnBoardingStep.values()[state.step + 1]
    }

    private fun updateUI() {
        binding.pbTutorial.progress = state.step + 1
        binding.tvTutorialStep.text = getString(R.string.onboard_button_step, state.step + 1)
        when (state.step) {
            OnBoardingStep.Hydration.step -> {
                binding.homeBg.setImageResource(R.drawable.bg_tutorial_1)
                binding.btnTutorialBack.visibility = android.view.View.INVISIBLE
            }
            OnBoardingStep.Recommendation.step -> {
                binding.homeBg.setImageResource(R.drawable.bg_tutorial_2)
                binding.btnTutorialBack.visibility = android.view.View.VISIBLE
            }
            OnBoardingStep.Metrics.step -> {
                binding.homeBg.setImageResource(R.drawable.bg_tutorial_3)
            }
            OnBoardingStep.GraphicTime.step -> {
                binding.homeBg.setImageResource(R.drawable.bg_tutorial_4)
            }
            OnBoardingStep.GraphicStreak.step -> {
                binding.btnTutorialSkip.visibility = android.view.View.VISIBLE
                binding.btnTutorialNext.text = getString(R.string.onboard_button_next)
                binding.homeBg.setImageResource(R.drawable.bg_tutorial_5)
            }
            OnBoardingStep.Scanner.step -> {
                binding.btnTutorialSkip.visibility = android.view.View.INVISIBLE
                binding.btnTutorialNext.text = getString(R.string.onboard_button_start)
                binding.homeBg.setImageResource(R.drawable.bg_tutorial_6)
            }
        }
    }

    companion object {
        val TAG: String = TutorialFragment::class.java.simpleName
    }
}
