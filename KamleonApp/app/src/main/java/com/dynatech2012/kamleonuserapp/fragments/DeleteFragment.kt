package com.dynatech2012.kamleonuserapp.fragments

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityDeleteBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteFragment : BaseFragment<ActivityDeleteBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityDeleteBinding = ActivityDeleteBinding.inflate(layoutInflater)

    override fun initView() {
        initObservers()
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { findNavController().popBackStack() }
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