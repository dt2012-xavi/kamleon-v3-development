package com.dynatech2012.kamleonuserapp.fragments

import android.text.Editable
import android.text.TextWatcher
import timber.log.Timber
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityRegisterBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : BaseFragment<ActivityRegisterBinding>() {
    private val viewModel: AuthViewModel by activityViewModels()

    override fun setBinding(): ActivityRegisterBinding =
        ActivityRegisterBinding.inflate(layoutInflater)

    private val inputWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(
            s: CharSequence, start: Int,
            count: Int, after: Int
        ) {
        }

        override fun onTextChanged(
            s: CharSequence, start: Int,
            before: Int, count: Int
        ) {
            updateButtonState()
        }
    }

    override fun initView() {
        Timber.d("Registration form displayed")
        binding.btnCreateAccount.isEnabled = false
        binding.inputBoxFName.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxLName.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxEmail.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxPwd.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_DONE

        binding.inputBoxFName.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxLName.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxEmail.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxPwd.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxPwdRepeat.getEditTextView()?.addTextChangedListener(inputWatcher)

        if (viewModel.recoveringProfile) {
            binding.inputBoxEmail.getEditTextView()?.setText(viewModel.recoveryEmail)
            binding.inputBoxEmail.isEnabled = false
            binding.inputBoxPwd.visibility = View.GONE
            binding.inputBoxPwdRepeat.visibility = View.GONE
            binding.btnCreateAccount.setText(R.string.signup_complete_profile)
        }
        binding.tvError.text = ""
        binding.errorLayout.visibility = View.GONE
        updateButtonState()
    }

    override fun initEvent() {
        binding.btnCreateAccount.setOnClickListener {
            Timber.d("Registration form submitted")
            viewModel.fName = binding.inputBoxFName.getEditTextView()?.text.toString().trim()
            viewModel.lName = binding.inputBoxLName.getEditTextView()?.text.toString().trim()
            viewModel.email = binding.inputBoxEmail.getEditTextView()?.text.toString().trim()
            viewModel.pass = binding.inputBoxPwd.getEditTextView()?.text.toString()
            //first check about the
            viewModel.checkRegisterInputs()
            //startPrivacy()
        }

        binding.tvToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        viewModel.resetRegisterInputs()
        viewModel.registerInputs.observe(viewLifecycleOwner) {
            Timber.d("Registration validation result: %s", it)
            when (it) {
                6 -> {
                    binding.errorLayout.visibility = View.GONE
                    startPrivacy()
                }
                0, 1 -> {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.tvError.text = (getString(R.string.signup_error_empty_fields))
                }
                2, 3 -> {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.tvError.text = (getString(R.string.signup_error_email_pattern))
                }
                4 -> {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.tvError.text = (getString(R.string.signup_error_password))
                }
                -3 -> {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.tvError.text = getString(R.string.signup_session_expired)
                }
            }
        }
    }

    /*
    override fun onDestroyView() {
        Log.d(TAG, "cxcxcx onDestroyView")
        binding.inputBoxFName.getEditTextView()?.text?.clear()
        binding.inputBoxLName.getEditTextView()?.text?.clear()
        binding.inputBoxEmail.getEditTextView()?.text?.clear()
        binding.inputBoxPwd.getEditTextView()?.text?.clear()
        binding.inputBoxPwdRepeat.getEditTextView()?.text?.clear()
        viewModel.fName = ""
        viewModel.lName = ""
        viewModel.email = ""
        viewModel.pass = ""
        binding.inputBoxFName.getEditTextView()?.removeTextChangedListener(inputWatcher)
        binding.inputBoxLName.getEditTextView()?.removeTextChangedListener(inputWatcher)
        binding.inputBoxEmail.getEditTextView()?.removeTextChangedListener(inputWatcher)
        binding.inputBoxPwd.getEditTextView()?.removeTextChangedListener(inputWatcher)
        binding.inputBoxPwdRepeat.getEditTextView()?.removeTextChangedListener(inputWatcher)
        super.onDestroyView()
    }
    */

    private fun startPrivacy() {
        Timber.d("Navigating to privacy consent")
        findNavController().navigate(R.id.action_registerFragment_to_privacyFragment)
    }

    private fun updateButtonState() {
        val hasValidInput = binding.inputBoxFName.getEditTextView()?.text.toString().isNotBlank()
                && binding.inputBoxLName.getEditTextView()?.text.toString().isNotBlank()
                && binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
                && (viewModel.recoveringProfile ||
                    (binding.inputBoxPwd.getEditTextView()?.text.toString().isNotBlank()
                        && binding.inputBoxPwdRepeat.getEditTextView()?.text.toString().isNotBlank()
                        && binding.inputBoxPwd.getEditTextView()?.text.toString() ==
                        binding.inputBoxPwdRepeat.getEditTextView()?.text.toString()))
        binding.btnCreateAccount.isEnabled = hasValidInput
    }

}
