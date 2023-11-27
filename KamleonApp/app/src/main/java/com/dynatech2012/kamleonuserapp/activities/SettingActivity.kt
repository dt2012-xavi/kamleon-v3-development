package com.dynatech2012.kamleonuserapp.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivitySettingBinding
import com.dynatech2012.kamleonuserapp.fragments.BottomFragmentDismissListener
import com.dynatech2012.kamleonuserapp.fragments.DataPickerFragment
import com.dynatech2012.kamleonuserapp.fragments.DatePickerFragment
import com.dynatech2012.kamleonuserapp.fragments.ImagePickFragment
import com.dynatech2012.kamleonuserapp.fragments.NotificationFragment
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.SettingMenuItemView
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class SettingActivity : BaseActivity<ActivitySettingBinding>(),
    SettingMenuItemView.SettingMenuItemViewListener {
    override fun setBinding(): ActivitySettingBinding = ActivitySettingBinding.inflate(layoutInflater)
    private var isSettingAccount: Boolean = true
    private val viewModel: MainViewModel by viewModels()
    private val onDismissPickImage = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {

        }
    }
    private val onDismissDatePicker = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {

        }
    }

    private val onDismissGenderPicker = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {

        }
    }

    private val onDismissWeightPicker = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {

        }
    }

    private val onDismissHeightPicker = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {

        }
    }

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

        initObservers()
        viewModel.getUserData()
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
            //showReadyDialog()
            updateUserData()
        }

        binding.profileLayout.setOnClickListener {
            val pickFragment = ImagePickFragment.newInstance(onDismissPickImage)
            pickFragment.show(supportFragmentManager, "ImagePick")
        }

        binding.btnSettingNoti.setOnClickListener {
            val notiFragment = NotificationFragment.newInstance(onDismissPickImage)
            notiFragment.show(supportFragmentManager, "Notifications")
        }

        binding.btnSettingClose.setOnClickListener {
            finish()
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
            viewModel.logout()
            //logoutDialog.dismiss()
            startActivity(Intent(this, InitActivity::class.java))
        }

        dialogView.findViewById<TextView>(R.id.tvBtnN).setText(R.string.dialog_logout_n)
        dialogView.findViewById<TextView>(R.id.tvBtnN).setOnClickListener {
            logoutDialog.dismiss()
        }
    }

    private fun showReadyDialog() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_dialog_profile_ready, null)

        dialog.setView(dialogView)
        dialog.setCancelable(false)
        val logoutDialog = dialog.show()
        logoutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<TextView>(R.id.tvDialogDesc).setOnClickListener {
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
            showWeightPicker()
        } else if (menuView == binding.prefMenuItemHeight) {
            showHeightPicker()
        } else if (menuView == binding.prefMenuItemGender) {
            showGenderPicker()
        } else if (menuView == binding.prefMenuItemBirth) {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val qrFragment = DatePickerFragment.newInstance(onDismissDatePicker, viewModel.userData.value?.dateOfBirth!!)
        qrFragment.show(supportFragmentManager, "DatePicker")
    }

    private fun showHeightPicker() {
        val value = viewModel.userData.value?.height?.toString()
        val dataFragment = DataPickerFragment.newInstance(onDismissHeightPicker, "height", value)
        dataFragment.show(supportFragmentManager, "HeightPicker")
    }

    private fun showWeightPicker() {
        val value = viewModel.userData.value?.weight?.toString()
        val dataFragment = DataPickerFragment.newInstance(onDismissWeightPicker, "weight", value)
        dataFragment.show(supportFragmentManager, "WeightPicker")
    }

    private fun showGenderPicker() {
        val value = viewModel.userData.value?.gender?.toString()
        val dataFragment = DataPickerFragment.newInstance(onDismissGenderPicker, "gender", value)
        dataFragment.show(supportFragmentManager, "GenderPicker")
    }

    private fun initObservers() {
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.userUpdated.observe(this, this::onUserDataUpdated)
    }

    private fun onUserDataChanged(userData: CustomUser) {
        binding.etAccSurName.setText(userData.name)
        binding.etAccName.setText(userData.lastName)
        binding.accMenuItemEmail.setValue(userData.email)
        binding.prefMenuItemWeight.setValue(userData.weight.toString())
        binding.prefMenuItemHeight.setValue(userData.height.toString())
        binding.prefMenuItemGender.setValue(userData.gender.toString())
        val localDate: LocalDate = userData.dateOfBirth.toInstant().atZone(ZoneId.systemDefault())
            .toLocalDate();
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
        val formattedDate = localDate.format(formatter)
        val age = Period.between(localDate, LocalDate.now()).years
        val stringDate = "$formattedDate ($age)"
        binding.prefMenuItemBirth.setValue(stringDate)
    }

    private fun onUserDataUpdated(updated: Boolean) {
        if (updated)
        {
            viewModel.resetUserUpdated()
            showReadyDialog()
        }
    }

    private fun updateUserData()
    {
        val data = hashMapOf<String, Any>()
        data["name"] = binding.etAccSurName.text.toString()
        data["lastName"] = binding.etAccName.text.toString()
        viewModel.updateUserData(data)
    }
}