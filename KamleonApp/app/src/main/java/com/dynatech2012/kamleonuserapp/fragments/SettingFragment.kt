package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.adapters.OrganizationsListAdapter
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivitySettingBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Organization
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.SettingMenuItemView
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class SettingFragment : BaseFragment<ActivitySettingBinding>(),
    SettingMenuItemView.SettingMenuItemViewListener {
    override fun setBinding(): ActivitySettingBinding = ActivitySettingBinding.inflate(layoutInflater)
    private var isSettingAccount: Boolean = true
    private val viewModel: MainViewModel by activityViewModels()
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
        Log.d(TAG, "init view")
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
        setupAdapter()
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
            Log.d(TAG, "show pick image fragment")
            childFragmentManager.beginTransaction().add(pickFragment, "ImagePick").commit()
            //pickFragment.show(parentFragmentManager, "ImagePick")
        }

        binding.btnSettingNoti.setOnClickListener {
            binding.btnSettingNotiBadge.visibility = View.GONE
            val notiFragment = InvitationFragment.newInstance(onDismissPickImage)
            notiFragment.show(parentFragmentManager, "Notifications")
        }

        binding.btnSettingClose.setOnClickListener {
            //val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            //val navController = navHostFragment.navController
            findNavController().popBackStack()
            //findNavController().navigateUp()
            //activity?.finish()
        }

        binding.tvDelete.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.action_settingFragment_to_deleteFragment)
        }
    }

    private fun updateOptionViewUI() {
        if (isSettingAccount) {
            binding.optionViewAcc.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_setting_option_layout)
//            binding.optionViewAcc.setBackgroundResource(R.drawable.bg_setting_option_layout)
//            binding.optionViewAcc.setBackgroundResource(R.drawable.bg_setting_option_shadow)
            binding.optionViewPref.background = null

            binding.layoutAccount.visibility = View.VISIBLE
            binding.layoutPreference.visibility = View.GONE
        } else {
            binding.optionViewAcc.background = null
            binding.optionViewPref.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_setting_option_layout)
//            binding.optionViewPref.setBackgroundResource(R.drawable.bg_setting_option_layout)
//            binding.optionViewPref.setBackgroundResource(R.drawable.bg_setting_option_shadow)

            binding.layoutAccount.visibility = View.GONE
            binding.layoutPreference.visibility = View.VISIBLE
        }
    }

    private fun showLogoutDialog() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
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
            logoutDialog.dismiss()
            //activity?.startActivity(Intent(requireContext(), InitActivity::class.java))
            activity?.finish()
        }

        dialogView.findViewById<TextView>(R.id.tvBtnN).setText(R.string.dialog_logout_n)
        dialogView.findViewById<TextView>(R.id.tvBtnN).setOnClickListener {
            logoutDialog.dismiss()
        }
    }

    private fun showReadyDialog() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
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
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_dialog_yn_delete, null)

        dialog.setView(dialogView)
        dialog.setCancelable(false)
        val logoutDialog = dialog.show()
        logoutDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.findViewById<TextView>(R.id.tvDialogDesc).setText(R.string.dialog_deactive_msg)
        dialogView.findViewById<TextView>(R.id.tvBtnY).setText(R.string.dialog_deactive_y)
        dialogView.findViewById<TextView>(R.id.tvBtnY).setOnClickListener {
            logoutDialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.tvBtnN).setText(R.string.dialog_deactive_n)
        dialogView.findViewById<TextView>(R.id.tvBtnN).setOnClickListener {
            logoutDialog.dismiss()
        }
    }

    override fun onSwitchChanged(menuView: SettingMenuItemView, isOn: Boolean) {

    }

    override fun onMenuItemClicked(menuView: SettingMenuItemView) {
        when (menuView) {
            binding.accMenuItemEmail -> {
                findNavController().navigate(R.id.action_settingFragment_to_emailFragment)
            }
            binding.accMenuItemPwd -> {
                findNavController().navigate(R.id.action_settingFragment_to_pwdFragment)
            }
            binding.accMenuItemPin -> {
                findNavController().navigate(R.id.action_settingFragment_to_pinFragment)
            }
            binding.accMenuItemHelp -> {
                openWebBrowser("https://help.com")
            }
            binding.accMenuItemTOS -> {
                openWebBrowser("https://terms_of_use.com")
            }
            binding.accMenuItemDSP -> {
                showDeleteAccountDialog()
            }
            binding.accMenuItemPP -> {
                openWebBrowser("https://privacy_policy.com")
            }
            binding.prefMenuItemWeight -> {
                showWeightPicker()
            }
            binding.prefMenuItemHeight -> {
                showHeightPicker()
            }
            binding.prefMenuItemGender -> {
                showGenderPicker()
            }
            binding.prefMenuItemBirth -> {
                showDatePicker()
            }
        }
    }

    private fun showDatePicker() {
        val dateFragment = DatePickerFragment.newInstance(onDismissDatePicker, viewModel.userData.value?.dateOfBirth!!)
        dateFragment.show(parentFragmentManager, "DatePicker")
    }

    private fun showHeightPicker() {
        val value = viewModel.userData.value?.height?.toString()
        val dataFragment = DataPickerFragment.newInstance(onDismissHeightPicker, "height", value)
        dataFragment.show(parentFragmentManager, "HeightPicker")
    }

    private fun showWeightPicker() {
        val value = viewModel.userData.value?.weight?.toString()
        val dataFragment = DataPickerFragment.newInstance(onDismissWeightPicker, "weight", value)
        dataFragment.show(parentFragmentManager, "WeightPicker")
    }

    private fun showGenderPicker() {
        val value = viewModel.userData.value?.gender?.toString()
        val dataFragment = DataPickerFragment.newInstance(onDismissGenderPicker, "gender", value)
        dataFragment.show(parentFragmentManager, "GenderPicker")
    }

    private fun initObservers() {
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.userProfiles.observe(this, this::onUserProfilesChanged)
        viewModel.userUpdated.observe(this, this::onUserDataUpdated)
        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
        viewModel.newInvitations.observe(this, this::onNewInvitation)
    }

    private fun setupAdapter() {
        val adapter = OrganizationsListAdapter()
        binding.rvSettingOrganizations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSettingOrganizations.adapter = adapter
    }

    private fun onUserDataChanged(userData: CustomUser) {
        Log.d(TAG, "on user data changed")
        val initials = getString(R.string.user_name_initials, userData.name.substring(0, 1).uppercase(), userData.lastName.substring(0, 1).uppercase())
        binding.tvSettingProfileName.text = initials
        binding.tvProfileName.text = getString(R.string.setting_display_name, userData.name, userData.lastName)
        binding.etAccSurName.setText(userData.lastName)
        binding.etAccName.setText(userData.name)
        binding.accMenuItemEmail.setValue(userData.email)
        binding.prefMenuItemWeight.setValue(getString(R.string.setting_label_user_weight_text, userData.weight.toString()))
        binding.prefMenuItemHeight.setValue(getString(R.string.setting_label_user_height_text, userData.height.toString()))
        binding.prefMenuItemGender.setValue(userData.gender.raw)
        val localDate: LocalDate = userData.dateOfBirth.toInstant().atZone(ZoneId.systemDefault())
            .toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val formattedDate = localDate.format(formatter)
        val age = Period.between(localDate, LocalDate.now()).years
        val stringDate = "$formattedDate ($age)"
        binding.prefMenuItemBirth.setValue(formattedDate)

    }

    private fun onUserProfilesChanged(organizations: ArrayList<Organization>) {
        Log.d(TAG, "on user profiles changed: $organizations")
        if (organizations.isEmpty()) {
            binding.llSettingAccountInfoSingle.visibility = View.GONE
            binding.llSettingAccountInfoMultiple.visibility = View.GONE
            return
        }
        else if (organizations.size == 1) {
            binding.llSettingAccountInfoMultiple.visibility = View.GONE
            binding.tvSettingAccountOrg.text = organizations[0].organizationName
            binding.tvSettingAccountCenter.text = organizations[0].centerName
            binding.tvSettingAccountTeam.text = organizations[0].teamName
            binding.llSettingAccountInfoSingle.visibility = View.VISIBLE
            return
        }
        else {
            binding.llSettingAccountInfoSingle.visibility = View.GONE
            val adapter = binding.rvSettingOrganizations.adapter as OrganizationsListAdapter
            adapter.submitList(organizations.toList())
            binding.llSettingAccountInfoMultiple.visibility = View.VISIBLE
        }
    }

    private fun onUserDataUpdated(updated: Boolean) {
        if (updated)
        {
            viewModel.resetUserUpdated()
            showReadyDialog()
        }
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(HomeFragment.TAG, "image changed")
        drawable?.let {
            binding.ivSettProfile.load(drawable)
            binding.ivSettProfile.visibility = View.VISIBLE
            return
        }
    }

    private fun updateUserData()
    {
        val data = hashMapOf<String, Any>()
        data["name"] = binding.etAccSurName.text.toString()
        data["lastName"] = binding.etAccName.text.toString()
        viewModel.updateUserData(data)
    }

    private fun onNewInvitation(isNew: Boolean) {
        if (isNew)
            binding.btnSettingNotiBadge.visibility = View.VISIBLE
    }

    companion object {
        val TAG: String = SettingFragment::class.java.simpleName
    }
}