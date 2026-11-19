package com.dynatech2012.kamleonuserapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.InitActivity
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.LoginState
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Login form displayed")
        (view as ComposeView).setContent {
            var email by rememberSaveable { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            val state by viewModel.uiState.observeAsState(0)
            // Observed so a link that arrives while Login is open updates the text.
            val scanSavedAt by viewModel.pendingScanSavedAt.observeAsState()

            AppTheme(useDarkTheme = false) {
                LoginScreen(
                    pendingScan = scanSavedAt != null && viewModel.hasPendingScan,
                    email = email,
                    password = password,
                    isLoading = state == 1 || state == 6,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onSignIn = { viewModel.login(email, password) },
                    onForgotPassword = {
                        findNavController().navigate(R.id.action_loginFragment_to_forgotPwdFragment)
                    },
                    onRegister = {
                        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                    }
                )
            }
        }
        onKamleonSheetResult("login_verify") { if (it == SheetResult.SECONDARY) viewModel.sendVerificationEmail() }
        // Signed in, but the profile is refused: staying signed in would bring the user back here.
        onKamleonSheetResult("login_account_rejected") { if (it == SheetResult.PRIMARY) viewModel.logOut() }
        onKamleonSheetResult("login_profile_retry") {
            when (it) {
                SheetResult.PRIMARY -> retryAfterSheetCloses { viewModel.retryProfileAfterLogin() }
                SheetResult.SECONDARY -> viewModel.logOut()
                SheetResult.TERTIARY, SheetResult.DISMISSED -> Unit
            }
        }
        onKamleonSheetResult("login_policy_retry") {
            when (it) {
                SheetResult.PRIMARY -> retryAfterSheetCloses { viewModel.acceptPolicy() }
                SheetResult.SECONDARY -> viewModel.logOut()
                SheetResult.TERTIARY, SheetResult.DISMISSED -> Unit
            }
        }
        onKamleonSheetResult("login_policy") {
            when (it) {
                SheetResult.PRIMARY -> viewModel.acceptPolicy()
                // Refusing means this account cannot be used: leave it clean instead of a button that closes the app.
                SheetResult.SECONDARY -> viewModel.logOut()
                SheetResult.TERTIARY, SheetResult.DISMISSED -> Unit
            }
        }
        viewModel.uiState.observe(viewLifecycleOwner, this::handleState)
    }

    /**
     * showKamleonSheet ignores a sheet whose tag is still on screen, and a sheet takes a moment to
     * close: a retry that fails at once would come back with no message at all. The pause also makes
     * the retry visible instead of a flicker.
     */
    private fun retryAfterSheetCloses(retry: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(RETRY_FEEDBACK_MS)
            retry()
        }
    }

    private fun handleState(state: Int) {
        when (state) {
            in Int.MIN_VALUE..-1 -> {
                showReadyDialog(state)
                // These sheets lead somewhere on their own: the state must not replay them when the
                // view is recreated.
                if (state in STATES_SHOWN_ONCE) viewModel.resetUiState()
            }
            3 -> Snackbar.make(requireView(), R.string.verification_email_sent, Snackbar.LENGTH_SHORT).show()
            5 -> Timber.d("Registration step completed; login available")
            6 -> showPolicyDialog()
            7 -> (requireActivity() as InitActivity).openMain()
            8 -> {
                viewModel.resetUiState()
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                }
            }
        }
    }

    private fun showReadyDialog(errorType: Int) {
        when (errorType) {
            LoginState.EMAIL_NOT_VERIFIED -> showVerificationDialog()
            LoginState.ACCOUNT_REJECTED -> showAccountRejectedDialog()
            LoginState.PROFILE_RETRY -> showProfileRetryDialog()
            LoginState.POLICY_RETRY -> showPolicyRetryDialog()
            else -> showLoginErrorDialog(errorType)
        }
    }

    /** A failed sign-in: no session exists, so the user only needs to know what went wrong. */
    private fun showLoginErrorDialog(errorType: Int) {
        val (title, message) = when (errorType) {
            LoginState.ACCOUNT_EXISTS ->
                R.string.signup_existing_account_title to R.string.signup_existing_account_message
            LoginState.NETWORK -> R.string.login_error_title_network to R.string.startup_error_message
            LoginState.TOO_MANY_ATTEMPTS ->
                R.string.login_error_title_too_many to R.string.login_error_message_too_many
            LoginState.ACCOUNT_DISABLED ->
                R.string.login_error_title_disabled to R.string.login_error_message_disabled
            LoginState.INVALID_EMAIL ->
                R.string.login_alert_title_login_error to R.string.login_alert_description_not_valid_email
            LoginState.USER_NOT_FOUND ->
                R.string.login_error_title_generic to R.string.login_alert_description_user_not_found
            LoginState.WRONG_PASSWORD ->
                R.string.login_error_title_generic to R.string.login_alert_description_wrong_pass
            else -> R.string.login_error_title_generic to R.string.login_alert_description_login_error_unknown
        }
        val text = getString(message)
        childFragmentManager.showKamleonSheet(
            "login_error",
            SheetSpec(
                title = getString(title),
                message = if (errorType == LoginState.ACCOUNT_DISABLED) withErrorCode(text) else text,
                primary = getString(R.string.dialog_ok)
            )
        )
    }

    private fun withErrorCode(message: String): String =
        viewModel.loginErrorCode?.let { message + "\n\n" + getString(R.string.error_code_line, it.label) }
            ?: message

    /** Signed in, but the server refuses the profile and retrying cannot help: leave cleanly. */
    private fun showAccountRejectedDialog() {
        childFragmentManager.showKamleonSheet(
            "login_account_rejected",
            SheetSpec(
                title = getString(R.string.login_account_rejected_title),
                message = withErrorCode(getString(R.string.login_account_rejected_message)),
                primary = getString(R.string.dialog_ok),
                dismissible = false
            )
        )
    }

    /** Signed in a moment ago, so there is network: the profile may load on a second try, or the user can leave. */
    private fun showProfileRetryDialog() {
        childFragmentManager.showKamleonSheet(
            "login_profile_retry",
            SheetSpec(
                title = getString(R.string.startup_error_title),
                message = getString(R.string.startup_error_message),
                primary = getString(R.string.retry),
                secondary = getString(R.string.dialog_logout_y),
                dismissible = false
            )
        )
    }

    /** The answer could not be saved: try again, or log out. There is no way to continue without it. */
    private fun showPolicyRetryDialog() {
        childFragmentManager.showKamleonSheet(
            "login_policy_retry",
            SheetSpec(
                title = getString(R.string.policy_retry_title),
                message = getString(R.string.policy_retry_message),
                primary = getString(R.string.retry),
                secondary = getString(R.string.dialog_logout_y),
                dismissible = false
            )
        )
    }

    private fun showVerificationDialog() {
        childFragmentManager.showKamleonSheet(
            "login_verify",
            SheetSpec(
                title = getString(R.string.dialog_verify_title),
                message = getString(R.string.login_alert_description_not_verified_email),
                primary = getString(R.string.dialog_ok),
                secondary = getText(R.string.dialog_verify_resend),
                dismissible = false
            )
        )
    }

    private fun showPolicyDialog() {
        childFragmentManager.showKamleonSheet(
            "login_policy",
            SheetSpec(
                title = getString(R.string.dialog_policy_title),
                message = getText(R.string.dialog_policy_desc),
                primary = getString(R.string.dialog_policy_agree),
                secondary = getText(R.string.dialog_policy_disagree),
                dismissible = false,
                linkUrl = UrlConstants.URL_POLICY_APP
            )
        )
    }

    private companion object {
        const val RETRY_FEEDBACK_MS = 600L
        val STATES_SHOWN_ONCE = setOf(
            LoginState.ACCOUNT_EXISTS,
            LoginState.ACCOUNT_DISABLED,
            LoginState.TOO_MANY_ATTEMPTS,
            LoginState.ACCOUNT_REJECTED,
            LoginState.PROFILE_RETRY,
            LoginState.POLICY_RETRY
        )
    }
}
