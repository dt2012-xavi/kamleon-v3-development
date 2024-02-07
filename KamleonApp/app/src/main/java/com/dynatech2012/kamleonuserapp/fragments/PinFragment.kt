package com.dynatech2012.kamleonuserapp.fragments

import android.text.InputType
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityPinBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PinFragment : BaseFragment<ActivityPinBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityPinBinding = ActivityPinBinding.inflate(layoutInflater)
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
        binding.ivPinNewEye.isClickable = true
        binding.ivPinNewEye.setOnClickListener {
            secureInputNew = !secureInputNew
            updateInputMode()
        }
        binding.ivPinConfirmEye.isClickable = true
        binding.ivPinConfirmEye.setOnClickListener {
            secureInputConfirm = !secureInputConfirm
            updateInputMode()
        }
        binding.btnSave.setOnClickListener {
            viewModel.changePin(binding.etPin.text.toString(), binding.etPin.text.toString())
        }
    }

    private fun updateInputMode() {
        binding.imageViewEye.setImageResource(if (secureInputCurrent) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputCurrent) {
            binding.etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            binding.etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.ivPinNewEye.setImageResource(if (secureInputNew) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputNew) {
            binding.etPinNew.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            binding.etPinNew.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.ivPinConfirmEye.setImageResource(if (secureInputConfirm) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputConfirm) {
            binding.etPinConf.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            binding.etPinConf.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
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