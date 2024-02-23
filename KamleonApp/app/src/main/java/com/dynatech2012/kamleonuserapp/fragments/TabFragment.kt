package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.camera.QRCodeFoundListener
import com.dynatech2012.kamleonuserapp.databinding.ActivityTabBinding
import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.models.observeEvent
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.wait
import java.util.concurrent.ExecutionException


@AndroidEntryPoint
class TabFragment : BaseFragment<ActivityTabBinding>() {
    private var qrFragment: ScanIntroFragment? = null
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
    private val preview by lazy { Preview.Builder().build() }

    override fun initView() {
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
                    Log.d(TAG, "qrScanner, qrDebug: ${it.data}")
                    appendDebugText("QR debug FROM ANALYZER -- ${it.data}")
                }
                is Response.Failure -> {
                    Log.e(TAG, "qrScanner, qrDebug: ${it.exception}")
                    appendDebugText("QR debug FROM ANALYZER -- ${it.exception}")
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
        qrFragment = ScanIntroFragment.newInstance(onDismissScanIntro)
        qrFragment?.show(parentFragmentManager, "QR")
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
        // Callback when result from analyzing image is returned
        /*
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
        */

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

    private fun uploadQr(qrResponse: Response<QRResponse>) {
        when (qrResponse) {
            is Response.Success -> {
                Log.d(TAG, "qrScanner, qrString: ${qrResponse.data}")
                appendDebugText("QR debug -- uploading to realtime")
                qrViewModel.uploadQRtoFirestore(qrResponse.data)
                //showScannedDialog()
            }
            is Response.Failure -> {
                Log.e(TAG, "qrScanner, qrString eeror : ${qrResponse.exception}")
                appendDebugText("QR debug FROM ANALYZER error -- ${qrResponse.exception}")
            }
            else -> { }
        }
    }

    private fun appendDebugText(text: String) {
        qrDebugging.text = qrDebugging.text.toString() + "\n" + text
    }

    private fun showScannedDialog() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_dialog_ok, null)

        dialog.setView(dialogView)
        dialog.setCancelable(false)
        val logoutDialog = dialog.show()
        logoutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        tvTitle.text = getString(R.string.dialog_scan_scanned)

        val tvDescr = dialogView.findViewById<TextView>(R.id.tvDialogDesc)
        tvDescr.text = ""
        dialogView.findViewById<TextView>(R.id.tvBtnOk).setOnClickListener {
            qrFragment?.dismiss()
            logoutDialog.dismiss()
            selectTab(0)
            binding.navHostFragmentTab.visibility = View.VISIBLE
            val navHostFragment = binding.navHostFragmentTab.getFragment<NavHostFragment>()
            val navController = navHostFragment.navController
            navController.popBackStack()
            navController.navigate(R.id.action_to_homeFragment)
        }
    }
}
