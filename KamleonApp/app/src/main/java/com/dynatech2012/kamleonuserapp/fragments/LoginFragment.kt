package com.dynatech2012.kamleonuserapp.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.InitActivity
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
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
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_ok, null)
        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text =
            getString(if (errorType == -7) R.string.signup_existing_account_title else R.string.login_alert_title_login_error)
        dialogView.findViewById<TextView>(R.id.tvDialogDesc).text = when (errorType) {
            -7 -> getString(R.string.signup_existing_account_message)
            -6 -> getString(R.string.startup_network_error)
            -4 -> getString(R.string.login_alert_description_not_valid_email)
            -3 -> getString(R.string.login_alert_description_user_not_found)
            -2 -> getString(R.string.login_alert_description_wrong_pass)
            -1 -> getString(R.string.login_alert_description_login_error_unknown)
            else -> ""
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.findViewById<TextView>(R.id.tvBtnOk).setOnClickListener { dialog.dismiss() }
    }

    private fun showVerificationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_email_verification, null)
        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_desc).text =
            getString(R.string.login_alert_description_not_verified_email)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_ok).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.tv_dialog_verify_resend).setOnClickListener {
            viewModel.sendVerificationEmail()
            dialog.dismiss()
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun showPolicyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_policy, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .show()
        dialogView.findViewById<TextView>(R.id.tv_dialog_policy_desc).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, UrlConstants.URL_POLICY_APP.toUri()))
        }
        dialogView.findViewById<TextView>(R.id.tv_dialog_policy_disagree).setOnClickListener {
            ActivityCompat.finishAffinity(requireActivity())
        }
        dialogView.findViewById<TextView>(R.id.tv_dialog_policy_agree).setOnClickListener {
            viewModel.acceptPolicy()
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }
}
