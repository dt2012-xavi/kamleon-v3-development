package com.dynatech2012.kamleonuserapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private val viewModel: AuthViewModel by activityViewModels()

    // Validation message currently shown under the form (null = none).
    private var error by mutableStateOf<Int?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Registration form displayed")
        val recovering = viewModel.recoveringProfile
        error = null

        (view as ComposeView).setContent {
            var firstName by rememberSaveable { mutableStateOf("") }
            var lastName by rememberSaveable { mutableStateOf("") }
            var email by rememberSaveable { mutableStateOf(if (recovering) viewModel.recoveryEmail.orEmpty() else "") }
            // Passwords are deliberately not saved in instance state.
            var password by remember { mutableStateOf("") }
            var repeatPassword by remember { mutableStateOf("") }

            AppTheme(useDarkTheme = false) {
                RegisterScreen(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    password = password,
                    repeatPassword = repeatPassword,
                    recovering = recovering,
                    error = error,
                    onFirstNameChange = { firstName = it; error = null },
                    onLastNameChange = { lastName = it; error = null },
                    onEmailChange = { email = it; error = null },
                    onPasswordChange = { password = it; error = null },
                    onRepeatPasswordChange = { repeatPassword = it; error = null },
                    onCreate = {
                        Timber.d("Registration form submitted")
                        viewModel.fName = firstName.trim()
                        viewModel.lName = lastName.trim()
                        viewModel.email = email.trim()
                        viewModel.pass = password
                        viewModel.checkRegisterInputs()
                    },
                    onLogin = {
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                )
            }
        }

        viewModel.resetRegisterInputs()
        viewModel.registerInputs.observe(viewLifecycleOwner) {
            Timber.d("Registration validation result: %s", it)
            when (it) {
                6 -> {
                    error = null
                    startPrivacy()
                }
                0, 1 -> error = R.string.signup_error_empty_fields
                2, 3 -> error = R.string.signup_error_email_pattern
                4 -> error = R.string.signup_error_password
                -3 -> error = R.string.signup_session_expired
            }
        }
    }

    private fun startPrivacy() {
        Timber.d("Navigating to privacy consent")
        findNavController().navigate(R.id.action_registerFragment_to_privacyFragment)
    }
}
