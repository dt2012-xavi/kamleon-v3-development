package com.dynatech2012.kamleonuserapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityDeleteBinding
import com.dynatech2012.kamleonuserapp.databinding.ActivityPinBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PinActivity : BaseActivity<ActivityPinBinding>() {
    override fun setBinding(): ActivityPinBinding = ActivityPinBinding.inflate(layoutInflater)
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
            binding.etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            binding.etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }
}