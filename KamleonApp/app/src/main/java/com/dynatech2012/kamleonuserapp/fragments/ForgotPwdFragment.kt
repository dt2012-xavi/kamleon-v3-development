package com.dynatech2012.kamleonuserapp.fragments

import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityForgotPwdBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPwdFragment : BaseFragment<ActivityForgotPwdBinding>() {
    override fun setBinding(): ActivityForgotPwdBinding = ActivityForgotPwdBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnSendLoginLink.isEnabled = false

        binding.inputBoxEmail.getEditTextView()?.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.inputBoxEmail.getEditTextView()?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                binding.btnSendLoginLink.isEnabled = binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
            }
        })
    }

    override fun initEvent() {
        binding.btnClose.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPwdFragment_to_loginFragment)
        }

        binding.tvCreateNewAcc.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPwdFragment_to_registerFragment)
        }
    }
}