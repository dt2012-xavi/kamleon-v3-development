package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.InitActivity
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.databinding.ActivityLoginBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class LoginFragment : BaseFragment<ActivityLoginBinding>() {
    private val viewModel: AuthViewModel by activityViewModels()
    override fun setBinding(): ActivityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)

    override fun initView() {
        Timber.d("Login form displayed")

        binding.btnSignIn.isEnabled = true

        binding.inputBoxEmail.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxPwd.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_DONE

        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updateButtonState()
            }
        }
        binding.inputBoxEmail.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxPwd.getEditTextView()?.addTextChangedListener(inputWatcher)

        initObservers()
    }

    override fun initEvent() {
        binding.btnSignIn.setOnClickListener {
            viewModel.login(binding.inputBoxEmail.getEditTextView()?.text.toString(),
                binding.inputBoxPwd.getEditTextView()?.text.toString())
        }

        binding.btnToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        binding.tvForgotPwd.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPwdFragment)
        }
    }

    private fun initObservers() {
        viewModel.uiState.observe(viewLifecycleOwner, this::handleState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.inputBoxEmail.getEditTextView()?.text?.clear()
        binding.inputBoxPwd.getEditTextView()?.text?.clear()
    }

    private fun handleState(state: Int) {
        when (state) {
            in Int.MIN_VALUE..-1 -> {
                showReadyDialog(state)
                showIdle()
                if (state == -7) viewModel.resetUiState()
            }
            0, 5 -> showIdle()
            1 -> showLoading()
            3 -> Snackbar.make(requireView(), R.string.verification_email_sent, Snackbar.LENGTH_SHORT).show()
            6 -> {
                showLoading()
                showPolicyDialog()
            }
            7 -> {
                showIdle()
                (requireActivity() as InitActivity).openMain()
            }
            8 -> {
                showIdle()
                viewModel.resetUiState()
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                }
            }
        }
    }

    private fun showIdle() {
        binding.btnSignIn.isEnabled = true
        binding.btnSignIn.text = getString(R.string.login_btn_signin)
        binding.pbLogin.visibility = INVISIBLE
    }

    private fun showLoading() {
        binding.btnSignIn.isEnabled = false
        binding.btnSignIn.text = ""
        binding.pbLogin.visibility = VISIBLE
    }

    private fun updateButtonState() {
        val hasValidInput = binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxPwd.getEditTextView()?.text.toString().isNotBlank()
        binding.btnSignIn.isEnabled = hasValidInput
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
            openWebBrowser(UrlConstants.URL_POLICY_APP)
        }
        dialogView.findViewById<TextView>(R.id.tv_dialog_policy_disagree).setOnClickListener {
            ActivityCompat.finishAffinity(requireActivity())
        }
        dialogView.findViewById<TextView>(R.id.tv_dialog_policy_agree).setOnClickListener {
            viewModel.acceptPolicy()
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}
