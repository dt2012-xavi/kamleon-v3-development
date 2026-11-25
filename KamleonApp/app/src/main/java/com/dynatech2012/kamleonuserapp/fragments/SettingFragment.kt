package com.dynatech2012.kamleonuserapp.fragments

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.graphics.Color
import android.view.View
import androidx.annotation.StringRes
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.views.showActionFailedSheet
import com.dynatech2012.kamleonuserapp.BuildConfig
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.home.NotificationsButton
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import com.dynatech2012.kamleonuserapp.views.InvitationsSheet
import com.dynatech2012.kamleonuserapp.activities.InitActivity
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.databinding.ActivitySettingBinding
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.utils.notificationPermissionAsked
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.SettingMenuItemView
import com.dynatech2012.kamleonuserapp.views.SheetResult
import com.dynatech2012.kamleonuserapp.views.SheetSpec
import com.dynatech2012.kamleonuserapp.views.onKamleonSheetResult
import com.dynatech2012.kamleonuserapp.views.showKamleonSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.dynatech2012.kamleonuserapp.views.showProfileImage
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.views.settings.ProfileField
import com.dynatech2012.kamleonuserapp.views.settings.PasswordSheet
import com.dynatech2012.kamleonuserapp.views.settings.UserRolesSheet
import com.dynatech2012.kamleonuserapp.views.settings.contactSupport
import com.dynatech2012.kamleonuserapp.views.settings.openInAppBrowser
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
        binding.tvSettingVersion.text = getString(R.string.setting_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        binding.cvSettingBell.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.cvSettingBell.setContent {
            val unread by viewModel.hasUnreadInvitations.observeAsState(false)
            NotificationsButton(hasNotifications = unread, onClick = ::openInvitations)
        }

        val menuItems: Array<SettingMenuItemView> = arrayOf(
            binding.accMenuItemEmail,
            binding.accMenuItemPwd,
            binding.accMenuItemPin,
            binding.accMenuItemRoles,
            binding.accMenuItemContact,
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
        initObservers()
        onKamleonSheetResult(SHEET_LOGOUT) { if (it == SheetResult.PRIMARY) logout() }
        onKamleonSheetResult(SHEET_NOTIFICATIONS_BLOCKED) { if (it == SheetResult.PRIMARY) openNotificationSettings() }
        childFragmentManager.setFragmentResultListener(ProfileFieldSheet.RESULT_SAVED, viewLifecycleOwner) { _, result ->
            showMessage(result.getInt(ProfileFieldSheet.RESULT_MESSAGE, R.string.dialog_saved_text))
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

        binding.btnSettingClose.setOnClickListener {
            //val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            //val navController = navHostFragment.navController
            findNavController().popBackStack()
            //findNavController().navigateUp()
            //activity?.finish()
        }

        binding.tvNotiOpenSettings.setOnClickListener { openNotificationSettings() }

        // The deletion request is made on the web.
        binding.tvDelete.setOnClickListener { openWebPage(UrlConstants.URL_DELETE) }
    }

    private fun openInvitations() {
        if (childFragmentManager.findFragmentByTag(InvitationsSheet.TAG) != null) return
        InvitationsSheet().show(childFragmentManager, InvitationsSheet.TAG)
    }

    private fun openWebPage(url: String) {
        if (!requireContext().openInAppBrowser(url)) showMessage(R.string.error_no_browser)
    }

    private fun showMessage(@StringRes message: Int) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showLogoutDialog() {
        childFragmentManager.showKamleonSheet(
            SHEET_LOGOUT,
            SheetSpec(
                title = getString(R.string.dialog_logout_title),
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
            binding.accMenuItemPwd -> showSheet { PasswordSheet() }
            binding.accMenuItemPin -> showProfileSheet(ProfileField.PIN)
            binding.accMenuItemRoles -> showSheet { UserRolesSheet() }

            binding.accMenuItemContact -> {
                if (!requireContext().contactSupport()) showMessage(R.string.help_error_mail)
            }
            binding.accMenuItemDSP -> openWebPage(UrlConstants.URL_CONSENT)
            binding.accMenuItemTOS -> openWebPage(UrlConstants.URL_TERMS)
            binding.accMenuItemPP -> openWebPage(UrlConstants.URL_POLICY_APP)

            binding.prefMenuItemName -> showProfileSheet(ProfileField.NAME)
            binding.prefMenuItemGender -> showProfileSheet(ProfileField.GENDER)
            binding.prefMenuItemBirth -> showProfileSheet(ProfileField.BIRTH)
            binding.prefMenuItemHeight -> showProfileSheet(ProfileField.HEIGHT)
            binding.prefMenuItemWeight -> showProfileSheet(ProfileField.WEIGHT)
        }
    }

    private fun showProfileSheet(field: ProfileField) {
        if (viewModel.userData.value == null) return
        showSheet { ProfileFieldSheet.newInstance(field) }
    }

    /** One Settings sheet at a time: a second tap while one is opening does nothing. */
    private fun showSheet(create: () -> BottomSheetDialogFragment) {
        if (childFragmentManager.findFragmentByTag(PROFILE_SHEET) != null) return
        create().show(childFragmentManager, PROFILE_SHEET)
    }

    private fun initObservers() {
        viewModel.userData.observe(viewLifecycleOwner, this::onUserDataChanged)
        viewModel.userImageUrl.observe(viewLifecycleOwner, this::onUserImageChanged)
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

    private fun onUserImageChanged(url: String?) {
        binding.ivSettProfile.showProfileImage(url, viewModel.imageLoader)
    }

    companion object {
        val TAG: String = SettingFragment::class.java.simpleName
        private const val SHEET_LOGOUT = "settings_logout"
        private const val PROFILE_SHEET = "settings_profile_sheet"
        private const val SHEET_NOTIFICATIONS_BLOCKED = "settings_notifications_blocked"
    }
}
