package com.dynatech2012.kamleonuserapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            AppTheme(useDarkTheme = false) {
                PrivacyScreen(
                    onBack = {
                        findNavController().navigate(R.id.action_privacyFragment_to_registerFragment)
                    },
                    onNext = {
                        Timber.d("Privacy consent accepted; opening onboarding")
                        findNavController().navigate(R.id.action_privacyFragment_to_onboardingFragment)
                    },
                    onOpenUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
                    onEmail = { address ->
                        startActivity(Intent(Intent.ACTION_SENDTO).apply { data = "mailto:$address".toUri() })
                    }
                )
            }
        }
    }
}
