package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.camera.QRCodeFoundListener
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityAnalyticBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.concurrent.ExecutionException

@AndroidEntryPoint
class AnalyticFragment : BaseFragment<ActivityAnalyticBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityAnalyticBinding = ActivityAnalyticBinding.inflate(layoutInflater)

    private val qrViewModel: QrViewModel by viewModels()
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var qrListener: QRCodeFoundListener? = null

    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }

    override fun initView() {
        /*
        binding.layoutTabHome.setOnClickListener {
            selectTab(0)
            findNavController().navigate(R.id.action_analyticFragment_to_homeFragment)
        }
        binding.layoutTabAnalytic.setOnClickListener {
            selectTab(1)
        }
        binding.layoutTabQr.setOnClickListener {
            selectTab(2)
            requestCamera()
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
        */

        //selectTab(1)

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

        binding.ivAnalyticProfile.isClickable = true
        binding.ivAnalyticProfile.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.action_tabFragment_to_settingFragment)
        }
    }

    private fun openGraphView(type: Int) {
        viewModel.graphicType = type
        val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.action_tabFragment_to_graphicsFragment)
        //findNavController().navigate(R.id.action_analyticFragment_to_graphicsFragment)
    }

    private fun initObservers() {
        onLastMeasureChanged(viewModel.lastMeasure.value)
        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
        viewModel.lastMeasure.observe(this, this::onLastMeasureChanged)
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(HomeFragment.TAG, "image changed")
        drawable?.let {
            binding.ivAnalyticProfile.load(drawable)
            return
        }
    }

    private fun onLastMeasureChanged(measureData: MeasureData?) {
        Log.d(TAG, "onLastMeasureChanged: $measureData")
        val cvHy = binding.cvAnalyticHydration
        val score = measureData?.score
        cvHy.value = score ?: 0//77
        //cvHy.analyticType = AnalyticType.HYDRATION
        cvHy.subtitle = when (score) {
            in 0..30 -> getString(R.string.analytic_severely_dehydrated)
            in 31..64 -> getString(R.string.analytic_dehydrated)
            in 65..90, null -> getString(R.string.analytic_hydrated)
            else -> getString(R.string.analytic_very_hydrated)
        }
        cvHy.description = when (score) {
            in 0..64 -> getString(R.string.analytic_hydratation_under)
            else -> getString(R.string.analytic_hydratation_above)
        }
        cvHy.onClick = { openGraphView(0) }
        val el = measureData?.msCond?.toInt()// ?: 0
        val cvEl = binding.cvAnalyticElectrol
        cvEl.value = el ?: 0
        //cvEl.analyticType = AnalyticType.ELECTROLYTE
        cvEl.subtitle = when (el) {
            in 0..4 -> getString(R.string.analytic_elec_hypo)
            in 5..19, null -> getString(R.string.analytic_elec_normal)
            else -> getString(R.string.analytic_elec_hyper)
        }
        cvEl.description = when (el) {
            in 0..4 -> getString(R.string.analytic_elec_under)
            in 5..19, null -> getString(R.string.analytic_elec_optimal)
            else -> getString(R.string.analytic_elec_above)
        }
        cvEl.onClick = { openGraphView(1) }

        val cvVol = binding.cvAnalyticVolume
        val vol = measureData?.vol?.toInt()
        cvVol.value = vol ?: 0//150
        //cvVol.analyticType = AnalyticType.VOLUME
        cvVol.subtitle = when (vol) {
            in 0..149 -> getString(R.string.analytic_volume_low)
            in 150..249, null -> getString(R.string.analytic_volume_medium)
            else -> getString(R.string.analytic_volume_high)
        }
        cvVol.description = when (vol) {
            in 0..149 -> getString(R.string.analytic_volume_insuff)
            else -> getString(R.string.analytic_volume_good)
        }
        cvVol.onClick = { openGraphView(2) }

        val cvNew = binding.cvAnalyticEmpty
        cvNew.onClick = { }

        measureData?.analysisDate?.let {
            val date = Date(it)
            val dateString = date.formatTime.uppercase()
            binding.tvAnalyticLastSample.text = getString(R.string.home_last_sample_message, dateString)
        }
    }

    companion object {
        val TAG: String = AnalyticFragment::class.java.simpleName
    }
}