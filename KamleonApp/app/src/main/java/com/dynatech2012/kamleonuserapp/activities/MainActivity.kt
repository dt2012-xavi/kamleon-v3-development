package com.dynatech2012.kamleonuserapp.activities

import android.Manifest
import android.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.PUSH_NOTIFICATION
import com.dynatech2012.kamleonuserapp.databinding.ActivityMainBinding
import com.dynatech2012.kamleonuserapp.extensions.px
import com.dynatech2012.kamleonuserapp.fragments.SettingFragment
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private val viewModel: MainViewModel by viewModels()
    override fun setBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            val galleryUri = it
            viewModel.setImageUri(galleryUri)
        }


        supportFragmentManager
            .setFragmentResultListener(Constants.PICK_IMAGE, this) { _, bundle ->
                Log.d(SettingFragment.TAG, "result from activity pick")
                val result = bundle.getBoolean(Constants.PICK_IMAGE_BUNDLE)
                if (result) {
                    galleryLauncher.launch("image/*")
                }
            }
        supportFragmentManager
            .setFragmentResultListener(Constants.TAKE_IMAGE, this) { _, bundle ->
                Log.d(SettingFragment.TAG, "result from activity take")
                val result = bundle.getBoolean(Constants.TAKE_IMAGE_BUNDLE)
                if (result) {
                    takePicture()
                }
            }
        initObservers()


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
    }


    private var firebaseMessagingReceiver: BroadcastReceiver? = null

    private fun setupFirebaseMessagingReceiver() {
        firebaseMessagingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == PUSH_NOTIFICATION) {
                    // what happens when a notification is received while the user is in this activity
                    viewModel.onGetNotification()
                } } } }

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
        val root = File(getExternalFilesDir(
            Environment.DIRECTORY_PICTURES), "my_images")
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
        currentImageUri = FileProvider.getUriForFile(this, /*applicationContext?.packageName + ".fileprovider"*/"com.dynatech2012.kamleonuserapp.fileprovider", sdImageMainDirectory)
        checkCameraPermission()
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
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
                    ) }

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
            askLocationPermission()
        }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED) {
                askLocationPermission()
                return
            }
            else requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private val requestLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _: Boolean ->

        }

    private fun askLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

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