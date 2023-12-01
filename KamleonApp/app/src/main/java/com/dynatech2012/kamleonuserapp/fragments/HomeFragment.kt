package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityHomeBinding
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.RecommendationType
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.cards.ViewPager
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale


@AndroidEntryPoint
class HomeFragment : BaseFragment<ActivityHomeBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }

    override fun initView() {
        binding.layoutTabAnalytic.setOnClickListener {
            selectTab(1)
            findNavController().navigate(R.id.action_homeFragment_to_analyticFragment)
        }

        binding.layoutTabQr.setOnClickListener {
            selectTab(2)
            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        sleep(400)
                    } catch (_: InterruptedException) {
                    }
                    activity?.runOnUiThread {
                        showQRIntroFragment()
                    }
                }
            }
            thread.start()
        }

        setupTips(null)

        selectTab(0)

        initObservers()
        viewModel.getUserData()
        Log.d(TAG, "got measures -2")
        viewModel.getUserMeasures()
    }

    override fun initEvent() {
        binding.ivHomeProfile.isClickable = true
        binding.ivHomeProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)
        }
    }
    private fun initObservers() {
        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
    }

    private fun selectTab(tabIndex: Int) {
        //binding.layoutContentAnalytics.visibility = View.GONE
        binding.layoutContentQR.visibility = View.GONE

        if (tabIndex == 0) {
            binding.layoutTabHome.background = getDrawable(requireContext(), R.drawable.bg_tab_button_shadow)
//            binding.layoutTabHome.setBackgroundResource(R.drawable.bg_setting_option_shadow)
            binding.layoutTabAnalytic.background = null
            binding.layoutTabQr.background = null
        } else if (tabIndex == 1) {
            binding.layoutTabAnalytic.setBackgroundResource(R.drawable.bg_tab_button_shadow)
            binding.layoutTabHome.background = null
            binding.layoutTabQr.background = null

            //binding.layoutContentHome.visibility = View.VISIBLE
        } else {
            binding.layoutTabQr.setBackgroundResource(R.drawable.bg_tab_button_shadow)
            binding.layoutTabAnalytic.background = null
            binding.layoutTabHome.background = null

            binding.layoutContentQR.visibility = View.VISIBLE
        }
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(TAG, "image changed")
        drawable?.let {
            binding.ivHomeProfile.load(drawable)
            return
        }
    }

    private fun onLastMeasureChanged(measure: MeasureData) {
        Log.d(TAG, "got measures 6 changed")
        binding.lastMeasure.text = measure.score.toString()
        binding.homeProgressBar.progress = measure.score
        val date = measure.analysisDate
        val format = SimpleDateFormat("d MMMM yyy", Locale.getDefault())
        val dateString = format.format(date)
        binding.tvHomeLastSample.text = getString(R.string.home_last_sample_message, dateString)
        when (measure.hydrationLevel) {
            MeasureData.HydrationLevel.VERYHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_very_hydrated)
                binding.tvHomeMessage.text = getString(R.string.analytic_very_hydrated)
            }
            MeasureData.HydrationLevel.HYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_hydrated)
                binding.tvHomeMessage.text = getString(R.string.analytic_hydrated)
            }
            MeasureData.HydrationLevel.DEHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_dehydrated)
                binding.tvHomeMessage.text = getString(R.string.analytic_dehydrated)
            }
            MeasureData.HydrationLevel.VERYDEHYDRATED -> {
                binding.homeBg.setImageResource(R.drawable.bg_severely_dehydrated)
                binding.tvHomeMessage.text = getString(R.string.analytic_severely_dehydrated)
            }
        }
        setupTips(measure.hydrationLevel)
    }


    private fun setupTips(hydrationLevel: MeasureData.HydrationLevel?) {
        binding.cvHome.setContent {
            ViewPager(recommendationType = RecommendationType.HOME, hydrationLevel = hydrationLevel, modifier = Modifier)
        }
    }

    private fun showQRIntroFragment() {
        val qrFragment = ScanIntroFragment.newInstance(onDismissScanIntro)
        qrFragment.show(parentFragmentManager, "QR")
    }

    companion object {
        val TAG: String = HomeFragment::class.java.simpleName
    }
}
