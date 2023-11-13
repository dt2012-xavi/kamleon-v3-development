package com.dynatech2012.kamleonuserapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityDeleteBinding
import com.dynatech2012.kamleonuserapp.databinding.ActivityEmailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmailActivity : BaseActivity<ActivityEmailBinding>() {
    override fun setBinding(): ActivityEmailBinding = ActivityEmailBinding.inflate(layoutInflater)

    override fun initView() {
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { finish() }
    }
}