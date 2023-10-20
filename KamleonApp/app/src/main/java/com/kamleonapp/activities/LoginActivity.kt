package com.kamleonapp.activities

import android.content.Intent
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityLoginBinding

class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    override fun setBinding(): ActivityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnSignIn.isEnabled = true

        binding.inputBoxEmail.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxPwd.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_DONE

        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                updateButtonState()
            }
        }
        binding.inputBoxEmail.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxPwd.getEditTextView()?.addTextChangedListener(inputWatcher)
    }

    override fun initEvent() {
        binding.btnSignIn.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        binding.btnToSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPwd.setOnClickListener {
            startActivity(Intent(this, ForgotPwdActivity::class.java))
        }
    }

    private fun updateButtonState() {
        val hasValidInput = binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxPwd.getEditTextView()?.text.toString().isNotBlank()
        binding.btnSignIn.isEnabled = hasValidInput
    }
}