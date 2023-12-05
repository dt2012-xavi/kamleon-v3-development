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
        binding.layoutTabHome.setOnClickListener {
            selectTab(0)
            findNavController().navigate(R.id.action_analyticFragment_to_homeFragment)
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

        selectTab(1)

        initObservers()
        viewModel.getUserData()
        //viewModel.getUserMeasures()

        // Scan
        bindViews()
    }

    private fun bindViews()
    {
        previewView = binding.cameraPreview
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
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


    private fun showQRIntroFragment() {
        val qrFragment = ScanIntroFragment.newInstance(onDismissScanIntro)
        qrFragment.show(parentFragmentManager, "QR")
    }

    companion object {
        val TAG: String = AnalyticFragment::class.java.simpleName
    }


    // Scan
    private val requestPermissionLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (result) {
            // Permission is granted
            Log.d(HomeFragment.TAG, "qrScanner, permission granted")
            startCamera()
        } else {
            // Permission is denied
            // Should go back to home fragment
            Log.e(HomeFragment.TAG, "qrScanner, permission denied")
            selectTab(1)
        }
    }

    private fun requestCamera() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(HomeFragment.TAG, "qrScanner, permission granted")
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                bindCameraPreview(cameraProvider)
            } catch (e: ExecutionException) {
                Log.e(HomeFragment.TAG, "startCamera: ${e.message}", e)
            } catch (e: InterruptedException) {
                Log.e(HomeFragment.TAG, "startCamera: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(HomeFragment.TAG, "startCamera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        Log.d(HomeFragment.TAG, "qrScanner, bindCameraPreview 1")
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)
        Log.d(HomeFragment.TAG, "qrScanner, bindCameraPreview 2")

        // Callback when result from analyzing image is returned
        qrListener = object : QRCodeFoundListener {
            var qrFound = false
            override fun onQRCodeFound(qrCode: String) {
                // Sometimes it is called multiple times
                if (view != null && !qrFound) {
                    Log.d(HomeFragment.TAG,"qrScanner, getView != null")
                    qrFound = true
                    uploadQr(qrCode)
                } else {
                    Log.e(HomeFragment.TAG, "qrScanner, getView == null")
                }
            }

            override fun onQRCodeException(e: Exception?) {
                Log.e(HomeFragment.TAG, "qrScanner, qrCode EXCEPTION: ", e)
            }

            override fun onQRCodeNotFound(e: Exception?) {
                Log.e(HomeFragment.TAG, "qrScanner, qrCode NOT FOUND, exception: ", e)
            }
        }

        // Create QRCode analyzer to analyze image
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzerKotlin(qrListener));
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzer(qrListener));
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzerMLKit(qrListener));
        qrViewModel.startScanner()
        qrViewModel.startAnalyzing()
        cameraProvider
            .bindToLifecycle(this as LifecycleOwner, cameraSelector, qrViewModel.imageAnalysis, preview)
    }

    private fun uploadQr(qrCode: String) {
        Log.d(HomeFragment.TAG, "qrCode: $qrCode")
        qrViewModel.uploadQRtoRealtime(qrCode)
        selectTab(1)
        qrViewModel.stopAnalyzing()
    }
}