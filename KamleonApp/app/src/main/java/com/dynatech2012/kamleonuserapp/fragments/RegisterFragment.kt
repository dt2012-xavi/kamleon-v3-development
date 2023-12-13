package com.dynatech2012.kamleonuserapp.fragments

import android.text.Editable
import android.text.TextWatcher
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

    override fun setBinding(): ActivityRegisterBinding = ActivityRegisterBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnCreateAccount.isEnabled = false
        binding.inputBoxFName.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxLName.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxEmail.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.inputBoxPwd.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_DONE

        val inputWatcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                updateButtonState()
            }
        }

        binding.inputBoxFName.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxLName.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxEmail.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxPwd.getEditTextView()?.addTextChangedListener(inputWatcher)
        binding.inputBoxPwdRepeat.getEditTextView()?.addTextChangedListener(inputWatcher)
    }

    override fun initEvent() {
        binding.btnCreateAccount.setOnClickListener {
            viewModel.fName = binding.inputBoxFName.getEditTextView()?.text.toString()
            viewModel.lName = binding.inputBoxLName.getEditTextView()?.text.toString()
            viewModel.email = binding.inputBoxEmail.getEditTextView()?.text.toString()
            viewModel.pass = binding.inputBoxPwd.getEditTextView()?.text.toString()
            startPrivacy()
        }

        binding.btnNavBack.setOnClickListener {
            activity?.finish()
        }
        binding.tvToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }
    private fun startPrivacy() {
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
}