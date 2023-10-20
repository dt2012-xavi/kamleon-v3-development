package com.kamleonapp.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.kamleonapp.R
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivitySettingBinding
import com.kamleonapp.views.SettingMenuItemView


class SettingActivity : BaseActivity<ActivitySettingBinding>(),
    SettingMenuItemView.SettingMenuItemViewListener {
    override fun setBinding(): ActivitySettingBinding = ActivitySettingBinding.inflate(layoutInflater)
    private var isSettingAccount: Boolean = true

    override fun initView() {
        updateOptionViewUI()

        binding.etAccName.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etAccSurName.imeOptions = EditorInfo.IME_ACTION_DONE

        val menuItems: Array<SettingMenuItemView> = arrayOf(
            binding.accMenuItemEmail,
            binding.accMenuItemPwd,
            binding.accMenuItemPin,
            binding.accMenuItemHelp,
            binding.accMenuItemTOS,
            binding.accMenuItemDSP,
            binding.accMenuItemPP,
            binding.prefMenuItemWeight,
            binding.prefMenuItemHeight,
            binding.prefMenuItemGender,
            binding.prefMenuItemBirth
        )

        for (menuItem in menuItems) {
            menuItem.setSettingMenuItemListener(this)
        }
    }

    override fun initEvent() {
        binding.optionViewAcc.setOnClickListener {
            if (!isSettingAccount) {
                isSettingAccount = true
                updateOptionViewUI()
            }
        }

        binding.optionViewPref.setOnClickListener {
            if (isSettingAccount) {
                isSettingAccount = false
                updateOptionViewUI()
            }
        }

        binding.optionViewLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.btnSave.setOnClickListener {

        }
    }

    private fun updateOptionViewUI() {
        if (isSettingAccount) {
            binding.optionViewAcc.background = resources.getDrawable(R.drawable.bg_setting_option_layout)
//            binding.optionViewAcc.setBackgroundResource(R.drawable.bg_setting_option_layout)
//            binding.optionViewAcc.setBackgroundResource(R.drawable.bg_setting_option_shadow)
            binding.optionViewPref.background = null

            binding.layoutAccount.visibility = View.VISIBLE
            binding.layoutPreference.visibility = View.GONE
        } else {
            binding.optionViewAcc.background = null
            binding.optionViewPref.background = resources.getDrawable(R.drawable.bg_setting_option_layout)
//            binding.optionViewPref.setBackgroundResource(R.drawable.bg_setting_option_layout)
//            binding.optionViewPref.setBackgroundResource(R.drawable.bg_setting_option_shadow)

            binding.layoutAccount.visibility = View.GONE
            binding.layoutPreference.visibility = View.VISIBLE
        }
    }

    private fun showLogoutDialog() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_dialog_yn, null)

        dialog.setView(dialogView)
        dialog.setCancelable(false)
        val logoutDialog = dialog.show()
        logoutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.findViewById<TextView>(R.id.tvDialogDesc).setText(R.string.dialog_logout_msg)
        dialogView.findViewById<TextView>(R.id.tvBtnY).setText(R.string.dialog_logout_y)
        dialogView.findViewById<TextView>(R.id.tvBtnY).setOnClickListener {
            logoutDialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.tvBtnN).setText(R.string.dialog_logout_n)
        dialogView.findViewById<TextView>(R.id.tvBtnN).setOnClickListener {
            logoutDialog.dismiss()
        }
    }

    private fun showDeleteAccountDialog() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_dialog_yn_delete, null)

        dialog.setView(dialogView)
        dialog.setCancelable(false)
        val logoutDialog = dialog.show()
        logoutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.findViewById<TextView>(R.id.tvDialogDesc).setText(R.string.dialog_desactive_msg)
        dialogView.findViewById<TextView>(R.id.tvBtnY).setText(R.string.dialog_desactive_y)
        dialogView.findViewById<TextView>(R.id.tvBtnY).setOnClickListener {
            logoutDialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.tvBtnN).setText(R.string.dialog_desactive_n)
        dialogView.findViewById<TextView>(R.id.tvBtnN).setOnClickListener {
            logoutDialog.dismiss()
        }
    }

    override fun onSwitchChanged(menuView: SettingMenuItemView, isOn: Boolean) {

    }

    override fun onMenuItemClicked(menuView: SettingMenuItemView) {
        if (menuView == binding.accMenuItemEmail) {
            startActivity(Intent(this, EmailActivity::class.java))
        } else if (menuView == binding.accMenuItemPwd) {
            startActivity(Intent(this, PwdActivity::class.java))
        } else if (menuView == binding.accMenuItemPin) {
            startActivity(Intent(this, PinActivity::class.java))
        } else if (menuView == binding.accMenuItemHelp) {
            openWebBrowser("https://help.com")
        } else if (menuView == binding.accMenuItemTOS) {
            openWebBrowser("https://terms_of_use.com")
        } else if (menuView == binding.accMenuItemDSP) {
            showDeleteAccountDialog()
        } else if (menuView == binding.accMenuItemPP) {
            openWebBrowser("https://privacy_policy.com")
        } else if (menuView == binding.prefMenuItemWeight) {

        } else if (menuView == binding.prefMenuItemHeight) {

        } else if (menuView == binding.prefMenuItemGender) {

        } else if (menuView == binding.prefMenuItemBirth) {

        }
    }
}