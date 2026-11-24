package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_CAMERA_ASKED
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_SCAN_INTRO_SEEN
import com.dynatech2012.kamleonuserapp.databinding.ActivityTabBinding
import com.dynatech2012.kamleonuserapp.models.observeEvent
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.ScanLinkState
import com.dynatech2012.kamleonuserapp.views.CameraAccess
import com.dynatech2012.kamleonuserapp.views.CameraAccessPanel
import com.dynatech2012.kamleonuserapp.views.ScanLinkSheet
import com.dynatech2012.kamleonuserapp.views.cameraAccessOf
import com.dynatech2012.kamleonuserapp.views.ScanSheets
import com.dynatech2012.kamleonuserapp.views.dismissKamleonSheet
import com.dynatech2012.kamleonuserapp.views.marginForNavigationBar
import com.dynatech2012.kamleonuserapp.views.showScanIntroSheet
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TabFragment : BaseFragment<ActivityTabBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityTabBinding = ActivityTabBinding.inflate(layoutInflater)

    // The activity's: the scan sheet (ScanLinkSheet) follows the same one.
    private val qrViewModel: QrViewModel by activityViewModels()
    private lateinit var previewView: PreviewView
    private var camera: Camera? = null

    private val preview by lazy { Preview.Builder().build() }

    /** Puts the scanner's hint back after a "not a Kamleon code" notice. */
    private val restoreHint = Runnable { binding.tvScanHint.setText(R.string.scan_hint) }

    override fun initView() {
        binding.tabContainer.marginForNavigationBar()
        previewView = binding.cameraPreview
        binding.scanOverlay.frame = binding.ivScanFrame
        val navHostFragment = binding.navHostFragmentTab.getFragment<NavHostFragment>()
        when (navHostFragment.childFragmentManager.fragments[0]) {
            is HomeFragment -> selectTab(0)
            is AnalyticFragment -> selectTab(1)
        }

        binding.layoutTabHome.setOnClickListener { goHome() }
        binding.layoutTabAnalytic.setOnClickListener {
            showDestination(R.id.analyticFragment, R.id.action_to_analyticFragment)
            selectTab(1)
            binding.navHostFragmentTab.visibility = View.VISIBLE
        }
        binding.layoutTabQr.setOnClickListener {
            selectTab(2)
            // Invisible, not gone: the screen underneath keeps its layout and state, so leaving the
            // scanner shows it as it was instead of laying it out (or recreating it) again.
            binding.navHostFragmentTab.visibility = View.INVISIBLE
            // The user just asked to scan: the first time, ask for the camera straight away.
            refreshCameraAccess(askIfNeverAsked = true)
        }
        binding.cvCameraAccess.setContent {
            AppTheme(useDarkTheme = false) {
                CameraAccessPanel(
                    access = cameraAccess,
                    onAllow = ::askForCamera,
                    onOpenSettings = ::openAppSettings,
                )
            }
        }
        binding.btnScanHelp.setOnClickListener { showQRIntroFragment() }
        binding.btnScanTorch.setOnClickListener { toggleTorch() }
        // Uncover the preview only once the camera is really streaming, with a short fade.
        previewView.previewStreamState.observe(viewLifecycleOwner) { state ->
            if (state == PreviewView.StreamState.STREAMING && camera != null) {
                binding.cameraPreviewCover.animate().alpha(0f).setDuration(PREVIEW_FADE_MS).start()
            }
        }

        initObservers()
        viewModel.getUserData()
        viewModel.getUserMeasures()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNewMeasures()
        // Back from the app settings (or the permission was changed meanwhile): start or explain.
        if (scannerShown && camera == null) refreshCameraAccess(askIfNeverAsked = false)
    }

    private val scannerShown: Boolean
        get() = view != null && binding.layoutContentQR.isVisible

    override fun initEvent() {
    }

    private fun initObservers() {
        qrViewModel.scanRead.observeEvent(viewLifecycleOwner) {
            // Tell the user at once that the code was read; the sheet follows the linking.
            previewView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            ScanLinkSheet.show(requireActivity().supportFragmentManager)
        }
        qrViewModel.foreignCode.observeEvent(viewLifecycleOwner) {
            binding.tvScanHint.removeCallbacks(restoreHint)
            binding.tvScanHint.setText(R.string.scan_not_kamleon)
            binding.tvScanHint.postDelayed(restoreHint, FOREIGN_NOTICE_MS)
        }
    }

    /** Opens the scanner tab, as if the user tapped it (used by Home's empty state). */
    fun openScanner() {
        binding.layoutTabQr.performClick()
    }

    private fun goHome() {
        showDestination(R.id.homeFragment, R.id.action_to_homeFragment)
        selectTab(0)
        binding.navHostFragmentTab.visibility = View.VISIBLE
    }

    /**
     * Puts [destination] in the tab content, building it right away: callers hide the scanner (or show
     * the content) just after, and the screen that was underneath must not show for a moment while
     * the fragment transaction waits for the next frame. Already there: kept as it is.
     */
    private fun showDestination(@IdRes destination: Int, @IdRes action: Int) {
        val host = binding.navHostFragmentTab.getFragment<NavHostFragment>()
        val navController = host.navController
        if (navController.currentDestination?.id == destination) return
        navController.popBackStack()
        navController.navigate(action)
        host.childFragmentManager.executePendingTransactions()
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
        binding.layoutScanControls.visibility = View.GONE
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
        childFragmentManager.showScanIntroSheet(requireContext())
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
        private const val FOREIGN_NOTICE_MS = 2_500L
        private const val PREVIEW_FADE_MS = 150L
    }


    // Scan
    /** What the scanner shows: the camera, or the panel that says why it can't use it. */
    private var cameraAccess by mutableStateOf(CameraAccess.Granted)
    private var cameraStarting = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (!granted) DebugLog.d(TAG) { "Camera permission denied" }
        // The scanner stays open either way: with the camera, or with the panel explaining it.
        if (scannerShown) refreshCameraAccess(askIfNeverAsked = false)
    }

    private fun currentCameraAccess(): CameraAccess {
        val context = requireContext()
        return cameraAccessOf(
            hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
            askedBefore = SharedPrefUtil(context).getBoolean(PREF_CAMERA_ASKED, false),
            showRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA),
        )
    }

    /**
     * Starts the camera when it may be used, or shows the panel that says why not. With
     * [askIfNeverAsked], a first visit asks for the permission right away instead.
     */
    private fun refreshCameraAccess(askIfNeverAsked: Boolean) {
        val access = currentCameraAccess()
        val neverAsked = !SharedPrefUtil(requireContext()).getBoolean(PREF_CAMERA_ASKED, false)
        if (access == CameraAccess.Ask && askIfNeverAsked && neverAsked) {
            // Keep the scanner empty while Android's own dialog is up.
            showCameraAccess(CameraAccess.Granted, cameraReady = false)
            askForCamera()
            return
        }
        showCameraAccess(access, cameraReady = access == CameraAccess.Granted)
        if (access == CameraAccess.Granted) startCamera()
    }

    private fun askForCamera() {
        SharedPrefUtil(requireContext()).saveBoolean(PREF_CAMERA_ASKED, true)
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    /** The panel when the camera can't be used; the frame, hint and buttons only when it can. */
    private fun showCameraAccess(access: CameraAccess, cameraReady: Boolean) {
        cameraAccess = access
        binding.cvCameraAccess.visibility = if (access == CameraAccess.Granted) View.GONE else View.VISIBLE
        val scannerUi = if (cameraReady) View.VISIBLE else View.INVISIBLE
        binding.ivScanFrame.visibility = scannerUi
        binding.tvScanHint.visibility = scannerUi
        binding.scanOverlay.visibility = scannerUi
        binding.layoutScanControls.visibility = if (cameraReady) View.VISIBLE else View.GONE
    }

    private fun startCamera() {
        if (camera != null || cameraStarting) return
        cameraStarting = true
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            cameraStarting = false
            // The user may have left the scanner, or the screen, while the camera was getting ready.
            if (!scannerShown) return@addListener
            try {
                bindCameraPreview(providerFuture.get())
                showQRIntroTheFirstTime()
            } catch (e: Exception) {
                // No usable back camera (or it is taken): say so instead of a black screen.
                DebugLog.e(TAG, e) { "startCamera: ${e.message}" }
                showCameraAccess(CameraAccess.NoCamera, cameraReady = false)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        if (camera == null) return
        ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
        camera = null
        binding.cameraPreviewCover.animate().cancel()
        binding.cameraPreviewCover.alpha = 1f
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
        // The flashlight button shows from the start (most phones have one) and only goes away when
        // there is none, so it never pops in a moment after the rest; its icon follows the torch.
        if (!boundCamera.cameraInfo.hasFlashUnit()) binding.btnScanTorch.visibility = View.INVISIBLE
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

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null))
        )
    }

    /** How the scan sheet closed, while this screen is there. */
    fun onScanLinkClosed(state: ScanLinkState) {
        if (!scannerShown) return
        when (state) {
            // Linked: back to Home, where the results will appear.
            ScanLinkState.Linked -> {
                childFragmentManager.dismissKamleonSheet(ScanSheets.SCAN_INTRO)
                goHome()
            }
            // It didn't work: the scanner reads again.
            is ScanLinkState.Failed -> qrViewModel.startAnalyzing()
            else -> Unit
        }
    }
}
