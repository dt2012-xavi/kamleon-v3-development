package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.security.PasswordPolicyException
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import com.dynatech2012.kamleonuserapp.security.messageRes
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class PwdFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()

    private companion object {
        const val STRENGTH_DEBOUNCE_MS = 150L
    }

    // Passwords are deliberately kept out of saved instance state.
    private var currentPassword by mutableStateOf("")
    private var newPassword by mutableStateOf("")
    private var confirmPassword by mutableStateOf("")
    // Verdicts reported back by the ViewModel, shown next to their field until it changes.
    private var currentError by mutableStateOf<Int?>(null)
    private var newError by mutableStateOf<Int?>(null)
    // Bumped after a successful change so the screen forgets it was already submitted.
    private var formKey by mutableIntStateOf(0)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            val checking by viewModel.pwdChanging.observeAsState(false)
            // Live strength feedback: debounced so it is not recomputed on every keystroke.
            var strength by remember { mutableStateOf<PasswordStrength?>(null) }
            LaunchedEffect(newPassword) {
                if (newPassword.isEmpty()) {
                    strength = null
                } else {
                    delay(STRENGTH_DEBOUNCE_MS)
                    strength = viewModel.passwordStrength(newPassword)
                }
            }
            AppTheme(useDarkTheme = false) {
                PwdScreen(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    currentError = currentError,
                    newError = newError,
                    checking = checking,
                    passwordStrength = strength,
                    onCurrentPasswordChange = { currentPassword = it; currentError = null },
                    onNewPasswordChange = { newPassword = it; newError = null },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onSave = { viewModel.changePwd(currentPassword, newPassword) },
                    onForgotPassword = { viewModel.resetPwd() },
                    onBack = { findNavController().popBackStack() },
                    formKey = formKey
                )
            }
        }

        viewModel.userUpdated.observe(viewLifecycleOwner, this::onPwdUpdated)
        viewModel.resetPwdSuccess.observe(viewLifecycleOwner) {
            if (it) showSuccessDialog()
            else showErrorDialog(getString(R.string.pwd_error_generic))
        }
    }

    private fun onPwdUpdated(response: Response<Unit>?) {
        if (response == null) return
        when (response) {
            is Response.Success -> {
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
                formKey++
                showSuccessDialog()
            }

            is Response.Failure -> when (val e = response.exception) {
                // Shown next to the field that needs fixing.
                is PasswordPolicyException -> newError = e.reason.messageRes()
                is FirebaseAuthInvalidCredentialsException -> currentError = R.string.login_alert_description_wrong_pass
                else -> showErrorDialog(response.errorMessage ?: getString(R.string.pwd_error_generic))
            }

            else -> {}
        }
        viewModel.resetUserUpdated()
    }

    private fun showSuccessDialog() {
        showReadyDialog(
            getString(R.string.dialog_reset_pwd_success_title),
            getString(R.string.dialog_reset_pwd_success_description)
        )
    }

    private fun showErrorDialog(message: String) {
        showReadyDialog(getString(R.string.pwd_error_title), message)
    }

    private fun showReadyDialog(title: String, message: String) {
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_ok, null)
        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = title
        dialogView.findViewById<TextView>(R.id.tvDialogDesc).text = message
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .show()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialogView.findViewById<TextView>(R.id.tvBtnOk).setOnClickListener { dialog.dismiss() }
    }
}
