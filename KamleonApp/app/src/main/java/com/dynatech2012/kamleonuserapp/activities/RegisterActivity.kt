package com.dynatech2012.kamleonuserapp.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityRegisterBinding
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {
    private val viewModel: AuthViewModel by viewModels()

    override fun setBinding(): ActivityRegisterBinding = ActivityRegisterBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
    }

    override fun initEvent() {
        binding.btnCreateAccount.setOnClickListener {
            viewModel.fName = binding.inputBoxFName.getEditTextView()?.text.toString()
            viewModel.lName = binding.inputBoxLName.getEditTextView()?.text.toString()
            viewModel.email = binding.inputBoxEmail.getEditTextView()?.text.toString()
            viewModel.pass = binding.inputBoxPwd.getEditTextView()?.text.toString()
            startActivity()
        }
        binding.btnNavBack.setOnClickListener { finish() }
        binding.tvToLogin.setOnClickListener {
            val to = Intent(this, LoginActivity::class.java)
            to.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(to)
        }
    }

    private fun startActivity() {
        val intent = Intent(this, PrivacyActivity::class.java)
        intent.putExtra("fName", viewModel.fName)
        intent.putExtra("lName", viewModel.lName)
        intent.putExtra("email", viewModel.email)
        intent.putExtra("pass", viewModel.pass)
        startActivity(intent)
    }

    private fun updateButtonState() {
        val hasValidInput = binding.inputBoxFName.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxLName.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxEmail.getEditTextView()?.text.toString().isNotBlank()
                            && binding.inputBoxPwd.getEditTextView()?.text.toString().isNotBlank()
        binding.btnCreateAccount.isEnabled = hasValidInput
    }
}