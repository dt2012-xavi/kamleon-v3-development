package com.dynatech2012.kamleonuserapp.fragments

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
        Log.d(TAG, "cxcxcx on init view registerFragment")
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

        binding.tvError.text = ""
        binding.errorLayout.visibility = View.GONE
    }

    override fun initEvent() {
        binding.btnCreateAccount.setOnClickListener {
            Log.d(TAG, "cxcxcx onClick create account with email: ${binding.inputBoxEmail.getEditTextView()?.text.toString()}|")
            Log.d(TAG, "cxcxcx onClick create account with password: ${binding.inputBoxPwd.getEditTextView()?.text.toString()}|")
            Log.d(TAG, "cxcxcx onClick create account with first name: ${binding.inputBoxFName.getEditTextView()?.text.toString()}|")
            Log.d(TAG, "cxcxcx onClick create account with last name: ${binding.inputBoxLName.getEditTextView()?.text.toString()}|")
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
        viewModel.registerInputs.observe(this) {
            Log.i(TAG, "cxcxcx register registerInputs: $it")
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
                5 -> {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.tvError.text = (getString(R.string.signup_error_email_used))
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
        Log.i(TAG,"cxcxcx startPrivacy from register fragment")
        findNavController().navigate(R.id.action_registerFragment_to_privacyFragment)
    }

    private fun updateButtonState() {
        val hasValidInput = binding.inputBoxFName.getEditTextView()?.text.toString().isNotBlank()
                && binding.inputBoxLName.getEditTextView()?.text.toString().isNotBlank()
                && binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
                && binding.inputBoxPwd.getEditTextView()?.text.toString().isNotBlank()
                && binding.inputBoxPwdRepeat.getEditTextView()?.text.toString().isNotBlank()
                && binding.inputBoxPwd.getEditTextView()?.text.toString() == binding.inputBoxPwdRepeat.getEditTextView()?.text.toString()
        binding.btnCreateAccount.isEnabled = hasValidInput
    }

    companion object {
        private const val TAG = "RegisterFragment"
    }
}