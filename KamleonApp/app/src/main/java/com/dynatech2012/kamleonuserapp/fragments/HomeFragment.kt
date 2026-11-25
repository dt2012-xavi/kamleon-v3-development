package com.dynatech2012.kamleonuserapp.fragments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.InvitationsSheet
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityHomeBinding
import com.dynatech2012.kamleonuserapp.models.HomeStatus
import com.dynatech2012.kamleonuserapp.models.homeContentOf
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.home.HomeProfile
import com.dynatech2012.kamleonuserapp.views.home.HomeScreen
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import dagger.hilt.android.AndroidEntryPoint

/** Hosts [HomeScreen] and connects it to [MainViewModel]. */
@AndroidEntryPoint
class HomeFragment : BaseFragment<ActivityHomeBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    override fun initView() {
        binding.cvHomeScreen.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.cvHomeScreen.setContent { HomeRoute() }
        viewModel.getUserData()
        viewModel.refreshInvitations()
    }

    override fun initEvent() = Unit

    @Composable
    private fun HomeRoute() {
        val user by viewModel.userData.observeAsState()
        val imageUrl by viewModel.userImageUrl.observeAsState()
        val lastMeasure by viewModel.lastMeasure.observeAsState()
        val status by viewModel.homeStatus.observeAsState(HomeStatus.Loading)
        val unreadInvitations by viewModel.hasUnreadInvitations.observeAsState(false)

        val profile = HomeProfile(
            initials = user?.let { getString(R.string.user_name_initials, it.name.take(1).uppercase(), it.lastName.take(1).uppercase()) },
            imageUrl = imageUrl,
            imageLoader = viewModel.imageLoader,
        )
        HomeScreen(
            content = homeContentOf(lastMeasure, status),
            profile = profile,
            hasNotifications = unreadInvitations,
            onProfileClick = ::openSettings,
            onNotificationsClick = ::openInvitations,
            onScan = { (parentFragment?.parentFragment as? TabFragment)?.openScanner() },
            onRetry = viewModel::retryUserMeasures,
        )
    }

    /** The same invitations sheet the settings open. */
    private fun openInvitations() {
        if (childFragmentManager.findFragmentByTag(InvitationsSheet.TAG) != null) return
        InvitationsSheet().show(childFragmentManager, InvitationsSheet.TAG)
    }

    private fun openSettings() {
        DebugLog.d(TAG) { "settings clicked" }
        val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navHostFragment.navController.navigate(R.id.action_tabFragment_to_settingFragment)
    }

    companion object {
        val TAG: String = HomeFragment::class.java.simpleName
    }
}
