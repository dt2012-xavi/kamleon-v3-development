package com.dynatech2012.kamleonuserapp.fragments

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.databinding.ActivityGraphicsBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.graph.GraphScreen
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.home.HomeProfile
import dagger.hilt.android.AndroidEntryPoint

/** Hosts [GraphScreen] for the metric in its arguments and connects it to [MainViewModel]. */
@AndroidEntryPoint
class GraphicsFragment : BaseFragment<ActivityGraphicsBinding>() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun setBinding(): ActivityGraphicsBinding = ActivityGraphicsBinding.inflate(layoutInflater)

    override fun initView() {
        // Read from the arguments, so the same graph comes back after the process is recreated.
        val graphType = graphTypeOf(arguments)
        binding.cvGraphScreen.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.cvGraphScreen.setContent { GraphRoute(graphType) }
    }

    override fun initEvent() = Unit

    @Composable
    private fun GraphRoute(type: KamleonGraphDataType) {
        val user by viewModel.userData.observeAsState()
        val imageUrl by viewModel.userImageUrl.observeAsState()
        val measures by viewModel.measures.observeAsState()
        val daily by viewModel.averageDailyMeasures.observeAsState()
        val monthly by viewModel.averageMonthlyMeasures.observeAsState()
        GraphScreen(
            type = type,
            measures = measures.orEmpty(),
            daily = daily.orEmpty(),
            monthly = monthly.orEmpty(),
            profile = HomeProfile(
                initials = user?.let { getString(R.string.user_name_initials, it.name.take(1).uppercase(), it.lastName.take(1).uppercase()) },
                imageUrl = imageUrl,
                imageLoader = viewModel.imageLoader,
            ),
            onBack = { findNavController().popBackStack() },
            onProfileClick = ::openSettings,
        )
    }

    private fun openSettings() {
        val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navHostFragment.navController.navigate(R.id.action_graphicsFragment_to_settingFragment)
    }

    companion object {
        val TAG: String = GraphicsFragment::class.java.simpleName

        /** Navigation argument with the [KamleonGraphDataType] name of the graph to show. */
        const val ARG_GRAPH_TYPE = "graphType"

        /** The arguments that open the graph of [type]. */
        fun argsFor(type: KamleonGraphDataType): Bundle = Bundle().apply { putString(ARG_GRAPH_TYPE, type.name) }

        /** The graph [args] ask for; hydration when they name none or an unknown one. */
        fun graphTypeOf(args: Bundle?): KamleonGraphDataType =
            KamleonGraphDataType.entries.firstOrNull { it.name == args?.getString(ARG_GRAPH_TYPE) }
                ?: KamleonGraphDataType.hydration
    }
}
