package com.dynatech2012.kamleonuserapp.fragments

import android.text.InputType
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityPwdBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PwdFragment : BaseFragment<ActivityPwdBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityPwdBinding = ActivityPwdBinding.inflate(layoutInflater)
    private var secureInputCurrent: Boolean = true
    private var secureInputNew: Boolean = true
    private var secureInputConfirm: Boolean = true
    override fun initView() {
        updateInputMode()
        initObservers()
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { findNavController().popBackStack() }
        binding.imageViewEye.isClickable = true
        binding.imageViewEye.setOnClickListener {
            secureInputCurrent = !secureInputCurrent
            updateInputMode()
        }
        binding.ivPwdNewEye.isClickable = true
        binding.ivPwdNewEye.setOnClickListener {
            secureInputNew = !secureInputNew
            updateInputMode()
        }
        binding.ivPwdConfirmEye.isClickable = true
        binding.ivPwdConfirmEye.setOnClickListener {
            secureInputConfirm = !secureInputConfirm
            updateInputMode()
        }
        binding.btnSave.setOnClickListener {
            // TODO: old pass needed to reauthenticate
            val oldPwd = ""
            viewModel.changePwd(oldPwd, binding.etPwd.text.toString())
        }
    }

    private fun updateInputMode() {
        binding.imageViewEye.setImageResource(if (secureInputCurrent) R.drawable.icn_eye_off else R.drawable.icn_eye_on)
        if (secureInputCurrent) {
            binding.etPwd.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            binding.etPwd.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.ivPwdNewEye.setImageResource(if (secureInputNew) R.drawable.icn_eye_off else R.drawable.icn_eye_on)
        if (secureInputNew) {
            binding.etPwdNew.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            binding.etPwdNew.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.ivPwdConfirmEye.setImageResource(if (secureInputConfirm) R.drawable.icn_eye_off else R.drawable.icn_eye_on)
        if (secureInputConfirm) {
            binding.etPwdConf.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            binding.etPwdConf.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
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