package com.dynatech2012.kamleonuserapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.network.actionFailureText
import com.dynatech2012.kamleonuserapp.network.isConnectionProblem
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.viewmodels.ForgotPwdViewModel
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPwdFragment : Fragment() {
    private val viewModel: ForgotPwdViewModel by viewModels()

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
            var email by rememberSaveable { mutableStateOf("") }
            AppTheme(useDarkTheme = false) {
                ForgotPwdScreen(
                    email = email,
                    onEmailChange = { email = it },
                    onSend = { viewModel.resetPwd(email) },
                    onClose = {
                        findNavController().navigate(R.id.action_forgotPwdFragment_to_loginFragment)
                    },
                    onCreateAccount = {
                        findNavController().navigate(R.id.action_forgotPwdFragment_to_registerFragment)
                    }
                )
            }
        }
        viewModel.resetPwdSuccess.observe(viewLifecycleOwner) {
            if (it.success) showSuccessDialog() else showErrorDialog(it.failure)
        }
    }

    private fun showSuccessDialog() {
        Snackbar.make(requireView(), R.string.dialog_reset_pwd_success_description, Snackbar.LENGTH_SHORT).show()
    }

    private fun showErrorDialog(failure: Exception?) {
        // A connection problem is told the same everywhere; anything else keeps this screen's own text.
        val (title, message) = if (failure?.isConnectionProblem() == true) actionFailureText(failure)
        else R.string.dialog_reset_pwd_error_title to R.string.dialog_reset_pwd_error_description
        childFragmentManager.showKamleonSheet(
            "forgot_error",
            SheetSpec(
                title = getString(title),
                message = getString(message),
                primary = getString(R.string.dialog_ok)
            )
        )
    }
}
