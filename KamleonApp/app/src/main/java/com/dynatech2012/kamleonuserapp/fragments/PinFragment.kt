package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityPinBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PinFragment : BaseFragment<ActivityPinBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityPinBinding = ActivityPinBinding.inflate(layoutInflater)
    private var secureInputCurrent: Boolean = true
    private var secureInputNew: Boolean = true
    private var secureInputConfirm: Boolean = true
    override fun initView() {
        updateInputMode()
        initObservers()
    }

    override fun initEvent() {
        val content = getString(R.string.pin_settings)
        val underlinedContent = SpannableString(content)
        underlinedContent.setSpan(UnderlineSpan(), 0, content.length, 0)
        binding.tvPinSettings.text = underlinedContent
        binding.tvPinSettings.setOnClickListener {
            viewModel.resetPIN()
        }


        binding.btnNavClose.setOnClickListener { findNavController().popBackStack() }
        binding.imageViewEye.isClickable = true
        binding.imageViewEye.setOnClickListener {
            secureInputCurrent = !secureInputCurrent
            updateInputMode()
        }
        binding.ivPinNewEye.isClickable = true
        binding.ivPinNewEye.setOnClickListener {
            secureInputNew = !secureInputNew
            updateInputMode()
        }
        binding.ivPinConfirmEye.isClickable = true
        binding.ivPinConfirmEye.setOnClickListener {
            secureInputConfirm = !secureInputConfirm
            updateInputMode()
        }
        binding.btnSave.setOnClickListener {
            viewModel.changePin(binding.etPin.text.toString(), binding.etPin.text.toString())
        }
    }

    private fun updateInputMode() {
        binding.imageViewEye.setImageResource(if (secureInputCurrent) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputCurrent) {
            binding.etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            binding.etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.ivPinNewEye.setImageResource(if (secureInputNew) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputNew) {
            binding.etPinNew.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            binding.etPinNew.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.ivPinConfirmEye.setImageResource(if (secureInputConfirm) R.drawable.ic_eye_crossed else R.drawable.ic_eye_open)
        if (secureInputConfirm) {
            binding.etPinConf.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            binding.etPinConf.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }

    private fun initObservers() {
        viewModel.userUpdated.observe(this, this::startActivity)
        viewModel.resetPINSuccess.observe(this) {
            if (it) showSuccessDialog()
            else showErrorDialog()
        }
    }

    private fun showSuccessDialog() {
        val title = getString(R.string.dialog_reset_pin_success_title)
        val message = getString(R.string.dialog_reset_pin_success_description)
        showReadyDialog(title, message)
    }

    private fun showErrorDialog() {
        val title = getString(R.string.dialog_reset_pin_error_title)
        val message = getString(R.string.dialog_reset_pin_error_description)
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

    private fun startActivity(updated: Boolean) {
        if (updated) {
            viewModel.resetUserUpdated()
        }
    }
}