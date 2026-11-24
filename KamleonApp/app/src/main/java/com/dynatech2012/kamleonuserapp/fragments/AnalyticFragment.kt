package com.dynatech2012.kamleonuserapp.fragments

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityAnalyticBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.analytics.AnalyticsScreen
import com.dynatech2012.kamleonuserapp.views.cards.AnalyticType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.home.HomeProfile
import dagger.hilt.android.AndroidEntryPoint

/** Hosts [AnalyticsScreen] and connects it to [MainViewModel]. */
@AndroidEntryPoint
class AnalyticFragment : BaseFragment<ActivityAnalyticBinding>() {
    private val viewModel: MainViewModel by activityViewModels()
    override fun setBinding(): ActivityAnalyticBinding = ActivityAnalyticBinding.inflate(layoutInflater)

    override fun initView() {
        binding.cvAnalyticScreen.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.cvAnalyticScreen.setContent { AnalyticsRoute() }
        viewModel.getUserData()
    }

    override fun initEvent() = Unit

    @Composable
    private fun AnalyticsRoute() {
        val user by viewModel.userData.observeAsState()
        val imageUrl by viewModel.userImageUrl.observeAsState()
        val lastMeasure by viewModel.lastMeasure.observeAsState()
        AnalyticsScreen(
            measure = lastMeasure,
            profile = HomeProfile(
                initials = user?.let { getString(R.string.user_name_initials, it.name.take(1).uppercase(), it.lastName.take(1).uppercase()) },
                imageUrl = imageUrl,
                imageLoader = viewModel.imageLoader,
            ),
            onProfileClick = { navigate(R.id.action_tabFragment_to_settingFragment) },
            onOpenGraph = ::openGraph,
        )
    }

    private fun openGraph(type: AnalyticType) {
        val graphType = when (type) {
            AnalyticType.HYDRATION -> KamleonGraphDataType.hydration
            AnalyticType.ELECTROLYTE -> KamleonGraphDataType.electrolytes
            AnalyticType.VOLUME -> KamleonGraphDataType.volume
        }
        navigate(R.id.action_tabFragment_to_graphicsFragment, GraphicsFragment.argsFor(graphType))
    }

    private fun navigate(action: Int, args: Bundle? = null) {
        val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navHostFragment.navController.navigate(action, args)
    }
}
