package com.dynatech2012.kamleonuserapp.fragments

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.constants.UrlConstants.URL_DEACTIVATE
import com.dynatech2012.kamleonuserapp.constants.UrlConstants.URL_DELETE
import com.dynatech2012.kamleonuserapp.databinding.ActivityDeleteBinding
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteFragment : BaseFragment<ActivityDeleteBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityDeleteBinding = ActivityDeleteBinding.inflate(layoutInflater)

    override fun initView() {
        initListeners()
        initObservers()
    }

    private fun initListeners() {
        binding.prefMenuItemDelete.setOnClickListener { openWebBrowser(URL_DELETE) }
        binding.prefMenuItemDesactive.setOnClickListener { openWebBrowser(URL_DEACTIVATE) }
    }

    override fun initEvent() {
        binding.btnNavClose.setOnClickListener { findNavController().popBackStack() }
    }

    private fun initObservers() {
        viewModel.userUpdated.observe(this, this::onUserDeleted)
    }

    private fun onUserDeleted(response: Response<Unit>?) {
        if (response == null) return

        viewModel.resetUserUpdated()
    }
}