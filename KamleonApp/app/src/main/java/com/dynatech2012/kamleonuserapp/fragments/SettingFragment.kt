package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.graphics.Color
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dynatech2012.kamleonuserapp.views.showActionFailedSheet
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.InitActivity
import com.dynatech2012.kamleonuserapp.adapters.OrganizationsListAdapter
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.databinding.ActivitySettingBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.Organization
import com.dynatech2012.kamleonuserapp.utils.notificationPermissionAsked
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.SettingMenuItemView
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.dynatech2012.kamleonuserapp.views.showProfileImage
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.views.settings.ProfileField
import com.dynatech2012.kamleonuserapp.views.settings.ProfileFieldSheet
import com.dynatech2012.kamleonuserapp.views.settings.ProfileValues

@AndroidEntryPoint
class SettingFragment : BaseFragment<ActivitySettingBinding>(),
    SettingMenuItemView.SettingMenuItemViewListener {
    override fun setBinding(): ActivitySettingBinding =
        ActivitySettingBinding.inflate(layoutInflater)

    private val viewModel: MainViewModel by activityViewModels()

    // What the profile says about notifications. The switch shows it only while the system also
    // lets the app post notifications (see [refreshNotificationSwitch]).
    private var notificationsEnabledInProfile = true
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            requireContext().notificationPermissionAsked = true
            if (granted) saveNotifications(true)
            refreshNotificationSwitch()
        }
    private val onDismissPickImage = object : BottomFragmentDismissListener {
        override fun onDismissFragment() {

        }
    }
    override fun initView() {
        DebugLog.d(TAG) { "init view" }

        val menuItems: Array<SettingMenuItemView> = arrayOf(
            binding.accMenuItemEmail,
            binding.accMenuItemPwd,
            binding.accMenuItemPin,
            binding.accMenuItemHelp,
            binding.accMenuItemTOS,
            binding.accMenuItemDSP,
            binding.accMenuItemPP,
            binding.prefMenuItemName,
            binding.prefMenuItemWeight,
            binding.prefMenuItemHeight,
            binding.prefMenuItemGender,
            binding.prefMenuItemBirth,
            binding.smNotiSwitch
        )

        for (menuItem in menuItems) {
            menuItem.setSettingMenuItemListener(this)
        }
        setupAdapter()
        initObservers()
        onKamleonSheetResult(SHEET_LOGOUT) { if (it == SheetResult.PRIMARY) logout() }
        onKamleonSheetResult(SHEET_NOTIFICATIONS_BLOCKED) { if (it == SheetResult.PRIMARY) openNotificationSettings() }
        childFragmentManager.setFragmentResultListener(ProfileFieldSheet.RESULT_SAVED, viewLifecycleOwner) { _, _ ->
            showSavedDialog()
        }
        viewModel.getUserData()
    }

    override fun initEvent() {
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.profileLayout.setOnClickListener {
            /*val lastImagePickFragment = childFragmentManager.fragments.lastOrNull() as? ImagePickFragment
            val ft = childFragmentManager.beginTransaction()
            if (lastImagePickFragment != null) {
                ft.remove(lastImagePickFragment)
                //return@setOnClickListener
            }
            Log.d(TAG, "show pick image fragment"
             */
            val pickFragment = ImagePickFragment.newInstance(onDismissPickImage)
            //ft.add(pickFragment, "ImagePick").commit()
            pickFragment.show(childFragmentManager, "ImagePick")
            DebugLog.d(TAG) { "image pick dialog after show" }
        }

        binding.btnSettingNoti.setOnClickListener {
            //binding.btnSettingNotiBadge.visibility = View.GONE
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

        binding.tvNotiOpenSettings.setOnClickListener { openNotificationSettings() }

        binding.tvDelete.setOnClickListener {
            val navHostFragment =
                requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.action_settingFragment_to_deleteFragment)
        }
    }

    private fun showSavedDialog() {
        Snackbar.make(binding.root, R.string.dialog_saved_text, Snackbar.LENGTH_SHORT).show()
    }

    private fun showLogoutDialog() {
        childFragmentManager.showKamleonSheet(
            SHEET_LOGOUT,
            SheetSpec(
                message = getString(R.string.dialog_logout_msg),
                primary = getString(R.string.dialog_logout_y),
                secondary = getString(R.string.dialog_logout_n),
                destructive = true,
                dismissible = false
            )
        )
    }

    private var loggingOut = false

    private fun logout() {
        // The sheet closes at once, so a second tap could start a second logout.
        if (loggingOut) return
        loggingOut = true
        // Observe with the activity, not the view: leaving this screen while the session and the
        // local data are being cleared must not cancel the redirect to the login.
        viewModel.logoutStatus.observe(requireActivity()) {
            if (it) {
                // Clear the whole task: no screen of the previous session may stay underneath.
                activity?.startActivity(
                    Intent(requireContext(), InitActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                activity?.finish()
            }
        }
        viewModel.logout()
    }

    override fun onSwitchChanged(menuView: SettingMenuItemView, isOn: Boolean) {
        when (menuView) {
            binding.smNotiSwitch -> onNotificationsSwitched(isOn)
        }
    }

    override fun onMenuItemClicked(menuView: SettingMenuItemView) {
        when (menuView) {
            binding.accMenuItemPwd -> {
                findNavController().navigate(R.id.action_settingFragment_to_pwdFragment)
            }

            binding.accMenuItemPin -> {
                findNavController().navigate(R.id.action_settingFragment_to_pinFragment)
            }

            binding.accMenuItemHelp -> {
                //viewModel.tutorialComingFromHome = false
                findNavController().navigate(R.id.action_settingFragment_to_helpFragment)
            }

            binding.accMenuItemDSP -> {
                openWebBrowser(UrlConstants.URL_CONSENT)
            }

            binding.accMenuItemTOS -> {
                openWebBrowser(UrlConstants.URL_TERMS)
            }

            binding.accMenuItemPP -> {
                openWebBrowser(UrlConstants.URL_POLICY_APP)
            }

            binding.prefMenuItemName -> showProfileSheet(ProfileField.NAME)
            binding.prefMenuItemGender -> showProfileSheet(ProfileField.GENDER)
            binding.prefMenuItemBirth -> showProfileSheet(ProfileField.BIRTH)
            binding.prefMenuItemHeight -> showProfileSheet(ProfileField.HEIGHT)
            binding.prefMenuItemWeight -> showProfileSheet(ProfileField.WEIGHT)
        }
    }

    /** One sheet at a time: a second tap while one is opening does nothing. */
    private fun showProfileSheet(field: ProfileField) {
        if (viewModel.userData.value == null || childFragmentManager.findFragmentByTag(PROFILE_SHEET) != null) return
        ProfileFieldSheet.newInstance(field).show(childFragmentManager, PROFILE_SHEET)
    }

    private fun initObservers() {
        viewModel.userData.observe(viewLifecycleOwner, this::onUserDataChanged)
        viewModel.userProfiles.observe(viewLifecycleOwner, this::onUserProfilesChanged)
        viewModel.userImageUrl.observe(viewLifecycleOwner, this::onUserImageChanged)
        viewModel.pendingInvitations.observe(viewLifecycleOwner, this::onGetPendingInvitations)
    }

    private fun setupAdapter() {
        val adapter = OrganizationsListAdapter()
        binding.rvSettingOrganizations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSettingOrganizations.adapter = adapter
    }

    private fun onUserDataChanged(userData: CustomUser) {
        DebugLog.d(TAG) { "on user data changed" }
        val initials = getString(
            R.string.user_name_initials,
            userData.name.take(1).uppercase(),
            userData.lastName.take(1).uppercase()
        )
        binding.tvSettingProfileName.text = initials
        binding.tvProfileName.text =
            getString(R.string.setting_display_name, userData.name, userData.lastName)
        binding.accMenuItemEmail.setValue(userData.email)
        binding.prefMenuItemName.setValue(getString(R.string.setting_display_name, userData.name, userData.lastName).trim())
        binding.prefMenuItemGender.setValue(userData.gender.raw)
        // The date as the phone's language writes it ("24 Nov 1992").
        binding.prefMenuItemBirth.setValue(
            userData.dateOfBirth.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        )
        val height = ProfileValues.height(userData.height)
        binding.prefMenuItemHeight.setValue(
            height?.let { getString(R.string.setting_label_user_height_text, it.toString()) } ?: ProfileValues.EMPTY,
            unset = height == null
        )
        val weight = ProfileValues.weightText(userData.weight)
        binding.prefMenuItemWeight.setValue(
            weight?.let { getString(R.string.setting_label_user_weight_text, it) } ?: ProfileValues.EMPTY,
            unset = weight == null
        )
        notificationsEnabledInProfile = userData.notifications["analytics"] ?: true
        refreshNotificationSwitch()
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed the permission in the system settings and come back.
        refreshNotificationSwitch()
    }

    private val notificationsAllowedBySystem: Boolean
        get() = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()

    /** On only when the profile wants notifications and the system lets the app show them. */
    private fun refreshNotificationSwitch() {
        if (view == null) return
        val allowed = notificationsAllowedBySystem
        binding.smNotiSwitch.getSwitchComp()?.isChecked = notificationsEnabledInProfile && allowed
        binding.llNotiBlocked.visibility = if (allowed) View.GONE else View.VISIBLE
    }

    private fun onNotificationsSwitched(isOn: Boolean) {
        if (!isOn || notificationsAllowedBySystem) {
            saveNotifications(isOn)
            return
        }
        // The system blocks notifications: the switch stays off until that changes.
        refreshNotificationSwitch()
        // Android 13+ can show the permission dialog, but stops doing so after the user has said
        // no twice (or once, if it was refused from the system settings); then only the system
        // settings can turn it back on.
        val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (!requireContext().notificationPermissionAsked ||
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
        if (canAsk) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showNotificationsBlockedDialog()
        }
    }

    /**
     * Saves whether the profile wants notifications. The switch moves at once; if the save fails it
     * goes back and the error is shown. The result is this screen's alone, so the PIN and password
     * screens never see it.
     */
    private fun saveNotifications(on: Boolean) {
        val before = notificationsEnabledInProfile
        notificationsEnabledInProfile = on
        refreshNotificationSwitch()
        viewLifecycleOwner.lifecycleScope.launch {
            val response = // Only this entry: other notification settings in the profile stay as they are.
            viewModel.saveProfile(mapOf("notifications.analytics" to on))
            if (!response.isSuccess) {
                notificationsEnabledInProfile = before
                refreshNotificationSwitch()
                showActionFailedSheet(response.error)
            }
        }
    }

    private fun showNotificationsBlockedDialog() {
        childFragmentManager.showKamleonSheet(
            SHEET_NOTIFICATIONS_BLOCKED,
            SheetSpec(
                message = getString(R.string.setting_noti_blocked_msg),
                primary = getString(R.string.setting_noti_open_settings),
                secondary = getString(R.string.dialog_deactive_n),
                dismissible = false
            )
        )
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        )
    }

    private fun onUserProfilesChanged(organizations: ArrayList<Organization>) {
        DebugLog.d(TAG) { "User profiles changed: count=${organizations.size}" }
        if (organizations.isEmpty()) {
            binding.llSettingAccountInfoSingle.visibility = View.GONE
            binding.llSettingAccountInfoMultiple.visibility = View.GONE
            return
        } else if (organizations.size == 1) {
            binding.llSettingAccountInfoMultiple.visibility = View.GONE
            binding.tvSettingAccountOrg.text = organizations[0].organizationName
            binding.tvSettingAccountCenter.text = organizations[0].centerName
            binding.tvSettingAccountTeam.text = organizations[0].teamName
            binding.llSettingAccountInfoSingle.visibility = View.VISIBLE
            return
        } else {
            binding.llSettingAccountInfoSingle.visibility = View.GONE
            val adapter = binding.rvSettingOrganizations.adapter as OrganizationsListAdapter
            adapter.submitList(organizations.toList())
            binding.llSettingAccountInfoMultiple.visibility = View.VISIBLE
        }
    }

    private fun onUserImageChanged(url: String?) {
        binding.ivSettProfile.showProfileImage(url, viewModel.imageLoader)
    }

    private fun onGetPendingInvitations(invitations: ArrayList<Invitation>) {
        if (invitations.isNotEmpty())
            binding.btnSettingNotiBadge.visibility = View.VISIBLE
        else binding.btnSettingNotiBadge.visibility = View.GONE
    }

    companion object {
        val TAG: String = SettingFragment::class.java.simpleName
        private const val SHEET_LOGOUT = "settings_logout"
        private const val PROFILE_SHEET = "settings_profile_sheet"
        private const val SHEET_NOTIFICATIONS_BLOCKED = "settings_notifications_blocked"
    }
}
