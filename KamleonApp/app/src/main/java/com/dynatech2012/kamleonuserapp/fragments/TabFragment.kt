package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.view.WindowCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_SCAN_INTRO_SEEN
import com.dynatech2012.kamleonuserapp.databinding.ActivityTabBinding
import com.dynatech2012.kamleonuserapp.models.observeEvent
import com.dynatech2012.kamleonuserapp.network.scanFailureText
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.marginForNavigationBar
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TabFragment : BaseFragment<ActivityTabBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityTabBinding = ActivityTabBinding.inflate(layoutInflater)

    private val qrViewModel: QrViewModel by viewModels()
    private lateinit var previewView: PreviewView
    private var camera: Camera? = null

    private val onDismissScanIntro = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {
        }
    }
    private val preview by lazy { Preview.Builder().build() }

    /** Puts the scanner's hint back after a "not a Kamleon code" notice. */
    private val restoreHint = Runnable { binding.tvScanHint.setText(R.string.scan_hint) }

    override fun initView() {
        onKamleonSheetResult(SCANNED_KEY) { onScannedAcknowledged() }
        // The scanner stops after reading a code; resume it so the user can scan again.
        onKamleonSheetResult(SCAN_FAILED_KEY) { qrViewModel.startAnalyzing() }
        onKamleonSheetResult(CAMERA_DENIED_KEY) { result ->
            if (result == SheetResult.PRIMARY && cameraBlocked) openAppSettings()
        }
        binding.tabContainer.marginForNavigationBar()
        previewView = binding.cameraPreview
        binding.scanOverlay.frame = binding.ivScanFrame
        val navHostFragment = binding.navHostFragmentTab.getFragment<NavHostFragment>()
        when (navHostFragment.childFragmentManager.fragments[0]) {
            is HomeFragment -> selectTab(0)
            is AnalyticFragment -> selectTab(1)
        }
        val navController = navHostFragment.navController

        binding.layoutTabHome.setOnClickListener { goHome() }
        binding.layoutTabAnalytic.setOnClickListener {
            selectTab(1)
            binding.navHostFragmentTab.visibility = View.VISIBLE
            if (navController.currentDestination?.id != R.id.analyticFragment) {
                navController.popBackStack()
                navController.navigate(R.id.action_to_analyticFragment)
            }
        }
        binding.layoutTabQr.setOnClickListener {
            selectTab(2)
            // Invisible, not gone: the screen underneath keeps its layout and state, so leaving the
            // scanner shows it as it was instead of laying it out (or recreating it) again.
            binding.navHostFragmentTab.visibility = View.INVISIBLE
            requestCamera()
        }
        binding.btnScanHelp.setOnClickListener { showQRIntroFragment() }
        binding.btnScanTorch.setOnClickListener { toggleTorch() }
        // Show the preview only once the camera is really streaming, with a short fade.
        previewView.previewStreamState.observe(viewLifecycleOwner) { state ->
            if (state == PreviewView.StreamState.STREAMING && camera != null) {
                previewView.alpha = 0f
                previewView.visibility = View.VISIBLE
                previewView.animate().alpha(1f).setDuration(PREVIEW_FADE_MS).start()
            }
        }

        initObservers()
        viewModel.getUserData()
        viewModel.getUserMeasures()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNewMeasures()
    }

    override fun initEvent() {
    }

    private fun initObservers() {
        qrViewModel.uploading.observe(viewLifecycleOwner) { uploading ->
            binding.pbScanUploading.visibility = if (uploading) View.VISIBLE else View.GONE
            // Tell the user at once that the code was read, before the upload answers.
            if (uploading) previewView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
        qrViewModel.foreignCode.observeEvent(viewLifecycleOwner) {
            binding.tvScanHint.removeCallbacks(restoreHint)
            binding.tvScanHint.setText(R.string.scan_not_kamleon)
            binding.tvScanHint.postDelayed(restoreHint, FOREIGN_NOTICE_MS)
        }
        qrViewModel.qrUploaded.observeEvent(viewLifecycleOwner) { result ->
            if (result.success) showScannedDialog() else showScanFailedDialog(result.failure)
        }
    }

    /** Opens the scanner tab, as if the user tapped it (used by Home's empty state). */
    fun openScanner() {
        binding.layoutTabQr.performClick()
    }

    private fun goHome() {
        selectTab(0)
        binding.navHostFragmentTab.visibility = View.VISIBLE
        val navController = binding.navHostFragmentTab.getFragment<NavHostFragment>().navController
        // Already on Home (as when leaving the scanner from it): keep it, don't build it again.
        if (navController.currentDestination?.id == R.id.homeFragment) return
        navController.popBackStack()
        navController.navigate(R.id.action_to_homeFragment)
    }

    private fun selectTab(tabIndex: Int) {
        binding.layoutTabHome.background = null
        binding.layoutTabAnalytic.background = null
        binding.layoutTabQr.background = null
        when (tabIndex) {
            0 -> binding.layoutTabHome.background = getDrawable(requireContext(), R.drawable.bg_tab_shape)
            1 -> binding.layoutTabAnalytic.background = getDrawable(requireContext(), R.drawable.bg_tab_shape)
            2 -> binding.layoutTabQr.background = getDrawable(requireContext(), R.drawable.bg_tab_shape)
        }
        val scanning = tabIndex == 2
        binding.layoutContentQR.visibility = if (scanning) View.VISIBLE else View.GONE
        binding.layoutScanControls.visibility = if (scanning) View.VISIBLE else View.GONE
        if (!scanning) {
            qrViewModel.stopAnalyzing()
            stopCamera()
        }
        // The camera preview is dark and fills the screen under the bars: use light bar icons there.
        setDarkSystemBarIcons(!scanning)
    }

    private fun setDarkSystemBarIcons(dark: Boolean) {
        val window = activity?.window ?: return
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }

    override fun onDestroyView() {
        // Other screens expect the default dark icons.
        setDarkSystemBarIcons(true)
        binding.tvScanHint.removeCallbacks(restoreHint)
        camera = null
        super.onDestroyView()
    }

    private fun showQRIntroFragment() {
        if (parentFragmentManager.findFragmentByTag(QR_TAG) != null) return
        ScanIntroFragment.newInstance(onDismissScanIntro).show(parentFragmentManager, QR_TAG)
    }

    /** The how-to sheet opens by itself only the first time; then it is behind the help button. */
    private fun showQRIntroTheFirstTime() {
        val prefs = SharedPrefUtil(requireContext())
        if (prefs.getBoolean(PREF_SCAN_INTRO_SEEN, false)) return
        prefs.saveBoolean(PREF_SCAN_INTRO_SEEN, true)
        showQRIntroFragment()
    }

    companion object {
        val TAG: String = TabFragment::class.java.simpleName
        private const val QR_TAG = "QR"
        private const val SCANNED_KEY = "scanned"
        private const val SCAN_FAILED_KEY = "scan_failed"
        private const val CAMERA_DENIED_KEY = "camera_denied"
        private const val FOREIGN_NOTICE_MS = 2_500L
        private const val PREVIEW_FADE_MS = 150L
    }


    // Scan
    /** Whether Android will no longer ask for the camera, so only the app settings can allow it. */
    private var cameraBlocked = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            startCamera()
        } else {
            DebugLog.d(TAG) { "Camera permission denied" }
            cameraBlocked = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            goHome()
            showCameraDeniedDialog()
        }
    }

    private fun requestCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            // The user may have left the scanner, or the screen, while the camera was getting ready.
            if (view == null || binding.layoutContentQR.visibility != View.VISIBLE) return@addListener
            try {
                bindCameraPreview(providerFuture.get())
                showQRIntroTheFirstTime()
            } catch (e: Exception) {
                DebugLog.e(TAG, e) { "startCamera: ${e.message}" }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        if (camera == null) return
        ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
        camera = null
        previewView.animate().cancel()
        previewView.visibility = View.INVISIBLE
        binding.btnScanTorch.visibility = View.INVISIBLE
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.unbindAll()
        val boundCamera = cameraProvider.bindToLifecycle(
            viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, qrViewModel.imageAnalysis, preview
        )
        camera = boundCamera
        qrViewModel.startAnalyzing()
        // Only offer the flashlight when there is one; its icon follows the real torch state.
        binding.btnScanTorch.visibility =
            if (boundCamera.cameraInfo.hasFlashUnit()) View.VISIBLE else View.INVISIBLE
        boundCamera.cameraInfo.torchState.removeObservers(viewLifecycleOwner)
        boundCamera.cameraInfo.torchState.observe(viewLifecycleOwner) { state ->
            val on = state == TorchState.ON
            binding.btnScanTorch.setImageResource(if (on) R.drawable.ic_flashlight_off else R.drawable.ic_flashlight)
            binding.btnScanTorch.contentDescription =
                getString(if (on) R.string.scan_torch_off else R.string.scan_torch_on)
        }
    }

    private fun toggleTorch() {
        val current = camera ?: return
        current.cameraControl.enableTorch(current.cameraInfo.torchState.value != TorchState.ON)
    }

    private fun showScannedDialog() {
        childFragmentManager.showKamleonSheet(
            SCANNED_KEY,
            SheetSpec(
                title = getString(R.string.dialog_scan_scanned),
                message = "",
                primary = getString(R.string.dialog_ok),
                dismissible = false
            )
        )
    }

    private fun showScanFailedDialog(failure: Exception?) {
        val (title, message) = scanFailureText(failure)
        childFragmentManager.showKamleonSheet(
            SCAN_FAILED_KEY,
            SheetSpec(
                title = getString(title),
                message = getString(message),
                primary = getString(R.string.dialog_ok)
            )
        )
    }

    private fun showCameraDeniedDialog() {
        childFragmentManager.showKamleonSheet(
            CAMERA_DENIED_KEY,
            if (cameraBlocked) {
                SheetSpec(
                    title = getString(R.string.scan_camera_denied_title),
                    message = getString(R.string.scan_camera_blocked_message),
                    primary = getString(R.string.setting_noti_open_settings),
                    secondary = getString(R.string.notifications_intro_later)
                )
            } else {
                SheetSpec(
                    title = getString(R.string.scan_camera_denied_title),
                    message = getString(R.string.scan_camera_denied_message),
                    primary = getString(R.string.dialog_ok)
                )
            }
        )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null))
        )
    }

    private fun onScannedAcknowledged() {
        // Looked up by tag so it also works after the activity was recreated.
        (parentFragmentManager.findFragmentByTag(QR_TAG) as? ScanIntroFragment)?.dismiss()
        goHome()
    }
}
