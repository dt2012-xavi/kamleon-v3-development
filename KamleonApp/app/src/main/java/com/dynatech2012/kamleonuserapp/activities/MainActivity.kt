package com.dynatech2012.kamleonuserapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.databinding.ActivityMainBinding
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
            .setFragmentResultListener(Constants.pickImage, this) { _, bundle ->
                Log.d(SettingFragment.TAG, "result from activity pick")
                val result = bundle.getBoolean(Constants.pickImageBundle)
                if (result) {
                    galleryLauncher.launch("image/*")
                }
            }
        supportFragmentManager
            .setFragmentResultListener(Constants.takeImage, this) { _, bundle ->
                Log.d(SettingFragment.TAG, "result from activity take")
                val result = bundle.getBoolean(Constants.takeImageBundle)
                if (result) {
                    takePicture()
                }
            }
    }

    override fun initEvent() {

    }

    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var currentImageUri: Uri


    private fun takePicture() {
        /*
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getpermission = Intent()
                getpermission.action = ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getpermission)
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

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }
}