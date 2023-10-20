package com.kamleonapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import com.kamleonapp.R
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityDeleteBinding
import com.kamleonapp.databinding.ActivityPinBinding

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