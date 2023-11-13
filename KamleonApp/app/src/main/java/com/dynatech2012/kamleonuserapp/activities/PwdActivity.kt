package com.dynatech2012.kamleonuserapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityDeleteBinding
import com.dynatech2012.kamleonuserapp.databinding.ActivityPwdBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PwdActivity : BaseActivity<ActivityPwdBinding>() {
    override fun setBinding(): ActivityPwdBinding = ActivityPwdBinding.inflate(layoutInflater)
    private var secureInput: Boolean = true
    override fun initView() {
        updateInputMode()
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { finish() }
        binding.imageViewEye.isClickable = true
        binding.imageViewEye.setOnClickListener {
            secureInput = !secureInput
            updateInputMode()
        }
    }

    private fun updateInputMode() {
        binding.imageViewEye.setImageResource(if (secureInput) R.drawable.icn_eye_on else R.drawable.icn_eye_off)
        if (secureInput) {
            binding.etPwd.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            binding.etPwd.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }
}