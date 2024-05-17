package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityPwdBinding
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PwdFragment : BaseFragment<ActivityPwdBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityPwdBinding = ActivityPwdBinding.inflate(layoutInflater)
    private var secureInputCurrent: Boolean = true
    private var secureInputNew: Boolean = true
    private var secureInputConfirm: Boolean = true
    override fun initView() {
        updateInputMode()
        initObservers()
    }

    override fun initEvent() {

        binding.tvPwdSettings.setOnClickListener {
            viewModel.resetPwd()
        }

        binding.btnNavClose.setOnClickListener { findNavController().popBackStack() }
        binding.imageViewEye.isClickable = true
        binding.imageViewEye.setOnClickListener {
            secureInputCurrent = !secureInputCurrent
            updateInputMode()
        }
        binding.ivPwdNewEye.isClickable = true
        binding.ivPwdNewEye.setOnClickListener {
            secureInputNew = !secureInputNew
            updateInputMode()
        }
        binding.ivPwdConfirmEye.isClickable = true
        binding.ivPwdConfirmEye.setOnClickListener {
            secureInputConfirm = !secureInputConfirm
            updateInputMode()
        }
        binding.btnSave.setOnClickListener {
            // TODO: old pass needed to reauthenticate
            val oldPwd = binding.etPwd.text.toString()
            viewModel.changePwd(oldPwd, binding.etPwd.text.toString())
        }
        binding.etPwd.addTextChangedListener(passTextWatcher)
        binding.etPwdNew.addTextChangedListener(passTextWatcher)
        binding.etPwdConf.addTextChangedListener(passTextWatcher)
    }

    private val passTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        override fun afterTextChanged(s: Editable?) {
            binding.btnSave.isEnabled = passValidated
        }
    }

    private val passValidated: Boolean
        get() = binding.etPwd.text.toString().isNotEmpty()
                && binding.etPwdNew.text.toString().isNotEmpty()
                && binding.etPwdConf.text.toString().isNotEmpty()

    private fun updateInputMode() {
        binding.imageViewEye.setImageResource(if (secureInputCurrent) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputCurrent) {
            binding.etPwd.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            binding.etPwd.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.ivPwdNewEye.setImageResource(if (secureInputNew) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputNew) {
            binding.etPwdNew.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            binding.etPwdNew.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.ivPwdConfirmEye.setImageResource(if (secureInputConfirm) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputConfirm) {
            binding.etPwdConf.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            binding.etPwdConf.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }

    private fun initObservers() {
        viewModel.userUpdated.observe(this, this::onPwdUpdated)

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

    private fun onPwdUpdated(response: Response<Unit>?) {
        if (response == null) return

        viewModel.resetUserUpdated()
    }
}