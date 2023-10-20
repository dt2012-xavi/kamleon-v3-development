package com.kamleonapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kamleonapp.R
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityDeleteBinding
import com.kamleonapp.databinding.ActivityEmailBinding

class EmailActivity : BaseActivity<ActivityEmailBinding>() {
    override fun setBinding(): ActivityEmailBinding = ActivityEmailBinding.inflate(layoutInflater)

    override fun initView() {
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { finish() }
    }
}