package com.dynatech2012.kamleonuserapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityDeleteBinding
import com.dynatech2012.kamleonuserapp.databinding.ActivityLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteActivity : BaseActivity<ActivityDeleteBinding>() {
    override fun setBinding(): ActivityDeleteBinding = ActivityDeleteBinding.inflate(layoutInflater)

    override fun initView() {
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { finish() }
    }
}