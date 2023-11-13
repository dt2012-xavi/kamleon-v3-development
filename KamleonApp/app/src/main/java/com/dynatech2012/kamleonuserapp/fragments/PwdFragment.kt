package com.dynatech2012.kamleonuserapp.fragments

import android.text.InputType
import androidx.fragment.app.activityViewModels
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityPwdBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PwdFragment : BaseFragment<ActivityPwdBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityPwdBinding = ActivityPwdBinding.inflate(layoutInflater)
    private var secureInput: Boolean = true
    override fun initView() {
        updateInputMode()
        initObservers()
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { activity?.finish() }
        binding.imageViewEye.isClickable = true
        binding.imageViewEye.setOnClickListener {
            secureInput = !secureInput
            updateInputMode()
        }
        binding.btnSave.setOnClickListener {
            // TODO: old pass needed to reauthenticate
            val oldPwd = ""
            viewModel.changePwd(oldPwd, binding.etPwd.text.toString())
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

    private fun initObservers() {
        viewModel.userUpdated.observe(this, this::startActivity)
    }

    private fun startActivity(updated: Boolean) {
        if (updated) {
            viewModel.resetUserUpdated()
        }
    }
}