package com.dynatech2012.kamleonuserapp.activities

import com.dynatech2012.kamleonuserapp.viewmodels.ScanLinkState
import com.dynatech2012.kamleonuserapp.fragments.TabFragment
import android.Manifest
import android.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.PUSH_NOTIFICATION
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants
import com.dynatech2012.kamleonuserapp.databinding.ActivityMainBinding
import com.dynatech2012.kamleonuserapp.extensions.px
import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.utils.NotificationAsk
import com.dynatech2012.kamleonuserapp.utils.notificationAskFor
import com.dynatech2012.kamleonuserapp.utils.notificationPermissionAsked
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.ScanLinkSheet
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.viewmodels.ErrorCode
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), ScanLinkSheet.Host {

    private val viewModel: MainViewModel by viewModels()
    private val qrViewModel: QrViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openInstances++
        if (savedInstanceState == null) handleIntent(intent)
        initObservers()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent
        handleIntent(intent)
    }

    /** Links the scan that InitActivity passed on from a /scan link, or says it can't be used. */
    private fun handleIntent(intent: Intent) {
        val unitId = intent.getStringExtra(Constants.SCAN_UNIT_ID)
        val sessionId = intent.getStringExtra(Constants.SCAN_SESSION_ID)
        val invalid = intent.getBooleanExtra(Constants.SCAN_INVALID, false)
        // Remove the extras so a recreated activity does not handle the same scan again.
        intent.removeExtra(Constants.SCAN_UNIT_ID)
        intent.removeExtra(Constants.SCAN_SESSION_ID)
        intent.removeExtra(Constants.SCAN_INVALID)
        when {
            !unitId.isNullOrEmpty() && !sessionId.isNullOrEmpty() -> {
                DebugLog.d(TAG) { "Scan link received by main activity" }
                qrViewModel.linkFromAppLink(QRResponse(unitId = unitId, sessionId = sessionId))
            }
            invalid -> qrViewModel.appLinkInvalid()
            else -> return
        }
        ScanLinkSheet.show(supportFragmentManager)
    }

    override fun onDestroy() {
        openInstances--
        super.onDestroy()
    }

    override fun onScanLinkClosed(state: ScanLinkState, result: SheetResult) {
        if (state == ScanLinkState.Invalid) {
            if (result == SheetResult.PRIMARY) openScanner()
            return
        }
        tabFragment()?.onScanLinkClosed(state)
    }

    private fun tabFragment(): TabFragment? {
        val navHost = supportFragmentManager.findFragmentById(
            com.dynatech2012.kamleonuserapp.R.id.nav_host_fragment_main
        ) as NavHostFragment
        return navHost.childFragmentManager.primaryNavigationFragment as? TabFragment
    }

    /** From the sheet of a scan link that can't be used: open the in-app scanner. */
    private fun openScanner() {
        val navHost = supportFragmentManager.findFragmentById(
            com.dynatech2012.kamleonuserapp.R.id.nav_host_fragment_main
        ) as NavHostFragment
        val navController = navHost.navController
        if (navController.currentDestination?.id != com.dynatech2012.kamleonuserapp.R.id.tabFragment) {
            navController.popBackStack(com.dynatech2012.kamleonuserapp.R.id.tabFragment, false)
            navHost.childFragmentManager.executePendingTransactions()
        }
        (navHost.childFragmentManager.primaryNavigationFragment as? TabFragment)?.openScanner()
    }

    override fun setBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        setupSystemBarInsets()
        // Android's photo picker: no storage permission, and the photos screen people expect.
        val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Cancelled: the photo sheet stays open.
            uri?.let(viewModel::setImageUri)
        }
        supportFragmentManager
            .setFragmentResultListener(Constants.PICK_IMAGE, this) { _, bundle ->
                DebugLog.d(TAG) { "result from activity pick" }
                val result = bundle.getBoolean(Constants.PICK_IMAGE_BUNDLE)
                if (result) {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
        supportFragmentManager
            .setFragmentResultListener(Constants.TAKE_IMAGE, this) { _, bundle ->
                DebugLog.d(TAG) { "result from activity take" }
                val result = bundle.getBoolean(Constants.TAKE_IMAGE_BUNDLE)
                if (result) {
                    takePicture()
                }
            }
        // TODO: trying to disable swipe navigation gesture
        /*
        binding.root.apply {
            doOnLayout {
                // updating exclusion rect
                val rects = mutableListOf<Rect>()
                rects.add(Rect(0,(height/2 - 75).px, 150.px, 150.px))
                //rects.add(Rect(width.px - 150.px,0, 150.px, (height.px/2)))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.d(TAG, "setting systemGestureExclusionRects")
                    systemGestureExclusionRects = rects
                }
            }
        }
        val content = binding.root
        val treeObserver = content.viewTreeObserver;
        treeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                content.viewTreeObserver.removeOnGlobalLayoutListener(this);
                //updateGestureExclusion(this@MainActivity)
            }
        })
        */
        onKamleonSheetResult(NOTIFICATIONS_INTRO_SHEET) {
            // The sheet is only shown from Android 13, where this permission exists.
            if (it == SheetResult.PRIMARY && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        onKamleonSheetResult(SESSION_ENDED_SHEET) { endSession() }
        askNotificationPermission()
    }

    private fun initObservers() {
        /*
        NotificationService.Notification.instance?.getNewOrderBoolean()?.observe(this) {
            Log.d(TAG, "notification received")
            if (it) {
                viewModel.onGetNotification()
                NotificationService.Notification.instance?.reset()
            }
        }
         */
        setupFirebaseMessagingReceiver()
        viewModel.updateUserToken()

        // The server ended the session (account disabled or deleted, sign-in revoked): say so, then sign out.
        viewModel.sessionEnded.observe(this) { code -> if (code != null) showSessionEndedSheet(code) }

    }

    /** Only way on is OK: it wipes this phone's data and signs out, as the same sheet does at startup. */
    private fun showSessionEndedSheet(code: ErrorCode) {
        supportFragmentManager.showKamleonSheet(
            SESSION_ENDED_SHEET,
            SheetSpec(
                title = getString(com.dynatech2012.kamleonuserapp.R.string.session_ended_title),
                message = getString(com.dynatech2012.kamleonuserapp.R.string.session_ended_message) + "\n\n" +
                    getString(com.dynatech2012.kamleonuserapp.R.string.error_code_line, code.label),
                primary = getString(com.dynatech2012.kamleonuserapp.R.string.dialog_ok),
                dismissible = false
            )
        )
    }

    private var endingSession = false

    private fun endSession() {
        if (endingSession) return
        endingSession = true
        viewModel.logoutStatus.observe(this) {
            if (it) {
                // Clear the whole task: no screen of the ended session may stay underneath.
                startActivity(
                    Intent(this, InitActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }
        }
        viewModel.logout()
    }

    private var firebaseMessagingReceiver: BroadcastReceiver? = null

    private fun setupFirebaseMessagingReceiver() {
        firebaseMessagingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == PUSH_NOTIFICATION) {
                    DebugLog.d(TAG) { "onGetNotification from Main Activity" }
                    // what happens when a notification is received while the user is in this activity
                    viewModel.onGetNotification()
                }
            }
        }
    }

    override fun initEvent() {

    }

    private lateinit var currentImageUri: Uri

    private fun takePicture() {
        /*
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getPermission = Intent()
                getPermission.action = ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getPermission)
                requestPermissionLauncher.launch(
                    ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                )
            }

        }*/
        //val root = File(getExternalStorageDirectory()/*cacheDir*/, "my_images")
        val root = File(
            getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            ), "my_images"
        )
        //val root = File(filesDir/*cacheDir*/, "my_images")
        // Create request to write to media storage
        if (!root.mkdirs()) {
            DebugLog.e(TAG) { "Directory not created" }
        }
        val fname = "img_${System.currentTimeMillis()}.jpg"
        val sdImageMainDirectory = File(root, fname)
        //sdImageMainDirectory.createNewFile()
        /*val sdImageMainDirectory = File.createTempFile ("img_${System.currentTimeMillis()}", ".jpg", getExternalStorageDirectory()).apply {
            createNewFile()
        }*/
        currentImageUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            sdImageMainDirectory
        )
        checkCameraPermission()
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                // The image was saved into the given Uri -> do something with it
                viewModel.setImageUri(currentImageUri)
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                DebugLog.i("Permission: ") { "Granted" }
                takePicture.launch(currentImageUri)
            } else {
                DebugLog.i("Permission: ") { "Denied" }
            }
        }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePicture.launch(currentImageUri)
                return
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }

            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            firebaseMessagingReceiver!!,
            IntentFilter(PUSH_NOTIFICATION)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(firebaseMessagingReceiver!!)
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _: Boolean ->
            savePreferencesUserAskerForPermission()
        }

    private fun askNotificationPermission() {
        val granted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        when (notificationAskFor(
            sdkInt = Build.VERSION.SDK_INT,
            alreadyAsked = getPreferencesUserAskerForPermission,
            granted = granted,
            fromScanLink = intent.hasExtra(Constants.SCAN_UNIT_ID)
        )) {
            NotificationAsk.Skip -> Unit
            NotificationAsk.MarkAsked -> savePreferencesUserAskerForPermission()
            NotificationAsk.ExplainFirst -> showNotificationsIntroSheet()
        }
    }

    /**
     * Users who arrive with an account that already exists never saw the onboarding step that
     * explains the notifications. "Not now" leaves the flag unset, so it is offered again next time.
     */
    private fun showNotificationsIntroSheet() {
        supportFragmentManager.showKamleonSheet(
            NOTIFICATIONS_INTRO_SHEET,
            SheetSpec(
                title = getString(com.dynatech2012.kamleonuserapp.R.string.onboard_noti_sub1),
                // Same text as the onboarding step: one explanation wherever it is asked.
                message = getString(com.dynatech2012.kamleonuserapp.R.string.onboard_noti_sub2),
                primary = getString(com.dynatech2012.kamleonuserapp.R.string.notifications_intro_activate),
                secondary = getString(com.dynatech2012.kamleonuserapp.R.string.notifications_intro_later),
                dismissible = false
            )
        )
    }

    private fun savePreferencesUserAskerForPermission() {
        notificationPermissionAsked = true
    }

    private val getPreferencesUserAskerForPermission: Boolean
        get() = notificationPermissionAsked

    companion object {
        val TAG: String = MainActivity::class.java.simpleName

        /**
         * Whether the main screen is open, so the user is signed in: a scan link then goes straight to
         * it instead of through the splash. Main thread only.
         */
        val isRunning: Boolean get() = openInstances > 0
        private var openInstances = 0
        private const val NOTIFICATIONS_INTRO_SHEET = "notifications_intro"
        private const val SESSION_ENDED_SHEET = "main_session_ended"
    }

    /**
     * The tab screen (Home, Analytics, Scan) draws under the status and navigation bars, and its
     * screens keep their own content clear of them. Every other screen keeps the old behaviour:
     * the whole screen is padded inside the bars (and above the keyboard).
     */
    private fun setupSystemBarInsets() {
        val root = binding.root
        val navHost = supportFragmentManager.findFragmentById(com.dynatech2012.kamleonuserapp.R.id.nav_host_fragment_main) as NavHostFragment
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            if (navHost.navController.currentDestination?.id == com.dynatech2012.kamleonuserapp.R.id.tabFragment) {
                v.setPadding(0, 0, 0, 0)
                insets
            } else {
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
                )
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
        root.post {
            navHost.navController.addOnDestinationChangedListener { _, _, _ -> ViewCompat.requestApplyInsets(root) }
        }
    }

    private var exclusionRects: MutableList<Rect> = ArrayList()

    fun updateGestureExclusion(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT < 29) return
        exclusionRects.clear()
        val rect = Rect(0, 0, 150.px, getScreenHeight(activity))
        exclusionRects.add(rect)
        binding.root.systemGestureExclusionRects = exclusionRects
        activity.findViewById<View>(R.id.content).systemGestureExclusionRects =
            exclusionRects
    }

    private fun getScreenHeight(activity: AppCompatActivity): Int {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

}
