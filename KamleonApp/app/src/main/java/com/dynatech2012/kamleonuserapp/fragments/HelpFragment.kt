package com.dynatech2012.kamleonuserapp.fragments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityHelpBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpFragment : BaseFragment<ActivityHelpBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityHelpBinding = ActivityHelpBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnNavClose.setOnClickListener { findNavController().popBackStack() }
        binding.menuAppGuide.setOnClickListener {
            viewModel.tutorialComingFromHome = false
            findNavController().navigate(R.id.action_helpFragment_to_onBoardingFragment)
        }

        binding.menuContact.setOnClickListener {
            sendEmail()
        }
    }

    private fun sendEmail() {
        val email = "info@kamleon.com"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }

        // Verify that the intent will resolve to an activity
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            // Show an error message
            Snackbar.make(binding.root, R.string.help_error_mail, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun initEvent() {

    }





    private fun startActivity(updated: Boolean) {
        if (updated) {
            viewModel.resetUserUpdated()
        }
    }
}