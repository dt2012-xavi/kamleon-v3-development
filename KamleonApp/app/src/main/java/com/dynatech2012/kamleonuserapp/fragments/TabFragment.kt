package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.databinding.ActivityTabBinding
import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.models.observeEvent
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
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
    lateinit var qrDebugging: TextView

    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }
    private val preview by lazy { Preview.Builder().build() }

    override fun initView() {
        onKamleonSheetResult("scanned") { onScannedAcknowledged() }
        Log.d(TAG, "initView")
        getFirstLogin()
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
            binding.navHostFragmentTab.visibility = View.VISIBLE
            navController.popBackStack()
            navController.navigate(R.id.action_to_homeFragment)
        }
        binding.layoutTabAnalytic.setOnClickListener {
            selectTab(1)
            binding.navHostFragmentTab.visibility = View.VISIBLE
            navController.popBackStack()
            navController.navigate(R.id.action_to_analyticFragment)
        }

        qrDebugging = binding.tvQrDebugging

        binding.layoutTabQr.setOnClickListener {
            selectTab(2)
            binding.navHostFragmentTab.visibility = View.GONE
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
        Log.d(TAG, "got measures -2 oncreateView")
        viewModel.getUserMeasures()

    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNewMeasures()
    }

    private fun getFirstLogin() {
        val isFirstLogin = viewModel.getFirstLogin()
        if (isFirstLogin) {
            //val navHostFragment = binding.navHostFragmentTab.getFragment<NavHostFragment>()
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.action_tabFragment_to_tutorialFragment)
            viewModel.tutorialComingFromHome = true
        }
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
        qrViewModel.qrDebug.observe(viewLifecycleOwner) {
            when (it) {
                is Response.Success -> {
                    DebugLog.d(TAG) { "QR analyzer status received" }
                    appendDebugText("QR analyzer status received")
                }
                is Response.Failure -> {
                    DebugLog.e(TAG) { "QR analyzer failed" }
                    appendDebugText("QR analyzer failed")
                }
                else -> { }
            }
        }
        qrViewModel.qrResponse.observe(viewLifecycleOwner, this::uploadQr)
        qrViewModel.qrUploaded.observeEvent(viewLifecycleOwner) {
            Log.d(TAG, "qrScanner, qrUploaded")
            appendDebugText("QR debug -- uploaded to realtime, showing success dialog")
            showScannedDialog()
        }
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
        ScanIntroFragment.newInstance(onDismissScanIntro).show(parentFragmentManager, QR_TAG)
    }

    companion object {
        val TAG: String = TabFragment::class.java.simpleName
        private const val QR_TAG = "QR"
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
        //cameraProvider.unbind(qrViewModel.imageAnalysis, preview)
        qrViewModel.cameraStarted = false
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        Log.d(TAG, "qrScanner, bindCameraPreview 1")
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)
        Log.d(TAG, "qrScanner, bindCameraPreview 2")
        appendDebugText("QR debug -- starting surface provider set")
        qrViewModel.startScanner()
        appendDebugText("QR debug -- scan qr started")
        qrViewModel.startAnalyzing()
        appendDebugText("QR debug -- scan qr started analyzing")
        cameraProvider
          .bindToLifecycle(this as LifecycleOwner, cameraSelector, qrViewModel.imageAnalysis, preview)
        appendDebugText("QR debug -- camera provider bound to lifecycle")
        qrViewModel.cameraStarted = true
    }

    private fun uploadQr(qrResponse: Response<QRResponse>) {
        when (qrResponse) {
            is Response.Success -> {
                DebugLog.d(TAG) { "QR parsed; starting upload" }
                appendDebugText("QR debug -- uploading to realtime")
                qrViewModel.uploadQRtoFirestore(qrResponse.data)
                //showScannedDialog()
            }
            is Response.Failure -> {
                DebugLog.e(TAG) { "QR parsing failed" }
                appendDebugText("QR parsing failed")
            }
            else -> { }
        }
    }

    private fun appendDebugText(text: String) {
        qrDebugging.text = qrDebugging.text.toString() + "\n" + text
    }

    private fun showScannedDialog() {
        childFragmentManager.showKamleonSheet(
            "scanned",
            SheetSpec(
                title = getString(R.string.dialog_scan_scanned),
                message = "",
                primary = getString(R.string.dialog_ok),
                dismissible = false
            )
        )
    }

    private fun onScannedAcknowledged() {
        // Looked up by tag so it also works after the activity was recreated.
        (parentFragmentManager.findFragmentByTag(QR_TAG) as? ScanIntroFragment)?.dismiss()
        selectTab(0)
        binding.navHostFragmentTab.visibility = View.VISIBLE
        val navHostFragment = binding.navHostFragmentTab.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController
        navController.popBackStack()
        navController.navigate(R.id.action_to_homeFragment)
    }
}
