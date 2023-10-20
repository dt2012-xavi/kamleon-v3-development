package com.kamleonapp.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.kamleonapp.R
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityLoginBinding
import com.kamleonapp.databinding.ActivityRegisterBinding

class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {
    override fun setBinding(): ActivityRegisterBinding = ActivityRegisterBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initView() {
        binding.btnCreateAccount.isEnabled = false
        binding.inputBoxFName.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxLName.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
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

        binding.inputBoxFName.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxLName.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxEmail.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxPwd.getEditTextView()?.addTextChangedListener(inputWatcher)
    }

    override fun initEvent() {
        binding.btnNavBack.setOnClickListener { finish() }
        binding.tvToLogin.setOnClickListener {
            val to = Intent(this, LoginActivity::class.java)
            to.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(to)
        }
    }

    private fun updateButtonState() {
        val hasValidInput = binding.inputBoxFName.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxLName.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxPwd.getEditTextView()?.text.toString().isNotBlank()
        binding.btnCreateAccount.isEnabled = hasValidInput
    }
}