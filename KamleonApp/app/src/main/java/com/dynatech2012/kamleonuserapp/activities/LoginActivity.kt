package com.dynatech2012.kamleonuserapp.activities

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityLoginBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    private val viewModel: AuthViewModel by viewModels()
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
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPwd.setOnClickListener {
            startActivity(Intent(this, ForgotPwdActivity::class.java))
        }
    }

    private fun initObservers() {
        viewModel.uiState.observe(this, this::startActivity)
    }

    private fun startActivity(state: Int) {
        if (state == 3)
            startActivity(Intent(this, InitActivity::class.java))
    }

    private fun updateButtonState() {
        val hasValidInput = binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxPwd.getEditTextView()?.text.toString().isNotBlank()
        binding.btnSignIn.isEnabled = hasValidInput
    }
}