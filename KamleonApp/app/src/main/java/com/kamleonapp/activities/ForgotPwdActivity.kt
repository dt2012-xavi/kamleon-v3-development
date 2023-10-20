package com.kamleonapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.kamleonapp.R
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityForgotPwdBinding
import com.kamleonapp.databinding.ActivityOnboardingBinding

class ForgotPwdActivity : BaseActivity<ActivityForgotPwdBinding>() {
    override fun setBinding(): ActivityForgotPwdBinding = ActivityForgotPwdBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnSendLoginLink.isEnabled = false

        binding.inputBoxEmail.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.inputBoxEmail.getEditTextView()?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                binding.btnSendLoginLink.isEnabled = binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
            }
        })
    }

    override fun initEvent() {
        binding.btnClose.setOnClickListener {
            finish()
        }
    }
}