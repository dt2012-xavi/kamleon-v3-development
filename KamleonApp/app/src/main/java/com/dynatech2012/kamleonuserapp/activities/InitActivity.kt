package com.dynatech2012.kamleonuserapp.activities

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.databinding.ActivityInitBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.dynatech2012.kamleonuserapp.utils.notificationPermissionAsked
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class InitActivity : BaseActivity<ActivityInitBinding>() {
    private val viewModel: AuthViewModel by viewModels()
    private var mainOpened = false
    private var resetRestoredFlow = false

    // Ask the NavHostFragment, not the view: after process death onCreate runs before the
    // container view has its NavController, and findNavController(viewId) throws there.
    private val navController: NavController
        get() = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_init) as NavHostFragment)
            .navController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetRestoredFlow = savedInstanceState != null && !viewModel.appInitializationStarted &&
            navController.currentDestination?.id != R.id.splashFragment
        // On a restore the scan is already in the saved state with its original time; reading the
        // launch intent again would restart the clock and keep an old scan alive.
        if (savedInstanceState == null) handleIntent(intent)
        viewModel.splashDurationCompleted.observe(this) { completed ->
            if (completed == true) route(viewModel.startupState.value)
        }
        viewModel.startupState.observe(this) { route(it) }
        // The session is checked while the splash plays; route() waits for both to be done.
        if (!viewModel.appInitializationStarted) {
            viewModel.appInitializationStarted = true
            viewModel.checkLogin()
        }
        if (resetRestoredFlow) viewModel.signalSplashDurationCompleted()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        if (viewModel.splashDurationCompleted.value == true) viewModel.checkLogin()
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        if (data?.path != "/scan") return
        val unitId = data.getQueryParameter(Constants.SCAN_UNIT_ID)
        val sessionId = data.getQueryParameter(Constants.SCAN_SESSION_ID)
        if (!unitId.isNullOrBlank() && !sessionId.isNullOrBlank()) {
            viewModel.pendingScan(unitId, sessionId)
            Timber.d("Scan link accepted for later delivery")
        } else {
            Timber.w("Scan link missing required parameters")
        }
    }

    private fun route(state: AuthViewModel.StartupState?) {
        if (viewModel.splashDurationCompleted.value != true || mainOpened) return
        when (state) {
            AuthViewModel.StartupState.Ready -> openMain()
            AuthViewModel.StartupState.NeedsLogin, AuthViewModel.StartupState.NeedsPolicy -> {
                if (resetRestoredFlow || navController.currentDestination?.id == R.id.splashFragment) {
                    resetRestoredFlow = false
                    if (navController.currentDestination?.id != R.id.loginFragment) {
                        navController.navigate(R.id.action_global_loginFragment)
                    }
                }
            }
            AuthViewModel.StartupState.NeedsProfile -> {
                if (resetRestoredFlow || navController.currentDestination?.id == R.id.splashFragment ||
                    navController.currentDestination?.id == R.id.loginFragment) {
                    resetRestoredFlow = false
                    if (navController.currentDestination?.id != R.id.registerFragment) {
                        navController.navigate(R.id.action_global_registerFragment)
                    }
                }
            }
            AuthViewModel.StartupState.RetryableError -> {
                if (resetRestoredFlow || navController.currentDestination?.id == R.id.splashFragment) {
                    showStartupErrorSheet()
                }
            }
            else -> Unit
        }
    }

    /** The account could not be loaded: the only way on is to try again, so the sheet is not dismissible. */
    private fun showStartupErrorSheet() {
        supportFragmentManager.showKamleonSheet(
            STARTUP_ERROR_SHEET,
            SheetSpec(
                title = getString(R.string.startup_error_title),
                message = getString(R.string.startup_error_message),
                primary = getString(R.string.retry),
                dismissible = false
            )
        )
    }

    private fun retryStartupCheck() {
        lifecycleScope.launch {
            // showKamleonSheet ignores a sheet that is still on screen, and this one takes a moment
            // to close: a failure that comes back at once would leave no message at all. The pause
            // also makes the retry visible (the logo keeps playing) instead of a flicker.
            delay(RETRY_FEEDBACK_MS)
            viewModel.checkLogin()
        }
    }

    fun openMain() {
        if (mainOpened) return
        mainOpened = true
        // A scan link opens InitActivity on top of a running MainActivity: reuse it (onNewIntent)
        // instead of stacking another one for every link.
        val mainIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        viewModel.consumePendingScan()?.let { (unitId, sessionId) ->
            mainIntent.action = Constants.SCAN_ACTION
            mainIntent.putExtra(Constants.SCAN_UNIT_ID, unitId)
            mainIntent.putExtra(Constants.SCAN_SESSION_ID, sessionId)
        }
        // Cross-fade into the main screen instead of the system's default activity transition.
        startActivity(mainIntent, ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_hold).toBundle())
        finish()
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Granted or not, the user has answered: MainActivity must not ask again.
            notificationPermissionAsked = true
            viewModel.notificationPermissionGranted()
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionAsked = true
            viewModel.notificationPermissionGranted()
        } else {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun setBinding(): ActivityInitBinding = ActivityInitBinding.inflate(layoutInflater)

    override fun initView() {
        supportFragmentManager.setFragmentResultListener(Constants.GRANT_NOTIFICATION, this) { _, bundle ->
            if (bundle.getBoolean(Constants.GRANT_NOTIFICATION_BUNDLE)) askNotificationPermission()
        }
        onKamleonSheetResult(STARTUP_ERROR_SHEET) { if (it == SheetResult.PRIMARY) retryStartupCheck() }
    }

    override fun initEvent() {}

    private companion object {
        const val STARTUP_ERROR_SHEET = "startup_error"
        const val RETRY_FEEDBACK_MS = 600L
    }
}
