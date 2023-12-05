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
import androidx.compose.ui.Modifier
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
import com.dynatech2012.kamleonuserapp.databinding.ActivityHomeBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.RecommendationType
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import com.dynatech2012.kamleonuserapp.views.cards.ViewPager
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.concurrent.ExecutionException


@AndroidEntryPoint
class HomeFragment : BaseFragment<ActivityHomeBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    private val qrViewModel: QrViewModel by viewModels()
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var qrListener: QRCodeFoundListener? = null

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
            requestCamera()
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

        // Scan
        bindViews()
    }

    private fun bindViews()
    {
        previewView = binding.cameraPreview
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
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

            startCamera()
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
        val date = Date(measure.analysisDate)
        //val format = SimpleDateFormat("d MMMM yyy", Locale.getDefault())
        //val dateString = format.format(date)
        val dateString = date.formatTime.uppercase()
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
        ///val qrFragment = ScanIntroFragment.newInstance(onDismissScanIntro)
        ///qrFragment.show(parentFragmentManager, "QR")
    }

    companion object {
        val TAG: String = HomeFragment::class.java.simpleName
    }


    // Scan
    private val requestPermissionLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (result) {
            // Permission is granted
            Log.d(TAG, "qrScanner, permission granted")
            //startCamera()
        } else {
            // Permission is denied
            // Should go back to home fragment
            Log.e(TAG, "qrScanner, permission denied")
            selectTab(0)
        }
    }

    private fun requestCamera() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "qrScanner, permission granted")
            //startCamera()
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
                Log.e(TAG, "startCamera: ${e.message}", e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "startCamera: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "startCamera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        Log.d(TAG, "qrScanner, bindCameraPreview 1")
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)
        Log.d(TAG, "qrScanner, bindCameraPreview 2")

        // Callback when result from analyzing image is returned
        qrListener = object : QRCodeFoundListener {
            var qrFound = false
            override fun onQRCodeFound(qrCode: String) {
                // Sometimes it is called multiple times
                if (view != null && !qrFound) {
                    Log.d(TAG,"qrScanner, getView != null")
                    qrFound = true
                    uploadQr(qrCode)
                } else {
                    Log.e(TAG, "qrScanner, getView == null")
                }
            }

            override fun onQRCodeException(e: Exception?) {
                Log.e(TAG, "qrScanner, qrCode EXCEPTION: ", e)
            }

            override fun onQRCodeNotFound(e: Exception?) {
                Log.e(TAG, "qrScanner, qrCode NOT FOUND, exception: ", e)
            }
        }

        // Create QRCode analyzer to analyze image
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzerKotlin(qrListener));
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzer(qrListener));
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzerMLKit(qrListener));
        ////qrViewModel.startScanner()
        ///qrViewModel.startAnalyzing()
        cameraProvider
          .bindToLifecycle(this as LifecycleOwner, cameraSelector, qrViewModel.imageAnalysis, preview)
    }

    private fun uploadQr(qrCode: String) {
        Log.d(TAG, "qrCode: $qrCode")
        qrViewModel.uploadQRtoRealtime(qrCode)
        selectTab(0)
        qrViewModel.stopAnalyzing()
    }
}
