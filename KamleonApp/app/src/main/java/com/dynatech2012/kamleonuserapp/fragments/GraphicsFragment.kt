package com.dynatech2012.kamleonuserapp.fragments

import com.dynatech2012.kamleonuserapp.views.padForSystemBars
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.dynatech2012.kamleonuserapp.views.showProfileImage
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityGraphicsBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.models.CustomUser
import dagger.hilt.android.AndroidEntryPoint
import com.dynatech2012.kamleonuserapp.utils.DebugLog
@AndroidEntryPoint
class GraphicsFragment : BaseFragment<ActivityGraphicsBinding>() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun setBinding(): ActivityGraphicsBinding = ActivityGraphicsBinding.inflate(layoutInflater)

    override fun initView() {
        binding.root.padForSystemBars()
        /*
        val yearData = yearDataSourceOf(Date())
        binding.graphViewNew.setData(yearData)
         */

        // Read from the arguments, so the same graph comes back after the process is recreated.
        val graphType = graphTypeOf(arguments)
        binding.graphViewNew.setGraphDataType(graphType)

        binding.tvNavTitle.text = graphType.identifier

        initObservers()
    }

    override fun initEvent() {
        binding.cvGraphBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.cvGraphProfile.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.action_graphicsFragment_to_settingFragment)
        }
    }
    private fun initObservers() {
        viewModel.userData.observe(viewLifecycleOwner, this::onUserDataChanged)
        viewModel.measures.observe(viewLifecycleOwner, this::onGetMeasures)
        viewModel.averageMonthlyMeasures.observe(viewLifecycleOwner, this::onGetAverageMonthlyMeasures)
        viewModel.averageDailyMeasures.observe(viewLifecycleOwner, this::onGetAverageDailyMeasures)
        viewModel.userImageUrl.observe(viewLifecycleOwner, this::onUserImageChanged)
    }

    private fun onUserDataChanged(userData: CustomUser) {
        DebugLog.d(SettingFragment.TAG) { "on user data changed" }
        val initials = getString(R.string.user_name_initials, userData.name.take(1).uppercase(), userData.lastName.take(1).uppercase())
        binding.tvGraphicProfileName.text = initials
    }

    private fun onGetMeasures(measures: ArrayList<MeasureData>) {
        DebugLog.d(TAG) { "got measures count: ${measures.size}" }
        // Update the graph
        binding.graphViewNew.setMeasuresDataSource(measures)
    }

    private fun onGetAverageMonthlyMeasures(averageMonthlyMeasureData: ArrayList<AverageMonthlyMeasureData>) {
        DebugLog.d(TAG) { "got average monthly measures count: ${averageMonthlyMeasureData.size}" }
        binding.graphViewNew.setMonthlyDataSource(averageMonthlyMeasureData)
    }

    private fun onGetAverageDailyMeasures(averageDailyMeasureData: ArrayList<AverageDailyMeasureData>) {
        DebugLog.d(TAG) { "got average daily measures count: ${averageDailyMeasureData.size}" }
        // Update the graph
        binding.graphViewNew.setDailyDataSource(averageDailyMeasureData)
    }

    private fun onUserImageChanged(url: String?) {
        binding.ivGraphProfile.showProfileImage(url, viewModel.imageLoader)
    }
    /*
    private fun dailyDataSourceOf(date: Date) : ArrayList<KamleonGraphBarItemData> {
        val dataSource = ArrayList<KamleonGraphBarItemData>()
        val startDate = date.beginningOfDay

        val curDateTime = Date().time
        for (i in 0..23 step 1) {
            if (startDate.addHours(i).time > curDateTime) { break }

            var data : Double = (i + date.month + date.date + Random.nextInt(0, 15)).toDouble()
            if (date.month % 3 == 0) {
                data = (i + date.month * Random.nextInt(1, 3) + date.date * Random.nextInt(1, 3)).toDouble()
            } else if (date.month % 3 == 1) {
                data = (i * Random.nextInt(1, 3) + date.month * Random.nextInt(0, 2) + date.date * Random.nextInt(2, 4)).toDouble()
            }

            dataSource.add(KamleonGraphBarItemData(data % 90 + 10.0, startDate.addHours(i).time))
        }

        return dataSource
    }

    private fun monthDataSourceOf(date: Date) : ArrayList<KamleonGraphBarItemData> {
        Log.d("LLL", "addMonthDataSourceOf" + date.formatDate())
        val dataSource = ArrayList<KamleonGraphBarItemData>()
        val startDate = date.beginningOfDay

        for (i in 0..date.endOfMonth.day()) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            val dayDataSource = dailyDataSourceOf(startDate.addDays(i))
            dataSource.addAll(dayDataSource)
        }
        return dataSource
    }

    private fun yearDataSourceOf(date: Date) : ArrayList<KamleonGraphBarItemData> {
        val dataSource = ArrayList<KamleonGraphBarItemData>()
        val startDate = date.beginningOfYear

        for (i in 0..11) {
            val monthDataSource = monthDataSourceOf(startDate.addMonths(i))
            dataSource.addAll(monthDataSource)
        }
        return dataSource
    }
     */

    companion object {
        val TAG: String = GraphicsFragment::class.java.simpleName

        /** Navigation argument with the [KamleonGraphDataType] name of the graph to show. */
        const val ARG_GRAPH_TYPE = "graphType"

        /** The arguments that open the graph of [type]. */
        fun argsFor(type: KamleonGraphDataType): Bundle = bundleOf(ARG_GRAPH_TYPE to type.name)

        /** The graph [args] ask for; hydration when they name none or an unknown one. */
        fun graphTypeOf(args: Bundle?): KamleonGraphDataType =
            KamleonGraphDataType.entries.firstOrNull { it.name == args?.getString(ARG_GRAPH_TYPE) }
                ?: KamleonGraphDataType.hydration
        // const val EXTRA_KEY_TYPE = "ExtraKeyType"
    }
}