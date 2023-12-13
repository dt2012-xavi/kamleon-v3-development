package com.dynatech2012.kamleonuserapp.fragments

import android.content.Intent
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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.MainActivity
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityLoginBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment<ActivityLoginBinding>() {
    private val viewModel: AuthViewModel by activityViewModels()
    override fun setBinding(): ActivityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)

    override fun initView() {
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
        viewModel.uiState.observe(this, this::startActivity)
    }

    private fun startActivity(state: Int) {
        when (state) {
            -3 -> { // email not valid
                showReadyDialog(state)
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = getString(R.string.login_btn_signin)
                binding.pbLogin.visibility = INVISIBLE
            }
            -2 -> { // password not valid
                showReadyDialog(state)
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = getString(R.string.login_btn_signin)
                binding.pbLogin.visibility = INVISIBLE
            }
            -1 -> { // firebase error
                showReadyDialog(state)
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = getString(R.string.login_btn_signin)
                binding.pbLogin.visibility = INVISIBLE
            }
            0 -> {
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = getString(R.string.login_btn_signin)
                binding.pbLogin.visibility = INVISIBLE
            }
            1 -> {
                binding.btnSignIn.isEnabled = false
                binding.btnSignIn.text = ""
                binding.pbLogin.visibility = VISIBLE
            }
            5 -> {
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = getString(R.string.login_btn_signin)
                binding.pbLogin.visibility = INVISIBLE
                startActivity(Intent(requireContext(), MainActivity::class.java))
            }
        }
    }

    private fun updateButtonState() {
        val hasValidInput = binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxPwd.getEditTextView()?.text.toString().isNotBlank()
        binding.btnSignIn.isEnabled = hasValidInput
    }

    private fun showReadyDialog(errorType: Int) {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_dialog_ok, null)

        dialog.setView(dialogView)
        dialog.setCancelable(false)
        // -3 is email not valid, -2 is password not valid, -1 is firebase error
        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = getString(R.string.login_alert_title_login_error)
        dialogView.findViewById<TextView>(R.id.tvDialogDesc).text = when (errorType) {
            -3 -> getString(R.string.login_alert_description_not_valid_email)
            -2 -> getString(R.string.login_alert_description_short_pass)
            -1 -> getString(R.string.login_alert_description_login_error)
            else -> ""
        }
        val logoutDialog = dialog.show()
        logoutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<TextView>(R.id.tvBtnOk).setOnClickListener {
            logoutDialog.dismiss()
        }
    }
}