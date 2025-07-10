package com.dynatech2012.kamleonuserapp.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.databinding.ActivityInitBinding
import com.dynatech2012.kamleonuserapp.fragments.SplashFragment
import com.dynatech2012.kamleonuserapp.fragments.SplashFragment.Companion
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InitActivity : BaseActivity<ActivityInitBinding>() {
    private val viewModel: AuthViewModel by viewModels()

    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "XXX init activity handle intent")
        viewModel.resetLogged()

        val data = intent.data
        Log.d(TAG, "XXX init activity handle intent data: $data")
        Log.d(TAG, "XXX init activity handle intent action: ${intent.action}")
        Log.d(TAG, "XXX init activity handle intent type: ${data?.path}")
        Log.d(TAG, "XXX init activity handle intent query: ${data?.query}")
        // Check if the intent is a deeplink and has the expected path
        val isNull = data == null
        val isPathScan = data?.path == "/scan"
        Log.d(TAG, "XXX init activity handle intent isNull: $isNull, isPathScan: $isPathScan")
        if (data != null && data.path == "/scan") {
            Log.d(TAG, "XXX Deeplink detected with path: ${data.path}")
            val unitId = data.getQueryParameter("unitId")
            val sessionId = data.getQueryParameter("sessionId")
            if (!unitId.isNullOrEmpty() && !sessionId.isNullOrEmpty()) {
                Log.d(TAG, "XXX Deeplink params found: unitId=$unitId, sessionId=$sessionId")
                viewModel.unitId = unitId
                viewModel.sessionId = sessionId
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "XXX init activity on new intent")
        intent?.let {
            setIntent(it) // Update the current intent
            handleIntent(it)
            viewModel.resetLogged() // Reset the logged state
            viewModel.checkLogin() // Check login status again
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content: View = findViewById(android.R.id.content)
        Log.d(TAG, "XXX init activity on create")
        handleIntent(intent)
        observeSplashCompletion()

        /*
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
         */
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "XXX init activity on start")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "XXX init activity on resume")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "XXX init activity on stop")
        clearNavigationObservers()
    }

    /*
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
    */

    private fun observeSplashCompletion() {
        Log.d(TAG, "XXX init activity observing splash completion")
        viewModel.splashDurationCompleted.observe(this) { completed ->
            // Check the flag to ensure this logic runs only once
            if (completed == true && !viewModel.appInitializationStarted) {
                viewModel.appInitializationStarted = true // Mark that app initialization has started
                Log.d(TAG, "Splash completed. Proceeding with checkLogin and setting up navigation observers.")
                viewModel.checkLogin()          // Now check login status
                setupNavigationObservers()      // Setup observers that handle navigation
            }
        }
    }

    private fun clearNavigationObservers() {
        Log.d(TAG, "XXX init activity removing navigation observers")
        viewModel.isReady.removeObservers(this)
        // Optionally remove splashDurationCompleted observer if desired, though the flag prevents re-entry
        // viewModel.splashDurationCompleted.removeObservers(this)
    }

    private fun setupNavigationObservers() {
        viewModel.isReady.observe(this) { isReadyValue ->
            Log.d(TAG, "XXX init activity isReady observer triggered with value: $isReadyValue")
            if (isReadyValue) { // Make sure to use the value of LiveData
                Log.d(TAG, "XXX isReady true - alreadyLogged: ${viewModel.alreadyLogged}, alreadyVerified: ${viewModel.alreadyVerified}, alreadyPolicy: ${viewModel.alreadyPolicy}")
                if (viewModel.alreadyLogged && viewModel.alreadyVerified && viewModel.alreadyPolicy) {
                    Log.d(TAG, "XXX already logged, navigating to MainActivity")
                    val intentAction = if (viewModel.unitId != null && viewModel.sessionId != null) "scan" else null
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        if (intentAction == "scan") {
                            action = "scan"
                            putExtra("unitId", viewModel.unitId)
                            putExtra("sessionId", viewModel.sessionId)
                        }
                    }
                    startActivity(mainIntent)
                    finish() // Finish InitActivity to prevent going back to splash/login
                } else {
                    Log.d(TAG, "XXX not logged, navigating to LoginFragment")
                    findNavController(R.id.nav_host_fragment_init).navigate(R.id.loginFragment)
                }
            }
        }
    }

    private fun removeObservers() {
        Log.d(TAG, "XXX init activity remove observers")
        viewModel.isReady.removeObservers(this)
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

    /*
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
    */

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
        } else {
            viewModel.notificationPermissionGranted()
        }
    }

    override fun setBinding(): ActivityInitBinding = ActivityInitBinding.inflate(layoutInflater)

    override fun initView() {
        /*supportFragmentManager
            .setFragmentResultListener(Constants.GRANT_LOCATION, this) { _, bundle ->
                Log.d(TAG, "result from activity location")
                val result = bundle.getBoolean(Constants.GRANT_LOCATION_BUNDLE)
                if (result) {
                    askLocationPermission()
                }
            }
        */
        supportFragmentManager
            .setFragmentResultListener(Constants.GRANT_NOTIFICATION, this) { _, bundle ->
                Log.d(
                    TAG,
                    "result from activity notification - ${bundle.getBoolean(Constants.GRANT_NOTIFICATION_BUNDLE)}"
                )
                val result = bundle.getBoolean(Constants.GRANT_NOTIFICATION_BUNDLE)

                if (result) {
                    askNotificationPermission()
                }
            }
    }

    override fun initEvent() {}

    companion object {
        val TAG: String = InitActivity::class.java.simpleName
    }
}