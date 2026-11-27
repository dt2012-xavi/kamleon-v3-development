package com.dynatech2012.kamleonuserapp.fragments

import com.google.android.material.snackbar.Snackbar
import com.dynatech2012.kamleonuserapp.views.settings.contactSupport
import com.dynatech2012.kamleonuserapp.views.settings.openInAppBrowser
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.analytics.Analytics
import com.dynatech2012.kamleonuserapp.application.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PrivacyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    override fun onResume() {
        super.onResume()
        // Still the signup: an attempt left idle too long is dropped here, without starting another.
        Analytics.auth?.keepAlive()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            AppTheme(useDarkTheme = false) {
                PrivacyScreen(
                    // Pop instead of navigating forward: the Register screen below keeps what was typed.
                    onBack = { findNavController().popBackStack() },
                    onNext = {
                        Timber.d("Privacy consent accepted; opening onboarding")
                        findNavController().navigate(R.id.action_privacyFragment_to_onboardingFragment)
                    },
                    onOpenUrl = { url ->
                        if (!requireContext().openInAppBrowser(url)) {
                            Snackbar.make(requireView(), R.string.error_no_browser, Snackbar.LENGTH_SHORT).show()
                        }
                    },
                    onEmail = {
                        if (!requireContext().contactSupport()) {
                            Snackbar.make(requireView(), R.string.help_error_mail, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
