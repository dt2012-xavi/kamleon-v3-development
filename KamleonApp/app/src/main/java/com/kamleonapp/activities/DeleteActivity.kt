package com.kamleonapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.kamleonapp.R
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityDeleteBinding
import com.kamleonapp.databinding.ActivityLoginBinding

class DeleteActivity : BaseActivity<ActivityDeleteBinding>() {
    override fun setBinding(): ActivityDeleteBinding = ActivityDeleteBinding.inflate(layoutInflater)

    override fun initView() {
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { finish() }
    }
}