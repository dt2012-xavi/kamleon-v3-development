package com.dynatech2012.kamleonuserapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Startup screen. It only plays the animated logo for [MIN_DURATION_MS] and reports that to the
 * ViewModel; `InitActivity` leaves it once that time has passed and the session check is done.
 */
@AndroidEntryPoint
class SplashFragment : Fragment() {
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { AppTheme(useDarkTheme = false) { SplashScreen() } }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Splash screen displayed")
        viewLifecycleOwner.lifecycleScope.launch {
            delay(MIN_DURATION_MS)
            viewModel.signalSplashDurationCompleted()
        }
    }

    companion object {
        /** Minimum time the splash stays up, even if the session check finishes sooner. */
        const val MIN_DURATION_MS = 2000L
    }
}
