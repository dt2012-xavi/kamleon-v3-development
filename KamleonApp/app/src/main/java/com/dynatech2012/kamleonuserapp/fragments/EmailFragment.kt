package com.dynatech2012.kamleonuserapp.fragments

import androidx.fragment.app.activityViewModels
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityEmailBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmailFragment : BaseFragment<ActivityEmailBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityEmailBinding = ActivityEmailBinding.inflate(layoutInflater)

    override fun initView() {
        initObservers()
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { activity?.finish() }
        binding.btnSave.setOnClickListener {
            // TODO: old pass needed to reauthenticate
            val currentPwd = ""
            viewModel.changeEmail(currentPwd, binding.etEmail.text.toString())
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