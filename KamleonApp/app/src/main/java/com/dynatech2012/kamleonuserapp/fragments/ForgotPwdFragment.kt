package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.viewmodels.ForgotPwdViewModel
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
            if (it) showSuccessDialog() else showErrorDialog()
        }
    }

    private fun showSuccessDialog() {
        showReadyDialog(
            getString(R.string.dialog_reset_pwd_success_title),
            getString(R.string.dialog_reset_pwd_success_description)
        )
    }

    private fun showErrorDialog() {
        showReadyDialog(
            getString(R.string.dialog_reset_pwd_error_title),
            getString(R.string.dialog_reset_pwd_error_description)
        )
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
