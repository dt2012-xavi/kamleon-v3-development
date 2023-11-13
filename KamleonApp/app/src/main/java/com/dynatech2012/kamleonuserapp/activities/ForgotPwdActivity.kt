package com.dynatech2012.kamleonuserapp.activities

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityForgotPwdBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

        binding.tvCreateNewAcc.setOnClickListener {
            val to = Intent(this, RegisterActivity::class.java)
            to.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(to)
            finish()
        }
    }
}