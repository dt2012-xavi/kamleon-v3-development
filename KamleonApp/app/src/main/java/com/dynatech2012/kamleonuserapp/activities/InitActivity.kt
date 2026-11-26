package com.dynatech2012.kamleonuserapp.activities

import com.google.firebase.auth.FirebaseAuth
import com.dynatech2012.kamleonuserapp.analytics.Analytics
import com.dynatech2012.kamleonuserapp.BuildConfig
import com.dynatech2012.kamleonuserapp.viewmodels.scanLinkFrom
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
import com.dynatech2012.kamleonuserapp.network.NetworkMonitor
import com.dynatech2012.kamleonuserapp.network.retryWhenOnline
import com.dynatech2012.kamleonuserapp.databinding.ActivityInitBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.dynatech2012.kamleonuserapp.utils.notificationPermissionAsked
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.dismissKamleonSheet
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class InitActivity : BaseActivity<ActivityInitBinding>() {
    private val viewModel: AuthViewModel by viewModels()
    @Inject lateinit var networkMonitor: NetworkMonitor
    private var autoRetryJob: Job? = null
    private var resetRestoredFlow = false
    private var verificationNotice: String? = null

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
        if (savedInstanceState == null && forwardScanLinkToRunningMain(intent)) return
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
        if (forwardScanLinkToRunningMain(intent)) return
        handleIntent(intent)
        if (viewModel.splashDurationCompleted.value == true) viewModel.checkLogin()
    }

    private fun handleIntent(intent: Intent) {
        val scan = scanLinkIn(intent) ?: return
        trackCameraLink(scan)
        if (scan is ScanLinkIntent.Valid) {
            viewModel.pendingScan(scan.unitId, scan.sessionId)
            Timber.d("Scan link accepted for later delivery")
        } else {
            viewModel.invalidScanLink()
            Timber.w("Scan link missing required parameters")
        }
    }

    private sealed interface ScanLinkIntent {
        data class Valid(val unitId: String, val sessionId: String) : ScanLinkIntent
        data object Invalid : ScanLinkIntent
    }

    /** The /scan link this intent carries, valid or not; null when it carries none. */
    private fun scanLinkIn(intent: Intent): ScanLinkIntent? {
        val data = intent.data ?: return null
        if (data.path?.trimEnd('/') != "/${Constants.SCAN_ACTION}") return null
        // The same check as the in-app scanner, so both accept exactly the same links.
        val scan = scanLinkFrom(data.toString(), BuildConfig.APP_LINK_HOST)
            ?: return ScanLinkIntent.Invalid
        return ScanLinkIntent.Valid(scan.unitId, scan.sessionId)
    }

    /** A QR attempt starts with every /scan link the phone's camera opens. */
    private fun trackCameraLink(scan: ScanLinkIntent) {
        Analytics.qr?.cameraLinkOpened(
            valid = scan is ScanLinkIntent.Valid,
            signedIn = FirebaseAuth.getInstance().currentUser != null
        )
    }

    /**
     * A scan link while the main screen is already open (so the user is signed in): hand it straight
     * to it, without the splash and the startup checks. Returns whether it did.
     */
    private fun forwardScanLinkToRunningMain(intent: Intent): Boolean {
        if (!MainActivity.isRunning) return false
        val scan = scanLinkIn(intent) ?: return false
        trackCameraLink(scan)
        val mainIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .setAction(Constants.SCAN_ACTION)
        when (scan) {
            is ScanLinkIntent.Valid -> mainIntent
                .putExtra(Constants.SCAN_UNIT_ID, scan.unitId)
                .putExtra(Constants.SCAN_SESSION_ID, scan.sessionId)
            ScanLinkIntent.Invalid -> mainIntent.putExtra(Constants.SCAN_INVALID, true)
        }
        Timber.d("Scan link forwarded to the open main screen")
        startActivity(mainIntent, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle())
        finish()
        return true
    }

    private fun route(state: AuthViewModel.StartupState?) {
        if (viewModel.splashDurationCompleted.value != true || viewModel.mainOpened) return
        val route = startupRouteFor(state, currentScreen(), resetRestoredFlow)
        if (route.endsRestoredFlow) resetRestoredFlow = false
        when (val action = route.action) {
            StartupAction.None -> Unit
            StartupAction.OpenMain -> openMain()
            StartupAction.ShowError -> showStartupErrorSheet()
            StartupAction.ShowServerError -> showStartupServerErrorSheet()
            StartupAction.ShowSessionEnded -> showSessionEndedSheet()
            StartupAction.ShowVerification -> {
                if (currentScreen() != StartupScreen.Login) navController.navigate(R.id.action_global_loginFragment)
                showVerificationSheet()
            }
            StartupAction.ShowPolicy -> {
                if (currentScreen() != StartupScreen.Login) navController.navigate(R.id.action_global_loginFragment)
                viewModel.showPolicy()
            }
            is StartupAction.GoTo -> navController.navigate(
                if (action.screen == StartupScreen.Register) R.id.action_global_registerFragment
                else R.id.action_global_loginFragment
            )
        }
    }

    private fun currentScreen() = when (navController.currentDestination?.id) {
        R.id.splashFragment -> StartupScreen.Splash
        R.id.loginFragment -> StartupScreen.Login
        R.id.registerFragment -> StartupScreen.Register
        else -> StartupScreen.Other
    }

    /** The account could not be loaded: the only way on is to try again, so the sheet is not dismissible. */
    private fun showStartupErrorSheet() {
        waitForTheNetworkToRetry()
        supportFragmentManager.showKamleonSheet(
            STARTUP_ERROR_SHEET,
            SheetSpec(
                title = getString(R.string.startup_error_title),
                message = getString(
                    if (viewModel.continueOffline) R.string.startup_error_message_offline else R.string.startup_error_message
                ),
                primary = getString(R.string.retry),
                secondary = if (viewModel.continueOffline) getString(R.string.startup_continue_offline) else null,
                dismissible = false
            )
        )
    }

    /**
     * With no network, the check repeats itself when it returns and the sheet closes. Not when the
     * network is up: then the failure was a timeout or a busy server, and repeating at once would loop.
     */
    private fun waitForTheNetworkToRetry() {
        autoRetryJob?.cancel()
        if (networkMonitor.isOnline) return
        autoRetryJob = lifecycleScope.launch {
            networkMonitor.retryWhenOnline {
                supportFragmentManager.dismissKamleonSheet(STARTUP_ERROR_SHEET)
                // The sheet takes a moment to leave: a check that fails at once could not show a new one.
                delay(RETRY_FEEDBACK_MS)
                viewModel.checkLogin()
            }
        }
    }

    /**
     * The account could not be loaded for a reason that is not the network, so retrying may never be
     * enough: the user can also log out (which wipes this phone's data) and sign in again. The
     * server answered, so there is network for that.
     */
    private fun showStartupServerErrorSheet() {
        supportFragmentManager.showKamleonSheet(
            STARTUP_SERVER_ERROR_SHEET,
            SheetSpec(
                title = getString(R.string.startup_server_error_title),
                message = withErrorCode(getString(R.string.startup_server_error_message)),
                primary = getString(R.string.retry),
                secondary = getString(R.string.dialog_logout_y),
                dismissible = false
            )
        )
    }

    /**
     * The server refused the session and only a new login can fix it. The only way on is OK, which
     * wipes this phone's data and signs out, so the next person to use the phone does not inherit it.
     */
    private fun showSessionEndedSheet() {
        supportFragmentManager.showKamleonSheet(
            SESSION_ENDED_SHEET,
            SheetSpec(
                title = getString(R.string.session_ended_title),
                message = withErrorCode(getString(R.string.session_ended_message)),
                primary = getString(R.string.dialog_ok),
                dismissible = false
            )
        )
    }

    /**
     * Signed in, but the email is not verified: three ways on, so the user is never stuck with an
     * address they mistyped. [verificationNotice] tells what happened to the last thing they tried.
     */
    private fun showVerificationSheet() {
        val email = viewModel.signedInEmail
        val base = if (email != null) getString(R.string.dialog_verify_desc, email)
        else getString(R.string.login_alert_description_not_verified_email)
        supportFragmentManager.showKamleonSheet(
            STARTUP_VERIFY_SHEET,
            SheetSpec(
                title = getString(R.string.dialog_verify_title),
                message = verificationNotice?.let { it + "\n\n" + base } ?: base,
                primary = getString(R.string.verify_done),
                secondary = getText(R.string.dialog_verify_resend),
                tertiary = getString(R.string.dialog_logout_y),
                dismissible = false
            )
        )
    }

    /** "I've verified it": ask the server again; if it is still not verified the sheet comes back saying so. */
    private fun recheckVerification() {
        lifecycleScope.launch {
            delay(RETRY_FEEDBACK_MS)
            verificationNotice = getString(R.string.verification_still_pending)
            // The user asked for this answer: show it wherever they are, as on the splash.
            resetRestoredFlow = true
            viewModel.recheckVerification()
        }
    }

    private fun resendVerification() {
        lifecycleScope.launch {
            delay(RETRY_FEEDBACK_MS)
            val sent = viewModel.requestVerificationEmail() == true
            verificationNotice = getString(if (sent) R.string.verification_email_sent else R.string.verification_email_failed)
            showVerificationSheet()
        }
    }

    private fun withErrorCode(message: String): String =
        viewModel.startupErrorCode?.let { message + "\n\n" + getString(R.string.error_code_line, it.label) }
            ?: message

    private fun retryStartupCheck() {
        lifecycleScope.launch {
            // showKamleonSheet ignores a sheet that is still on screen, and this one takes a moment
            // to close: a failure that comes back at once would leave no message at all. The pause
            // also makes the retry visible (the logo keeps playing) instead of a flicker.
            delay(RETRY_FEEDBACK_MS)
            // The user asked for this: try the server for real even if the monitor says there is no network.
            viewModel.checkLogin(trustMonitor = false)
        }
    }

    fun openMain() {
        if (viewModel.mainOpened) return
        viewModel.mainOpened = true
        // A scan link opens InitActivity on top of a running MainActivity: reuse it (onNewIntent)
        // instead of stacking another one for every link.
        val mainIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingScan = viewModel.consumePendingScan()
        if (pendingScan != null) {
            mainIntent.action = Constants.SCAN_ACTION
            mainIntent.putExtra(Constants.SCAN_UNIT_ID, pendingScan.first)
            mainIntent.putExtra(Constants.SCAN_SESSION_ID, pendingScan.second)
        } else if (viewModel.consumeInvalidScan()) {
            mainIntent.action = Constants.SCAN_ACTION
            mainIntent.putExtra(Constants.SCAN_INVALID, true)
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
        onKamleonSheetResult(STARTUP_ERROR_SHEET) {
            autoRetryJob?.cancel()
            when (it) {
                SheetResult.PRIMARY -> retryStartupCheck()
                // The account was checked when it was last online; the server is asked again next time.
                SheetResult.SECONDARY -> openMain()
                SheetResult.TERTIARY, SheetResult.DISMISSED -> Unit
            }
        }
        onKamleonSheetResult(STARTUP_VERIFY_SHEET) {
            when (it) {
                SheetResult.PRIMARY -> recheckVerification()
                SheetResult.SECONDARY -> resendVerification()
                SheetResult.TERTIARY -> viewModel.logOut(restartStartupCheck = true)
                SheetResult.DISMISSED -> Unit
            }
        }
        onKamleonSheetResult(SESSION_ENDED_SHEET) {
            if (it == SheetResult.PRIMARY) viewModel.logOut(restartStartupCheck = true)
        }
        onKamleonSheetResult(STARTUP_SERVER_ERROR_SHEET) {
            when (it) {
                SheetResult.PRIMARY -> retryStartupCheck()
                SheetResult.SECONDARY -> viewModel.logOut(restartStartupCheck = true)
                SheetResult.TERTIARY, SheetResult.DISMISSED -> Unit
            }
        }
    }

    override fun initEvent() {}

    private companion object {
        const val STARTUP_ERROR_SHEET = "startup_error"
        const val STARTUP_SERVER_ERROR_SHEET = "startup_server_error"
        const val STARTUP_VERIFY_SHEET = "startup_verify"
        const val SESSION_ENDED_SHEET = "startup_session_ended"
        const val RETRY_FEEDBACK_MS = 600L
    }
}
