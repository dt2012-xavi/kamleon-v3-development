package com.dynatech2012.kamleonuserapp.activities

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
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.PUSH_NOTIFICATION
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants
import com.dynatech2012.kamleonuserapp.databinding.ActivityMainBinding
import com.dynatech2012.kamleonuserapp.extensions.px
import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.models.observeEvent
import com.dynatech2012.kamleonuserapp.utils.notificationPermissionAsked
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.QrViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: MainViewModel by viewModels()
    private val qrViewModel: QrViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        initObservers()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent
        handleIntent(intent)
    }

    /** Uploads the scan that InitActivity passed on from a /scan link, if there is one. */
    private fun handleIntent(intent: Intent) {
        val unitId = intent.getStringExtra(Constants.SCAN_UNIT_ID)
        val sessionId = intent.getStringExtra(Constants.SCAN_SESSION_ID)
        if (unitId.isNullOrEmpty() || sessionId.isNullOrEmpty()) return
        // Remove the extras so a recreated activity does not upload the same scan again.
        intent.removeExtra(Constants.SCAN_UNIT_ID)
        intent.removeExtra(Constants.SCAN_SESSION_ID)
        DebugLog.d(TAG) { "Scan link received by main activity" }
        qrViewModel.uploadQRtoFirestore(QRResponse(unitId = unitId, sessionId = sessionId))
    }

    override fun setBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            val galleryUri = it
            viewModel.setImageUri(galleryUri)
        }
        supportFragmentManager
            .setFragmentResultListener(Constants.PICK_IMAGE, this) { _, bundle ->
                Log.d(TAG, "result from activity pick")
                val result = bundle.getBoolean(Constants.PICK_IMAGE_BUNDLE)
                if (result) {
                    galleryLauncher.launch("image/*")
                }
            }
        supportFragmentManager
            .setFragmentResultListener(Constants.TAKE_IMAGE, this) { _, bundle ->
                Log.d(TAG, "result from activity take")
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
        onBackPressedDispatcher.addCallback {
            //binding.root.findNavController().navigateUp()
        }

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

        qrViewModel.qrUploaded.observeEvent(this) { success ->
            DebugLog.d(TAG) { "QR Upload finished; success: $success" }
            if (success) showScannedDialog() else showScanFailedDialog()
        }
    }

    private fun showScannedDialog() {
        supportFragmentManager.showKamleonSheet(
            "scanned",
            SheetSpec(
                title = getString(com.dynatech2012.kamleonuserapp.R.string.dialog_scan_scanned),
                message = "",
                primary = getString(com.dynatech2012.kamleonuserapp.R.string.dialog_ok),
                dismissible = false
            )
        )
    }

    private fun showScanFailedDialog() {
        supportFragmentManager.showKamleonSheet(
            "scan_failed",
            SheetSpec(
                title = getString(com.dynatech2012.kamleonuserapp.R.string.dialog_scan_failed_title),
                message = getString(com.dynatech2012.kamleonuserapp.R.string.dialog_scan_failed_message),
                primary = getString(com.dynatech2012.kamleonuserapp.R.string.dialog_ok)
            )
        )
    }

    private var firebaseMessagingReceiver: BroadcastReceiver? = null

    private fun setupFirebaseMessagingReceiver() {
        firebaseMessagingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == PUSH_NOTIFICATION) {
                    Log.d(TAG, "onGetNotification from Main Activity")
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
            Log.e(TAG, "Directory not created")
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
                Log.i("Permission: ", "Granted")
                takePicture.launch(currentImageUri)
            } else {
                Log.i("Permission: ", "Denied")
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
        if (getPreferencesUserAskerForPermission) return
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                savePreferencesUserAskerForPermission()
                return
            } else requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else savePreferencesUserAskerForPermission()
    }

    private fun savePreferencesUserAskerForPermission() {
        notificationPermissionAsked = true
    }

    private val getPreferencesUserAskerForPermission: Boolean
        get() = notificationPermissionAsked

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
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

    fun dpToPx(context: Context, i: Int): Int {
        return (i.toFloat() * context.resources.displayMetrics.density).toInt()
    }
}
