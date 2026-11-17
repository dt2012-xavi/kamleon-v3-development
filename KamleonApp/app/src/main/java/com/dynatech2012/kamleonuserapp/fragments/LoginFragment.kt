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
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.InitActivity
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
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

            AppTheme(useDarkTheme = false) {
                LoginScreen(
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
        onKamleonSheetResult("login_policy") {
            when (it) {
                SheetResult.PRIMARY -> viewModel.acceptPolicy()
                SheetResult.SECONDARY -> ActivityCompat.finishAffinity(requireActivity())
                SheetResult.DISMISSED -> Unit
            }
        }
        viewModel.uiState.observe(viewLifecycleOwner, this::handleState)
    }

    private fun handleState(state: Int) {
        when (state) {
            in Int.MIN_VALUE..-1 -> {
                showReadyDialog(state)
                if (state == -7) viewModel.resetUiState()
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
        if (errorType == -5) {
            showVerificationDialog()
            return
        }
        childFragmentManager.showKamleonSheet(
            "login_error",
            SheetSpec(
                title = getString(
                    if (errorType == -7) R.string.signup_existing_account_title else R.string.login_alert_title_login_error
                ),
                message = when (errorType) {
                    -7 -> getString(R.string.signup_existing_account_message)
                    -6 -> getString(R.string.startup_network_error)
                    -4 -> getString(R.string.login_alert_description_not_valid_email)
                    -3 -> getString(R.string.login_alert_description_user_not_found)
                    -2 -> getString(R.string.login_alert_description_wrong_pass)
                    -1 -> getString(R.string.login_alert_description_login_error_unknown)
                    else -> ""
                },
                primary = getString(R.string.dialog_ok)
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
}
