package com.dynatech2012.kamleonuserapp.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.databinding.ActivityInitBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InitActivity : BaseActivity<ActivityInitBinding>() {
    private val viewModel: AuthViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content: View = findViewById(android.R.id.content)
        viewModel.resetLogged()
        viewModel.checkLogin()
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    Log.d(TAG, "activity ready?")
                    // Check if the initial data is ready.
                    return if (viewModel.isReady) {
                        // The content is ready; start drawing.
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        if (!viewModel.alreadyLogged) {
                            Log.d(TAG, "not logged, keep in init activity")
                        } else {
                            Log.d(TAG, "already logged")
                            startActivity(Intent(applicationContext, MainActivity::class.java))
                        }
                        true
                    } else {
                        // The content is not ready; suspend.
                        false
                    }
                }
            }
        )
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("Permission: ", "Granted")
                viewModel.locationPermissionGranted()
            } else {
                Log.i("Permission: ", "Denied")
            }
        }
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("Permission: ", "Granted")
                viewModel.notificationPermissionGranted()
            } else {
                Log.i("Permission: ", "Denied")
            }
        }

    private fun askLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.locationPermissionGranted()
                return
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION
                ) }

            else -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    viewModel.notificationPermissionGranted()
                    return
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else ->
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    override fun setBinding(): ActivityInitBinding = ActivityInitBinding.inflate(layoutInflater)

    override fun initView() {
        supportFragmentManager
            .setFragmentResultListener(Constants.GRANT_LOCATION, this) { _, bundle ->
                Log.d(TAG, "result from activity location")
                val result = bundle.getBoolean(Constants.GRANT_LOCATION_BUNDLE)
                if (result) {
                    askLocationPermission()
                }
            }
        supportFragmentManager
            .setFragmentResultListener(Constants.GRANT_NOTIFICATION, this) { _, bundle ->
                Log.d(TAG, "result from activity notification")
                val result = bundle.getBoolean(Constants.GRANT_NOTIFICATION_BUNDLE)
                if (result) {
                    askNotificationPermission()
                }
            }
    }

    override fun initEvent() { }

    companion object {
        val TAG: String = InitActivity::class.java.simpleName
    }
}