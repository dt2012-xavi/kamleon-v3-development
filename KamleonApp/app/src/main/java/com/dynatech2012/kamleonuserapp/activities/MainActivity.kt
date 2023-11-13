package com.dynatech2012.kamleonuserapp.activities

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityInitBinding
import com.dynatech2012.kamleonuserapp.databinding.ActivityLoginBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityInitBinding>() {
    private val viewModel: AuthViewModel by viewModels()
    override fun setBinding(): ActivityInitBinding = ActivityInitBinding.inflate(layoutInflater)

    override fun initView() {

    }

    override fun initEvent() {

    }
}