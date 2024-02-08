package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityForgotPwdBinding
import com.dynatech2012.kamleonuserapp.viewmodels.ForgotPwdViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPwdFragment : BaseFragment<ActivityForgotPwdBinding>() {
    override fun setBinding(): ActivityForgotPwdBinding = ActivityForgotPwdBinding.inflate(layoutInflater)
    private val viewModel: ForgotPwdViewModel by viewModels()
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

        initObservers()
    }

    override fun initEvent() {
        binding.btnClose.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPwdFragment_to_loginFragment)
        }

        binding.tvCreateNewAcc.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPwdFragment_to_registerFragment)
        }

        binding.btnSendLoginLink.setOnClickListener {
            viewModel.resetPwd(binding.inputBoxEmail.getEditTextView()?.text.toString())
        }
    }

    private fun initObservers() {
        viewModel.resetPwdSuccess.observe(this) {
            if (it) showSuccessDialog()
            else showErrorDialog()
        }
    }


    private fun showSuccessDialog() {
        val title = getString(R.string.dialog_reset_pwd_success_title)
        val message = getString(R.string.dialog_reset_pwd_success_description)
        showReadyDialog(title, message)
    }

    private fun showErrorDialog() {
        val title = getString(R.string.dialog_reset_pwd_error_title)
        val message = getString(R.string.dialog_reset_pwd_error_description)
        showReadyDialog(title, message)
    }

    private fun showReadyDialog(title: String, message: String) {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_dialog_ok, null)

        dialog.setView(dialogView)
        dialog.setCancelable(false)
        // -3 is email not valid, -2 is password not valid, -1 is firebase error
        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = title
        dialogView.findViewById<TextView>(R.id.tvDialogDesc).text = message
        val logoutDialog = dialog.show()
        logoutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<TextView>(R.id.tvBtnOk).setOnClickListener {
            logoutDialog.dismiss()
        }
    }

    companion object {
        private val TAG = ForgotPwdFragment::class.java.simpleName
    }
}