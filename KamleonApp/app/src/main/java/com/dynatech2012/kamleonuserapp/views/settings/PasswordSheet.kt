package com.dynatech2012.kamleonuserapp.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.fragments.PasswordSheetContent
import com.dynatech2012.kamleonuserapp.network.actionFailureText
import com.dynatech2012.kamleonuserapp.network.isConnectionProblem
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.security.PasswordPolicyException
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import com.dynatech2012.kamleonuserapp.security.messageRes
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Settings sheet to change the password. It runs the change itself and waits for the answer:
 * on success it closes and tells the screen through [ProfileFieldSheet.RESULT_SAVED]; a wrong
 * current password or a rejected new one shows under its field; anything else shows the error and
 * the sheet stays open. "Forgot password?" asks first, then emails a reset link and closes the sheet.
 */
class PasswordSheet : BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()

    // Passwords are deliberately kept out of saved instance state.
    private var currentPassword by mutableStateOf("")
    private var newPassword by mutableStateOf("")
    private var confirmPassword by mutableStateOf("")
    // Verdicts reported back by the ViewModel, shown next to their field until it changes.
    private var currentError by mutableStateOf<Int?>(null)
    private var newError by mutableStateOf<Int?>(null)
    private var busy by mutableStateOf(false)
    /** The busy work is the reset email, shown by the link rather than by Save. */
    private var sendingReset by mutableStateOf(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
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
                    PasswordSheetContent(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                        currentError = currentError,
                        newError = newError,
                        checking = busy && !sendingReset,
                        sendingReset = sendingReset,
                        passwordStrength = strength,
                        onCurrentPasswordChange = { currentPassword = it; currentError = null },
                        onNewPasswordChange = { newPassword = it; newError = null },
                        onConfirmPasswordChange = { confirmPassword = it },
                        onSave = ::save,
                        onForgotPassword = ::confirmReset
                    )
                }
            }
        }

    override fun onStart() {
        super.onStart()
        showExpanded(draggable = true)
    }

    private fun save() = run(R.string.dialog_reset_pwd_success_description) {
        val response = viewModel.changePassword(currentPassword, newPassword)
        when (response) {
            is Response.Success -> true
            is Response.Failure -> {
                when (val e = response.exception) {
                    // Shown next to the field that needs fixing.
                    is PasswordPolicyException -> newError = e.reason.messageRes()
                    is FirebaseAuthInvalidCredentialsException -> currentError = R.string.login_alert_description_wrong_pass
                    else -> showError(connectionOr(e, response.errorMessage ?: getString(R.string.pwd_error_generic)))
                }
                false
            }
            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onKamleonSheetResult(SHEET_RESET) { if (it == SheetResult.PRIMARY) sendReset() }
    }

    /** "Forgot password?": says where the link goes and sends it only once confirmed. */
    private fun confirmReset() {
        if (busy) return
        val email = viewModel.accountEmail ?: return
        // The same words as the login's "Trouble logging in?" screen.
        childFragmentManager.showKamleonSheet(
            SHEET_RESET,
            SheetSpec(
                title = getString(R.string.forgotpwd_confirm_title),
                message = getString(R.string.forgotpwd_confirm_desc, email),
                primary = getString(R.string.forgotpwd_send),
                secondary = getString(R.string.dialog_logout_n)
            )
        )
    }

    private fun sendReset() = run(R.string.forgotpwd_sent) {
        sendingReset = true
        val result = viewModel.sendPasswordReset()
        sendingReset = false
        if (result == null) return@run false
        if (!result.success) showError(connectionOr(result.failure, getString(R.string.forgotpwd_send_error)))
        result.success
    }

    /**
     * Runs [action] once at a time with the sheet locked; when it reports success the sheet closes
     * and Settings confirms with [message].
     */
    private fun run(message: Int, action: suspend () -> Boolean) {
        if (busy) return
        busy = true
        // Leaving mid-change would hide whether it worked.
        isCancelable = false
        viewLifecycleOwner.lifecycleScope.launch {
            val done = action()
            busy = false
            isCancelable = true
            if (done) {
                parentFragmentManager.setFragmentResult(
                    ProfileFieldSheet.RESULT_SAVED,
                    Bundle().apply { putInt(ProfileFieldSheet.RESULT_MESSAGE, message) }
                )
                dismissAllowingStateLoss()
            }
        }
    }

    /** A connection problem is told the same on every screen; otherwise this sheet's own [message]. */
    private fun connectionOr(failure: Exception?, message: String): String =
        if (failure?.isConnectionProblem() == true) getString(actionFailureText(failure).second) else message

    private fun showError(message: String) {
        childFragmentManager.showKamleonSheet(
            "pwd_error",
            SheetSpec(
                title = getString(R.string.pwd_error_title),
                message = message,
                primary = getString(R.string.dialog_ok)
            )
        )
    }

    private companion object {
        const val STRENGTH_DEBOUNCE_MS = 150L
        const val SHEET_RESET = "pwd_reset_confirm"
    }
}
