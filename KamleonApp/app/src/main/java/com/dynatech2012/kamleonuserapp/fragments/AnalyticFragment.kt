package com.dynatech2012.kamleonuserapp.fragments

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityAnalyticBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnalyticFragment : BaseFragment<ActivityAnalyticBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityAnalyticBinding = ActivityAnalyticBinding.inflate(layoutInflater)


    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }

    override fun initView() {
        binding.layoutTabHome.setOnClickListener {
            selectTab(0)
            findNavController().navigate(R.id.action_analyticFragment_to_homeFragment)
        }
        binding.layoutTabQr.setOnClickListener {
            selectTab(2)
            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        sleep(400)
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "run: ", e)
                    }
                    activity?.runOnUiThread {
                        showQRIntroFragment()
                    }
                }
            }
            thread.start()
        }

        selectTab(1)

        initObservers()
        viewModel.getUserData()
        //viewModel.getUserMeasures()
    }

    override fun initEvent() {
        /*
        binding.listItemHydration.isClickable = true
        binding.listItemHydration.setOnClickListener {
            openGraphView(0)
        }
        binding.listItemElectrolyte.isClickable = true
        binding.listItemElectrolyte.setOnClickListener {
            openGraphView(1)
        }
        binding.listItemVolume.isClickable = true
        binding.listItemVolume.setOnClickListener {
            openGraphView(2)
        }
        */

        binding.btnNavProfile.isClickable = true
        binding.btnNavProfile.setOnClickListener {
            findNavController().navigate(R.id.action_analyticFragment_to_settingFragment)
        }
    }

    private fun openGraphView(type: Int) {
        viewModel.graphicType = type
        findNavController().navigate(R.id.action_analyticFragment_to_graphicsFragment)
    }

    private fun selectTab(tabIndex: Int) {
        binding.layoutContentAnalytics.visibility = View.GONE
        binding.layoutContentQR.visibility = View.GONE

        when (tabIndex) {
            0 -> {
                binding.layoutTabHome.background = getDrawable(requireContext(), R.drawable.bg_tab_button_shadow)
    //            binding.layoutTabHome.setBackgroundResource(R.drawable.bg_setting_option_shadow)
                binding.layoutTabAnalytic.background = null
                binding.layoutTabQr.background = null
            }
            1 -> {
                binding.layoutTabAnalytic.setBackgroundResource(R.drawable.bg_tab_button_shadow)
                binding.layoutTabHome.background = null
                binding.layoutTabQr.background = null

                binding.layoutContentAnalytics.visibility = View.VISIBLE
            }
            else -> {
                binding.layoutTabQr.setBackgroundResource(R.drawable.bg_tab_button_shadow)
                binding.layoutTabAnalytic.background = null
                binding.layoutTabHome.background = null

                binding.layoutContentQR.visibility = View.VISIBLE
            }
        }
    }
    private fun initObservers() {
        onLastMeasureChanged(viewModel.lastMeasure.value)
        viewModel.userImage.observe(this, this::onProfileImageUriChanged)
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
    }

    private fun onLastMeasureChanged(measureData: MeasureData?) {
        Log.d(TAG, "onLastMeasureChanged: $measureData")
        val cvHy = binding.cvAnalyticHydration
        val score = measureData?.score
        cvHy.value = score ?: 0//77
        //cvHy.analyticType = AnalyticType.HYDRATION
        cvHy.subtitle = when (score) {
            in 0..30 -> getString(R.string.severely_dehydrated)
            in 31..64 -> getString(R.string.dehydrated)
            in 65..90, null -> getString(R.string.hydrated)
            else -> getString(R.string.hydrated)
        }
        cvHy.description = when (score) {
            in 0..64 -> getString(R.string.hydratation_under)
            else -> getString(R.string.hydratation_above)
        }
        cvHy.onClick = { openGraphView(0) }
        val el = measureData?.msCond?.toInt()// ?: 0
        val cvEl = binding.cvAnalyticElectrol
        cvEl.value = el ?: 0
        //cvEl.analyticType = AnalyticType.ELECTROLYTE
        cvEl.subtitle = when (el) {
            in 0..4 -> getString(R.string.elec_hypo)
            in 5..19, null -> getString(R.string.elec_normal)
            else -> getString(R.string.elec_hyper)
        }
        cvEl.description = when (el) {
            in 0..4 -> getString(R.string.elec_under)
            in 5..19, null -> getString(R.string.elec_optimal)
            else -> getString(R.string.elec_above)
        }
        cvEl.onClick = { openGraphView(1) }

        val cvVol = binding.cvAnalyticVolume
        val vol = measureData?.vol?.toInt()
        cvVol.value = vol ?: 0//150
        //cvVol.analyticType = AnalyticType.VOLUME
        cvVol.subtitle = when (vol) {
            in 0..149 -> getString(R.string.volume_low)
            in 150..249, null -> getString(R.string.volume_medium)
            else -> getString(R.string.volume_high)
        }
        cvVol.description = when (vol) {
            in 0..149 -> getString(R.string.volume_insuff)
            else -> getString(R.string.volume_good)
        }
        cvVol.onClick = { openGraphView(2) }
    }

    private fun onUserDataChanged(userData: CustomUser) {
        if (userData.imageUrl.isNotBlank())
            binding.btnNavProfile.load(userData.imageUrl) {
                placeholder(R.drawable.image_profile)
            }
    }

    private fun onProfileImageUriChanged(uri: Uri?) {
        Log.d(SettingFragment.TAG, "Got image profile")
        binding.btnNavProfile.setImageURI(uri)
    }

    private fun showQRIntroFragment() {
        val qrFragment = ScanIntroFragment.newInstance(onDismissScanIntro)
        qrFragment.show(parentFragmentManager, "QR")
    }

    companion object {
        val TAG: String = AnalyticFragment::class.java.simpleName
    }
}