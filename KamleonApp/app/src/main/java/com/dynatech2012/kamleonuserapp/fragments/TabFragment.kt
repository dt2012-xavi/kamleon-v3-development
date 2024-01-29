package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.TextView
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
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.camera.QRCodeFoundListener
import com.dynatech2012.kamleonuserapp.databinding.ActivityTabBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutionException


@AndroidEntryPoint
class TabFragment : BaseFragment<ActivityTabBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityTabBinding = ActivityTabBinding.inflate(layoutInflater)

    private val qrViewModel: QrViewModel by viewModels()
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var qrListener: QRCodeFoundListener? = null
    lateinit var qrDebugging: TextView

    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }

    override fun initView() {
        Log.d(TAG, "initView")
        bindViews()
        //val navHostFragment = childFragmentManager.findFragmentById(R.id.nav_host_fragment_tab) as NavHostFragment
        val navHostFragment = binding.navHostFragmentTab.getFragment<NavHostFragment>()
        when (navHostFragment.childFragmentManager.fragments[0]) {
            is HomeFragment -> {
                Log.d(TAG, "initView is home")
                selectTab(0)
            }
            is AnalyticFragment -> {
                Log.d(TAG, "initView is analytic")
                selectTab(1)
            }
        }
        val navController = navHostFragment.navController

        binding.layoutTabHome.setOnClickListener {
            selectTab(0)
            navController.popBackStack()
            navController.navigate(R.id.action_to_homeFragment)
        }
        binding.layoutTabAnalytic.setOnClickListener {
            selectTab(1)
            navController.popBackStack()
            navController.navigate(R.id.action_to_analyticFragment)
        }

        qrDebugging = binding.tvQrDebugging

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

        //selectTab(0)

        initObservers()
        viewModel.getUserData()
        Log.d(TAG, "got measures -2")
        viewModel.getUserMeasures()

        // Scan
    }

    private fun bindViews()
    {
        previewView = binding.cameraPreview
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
    }

    override fun initEvent() {
        /*
        binding.ivHomeProfile.isClickable = true
        binding.ivHomeProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)
        }
        */
    }
    private fun initObservers() {

    }

    private fun selectTab(tabIndex: Int) {
        when (tabIndex) {
            0 -> {
                binding.layoutTabHome.background = getDrawable(requireContext(), R.drawable.bg_tab_shape)
                binding.layoutTabAnalytic.background = null
                binding.layoutTabQr.background = null
                binding.layoutContentQR.visibility = View.GONE
                qrViewModel.stopAnalyzing()
                stopCamera()
            }
            1 -> {
                binding.layoutTabAnalytic.setBackgroundResource(R.drawable.bg_tab_shape)
                binding.layoutTabHome.background = null
                binding.layoutTabQr.background = null
                binding.layoutContentQR.visibility = View.GONE
                qrViewModel.stopAnalyzing()
                stopCamera()
            }
            2 -> {
                binding.layoutTabQr.setBackgroundResource(R.drawable.bg_tab_shape)
                binding.layoutTabAnalytic.background = null
                binding.layoutTabHome.background = null
                binding.layoutContentQR.visibility = View.VISIBLE
            }
        }
    }


    private fun showQRIntroFragment() {
        val qrFragment = ScanIntroFragment.newInstance(onDismissScanIntro)
        qrFragment.show(parentFragmentManager, "QR")
    }

    companion object {
        val TAG: String = TabFragment::class.java.simpleName
    }


    // Scan
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (result) {
            // Permission is granted
            Log.d(TAG, "qrScanner, permission granted")
            appendDebugText("QR debug -- camera permission granted")
            startCamera()
        } else {
            // Permission is denied
            // Should go back to home fragment
            Log.e(TAG, "qrScanner, permission denied")
            appendDebugText("QR debug -- camera permission denied")
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
            appendDebugText("QR debug -- camera permission granted")
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        appendDebugText("QR debug -- starting camera")
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                appendDebugText("QR debug -- starting get camera provider")
                bindCameraPreview(cameraProvider)
            } catch (e: ExecutionException) {
                appendDebugText("QR debug -- starting camera execution exception: $e")
                Log.e(TAG, "startCamera: ${e.message}", e)
            } catch (e: InterruptedException) {
                appendDebugText("QR debug -- starting camera interrupted exception: $e")
                Log.e(TAG, "startCamera: ${e.message}", e)
            } catch (e: Exception) {
                appendDebugText("QR debug -- starting camera other exception: $e")
                Log.e(TAG, "startCamera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        if (!qrViewModel.cameraStarted) { return }
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
        qrViewModel.cameraStarted = false
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
        appendDebugText("QR debug -- starting surface provider set")
        // Callback when result from analyzing image is returned
        qrListener = object : QRCodeFoundListener {
            var qrFound = false
            override fun onQRCodeFound(qrCode: String) {
                // Sometimes it is called multiple times
                if (view != null && !qrFound) {
                    appendDebugText("QR debug -- qr found")
                    Log.d(TAG,"qrScanner, getView != null")
                    qrFound = true
                    uploadQr(qrCode)
                } else {
                    appendDebugText("QR debug -- qr found but view is null")
                    Log.e(TAG, "qrScanner, getView == null")
                }
            }

            override fun onQRCodeException(e: Exception?) {
                appendDebugText("QR debug -- scan qr exception: $e")
                Log.e(TAG, "qrScanner, qrCode EXCEPTION: ", e)
            }

            override fun onQRCodeNotFound(e: Exception?) {
                appendDebugText("QR debug -- scan qr not found exception: $e")
                Log.e(TAG, "qrScanner, qrCode NOT FOUND, exception: ", e)
            }
        }

        // Create QRCode analyzer to analyze image
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzerKotlin(qrListener));
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzer(qrListener));
        //imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), new QRCodeImageAnalyzerMLKit(qrListener));
        qrViewModel.startScanner()
        appendDebugText("QR debug -- scan qr started")
        qrViewModel.startAnalyzing()
        appendDebugText("QR debug -- scan qr started analyzing")
        cameraProvider
          .bindToLifecycle(this as LifecycleOwner, cameraSelector, qrViewModel.imageAnalysis, preview)
        appendDebugText("QR debug -- camera provider bound to lifecycle")
        qrViewModel.cameraStarted = true
    }

    private fun uploadQr(qrCode: String) {
        Log.d(TAG, "qrCode: $qrCode")
        appendDebugText("QR debug -- uploading to realtime")
        qrViewModel.uploadQRtoRealtime(qrCode)
        appendDebugText("QR debug -- uploaded to realtime, going back to home")
        selectTab(0)
    }

    private fun appendDebugText(text: String) {
        qrDebugging.text = qrDebugging.text.toString() + "\n" + text
    }
}
