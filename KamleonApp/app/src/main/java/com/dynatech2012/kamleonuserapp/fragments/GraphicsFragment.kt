package com.dynatech2012.kamleonuserapp.fragments

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.compose.ui.Modifier
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityGraphicsBinding
import com.dynatech2012.kamleonuserapp.models.RecommendationType
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.cards.ViewPager
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.models.CustomUser
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class GraphicsFragment : BaseFragment<ActivityGraphicsBinding>() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun setBinding(): ActivityGraphicsBinding = ActivityGraphicsBinding.inflate(layoutInflater)

    private val graphTypes = arrayOf(KamleonGraphDataType.hydration, KamleonGraphDataType.electrolytes, KamleonGraphDataType.volume)

    override fun initView() {
        /*
        val yearData = yearDataSourceOf(Date())
        binding.graphViewNew.setData(yearData)
         */

        val typeFromIntent = viewModel.graphicType
        binding.graphViewNew.setGraphDataType(graphTypes[typeFromIntent])

        binding.tvNavTitle.text = graphTypes[typeFromIntent].identifier

        setupTips()
        initObservers()
    }

    private fun setupTips() {
        val tipType = when (viewModel.graphicType) {
            0 -> RecommendationType.HYDRATION
            1 -> RecommendationType.ELECTROLYTE
            2 -> RecommendationType.VOLUME
            else -> RecommendationType.HOME
        }
        binding.cvGraphTips.setContent {
            ViewPager(recommendationType = tipType, null, modifier = Modifier)
        }
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
        viewModel.userData.observe(this, this::onUserDataChanged)
        viewModel.measures.observe(this, this::onGetMeasures)
        viewModel.averageMonthlyMeasures.observe(this, this::onGetAverageMonthlyMeasures)
        viewModel.averageDailyMeasures.observe(this, this::onGetAverageDailyMeasures)
        viewModel.userImageDrawable.observe(this, this::onUserImageChanged)
    }

    private fun onUserDataChanged(userData: CustomUser) {
        Log.d(SettingFragment.TAG, "on user data changed")
        val initials = getString(R.string.user_name_initials, userData.name.substring(0, 1).uppercase(), userData.lastName.substring(0, 1).uppercase())
        binding.tvGraphicProfileName.text = initials
    }

    private fun onGetMeasures(measures: ArrayList<MeasureData>) {
        Log.d(TAG, "got measures count: ${measures.size}")
        // Update the graph
        binding.graphViewNew.setMeasuresDataSource(measures)
    }

    private fun onGetAverageMonthlyMeasures(averageMonthlyMeasureData: ArrayList<AverageMonthlyMeasureData>) {
        Log.d(TAG, "got average monthly measures count: ${averageMonthlyMeasureData.size}")
        binding.graphViewNew.setMonthlyDataSource(averageMonthlyMeasureData)
    }

    private fun onGetAverageDailyMeasures(averageDailyMeasureData: ArrayList<AverageDailyMeasureData>) {
        Log.d(TAG, "got average daily measures count: ${averageDailyMeasureData.size}")
        // Update the graph
        binding.graphViewNew.setDailyDataSource(averageDailyMeasureData)
    }

    private fun onUserImageChanged(drawable: Drawable?) {
        Log.d(HomeFragment.TAG, "image changed")
        if (drawable == null) {
            binding.ivGraphProfile.setImageDrawable(null)
            binding.ivGraphProfile.visibility = View.INVISIBLE
            return
        }
        binding.ivGraphProfile.load(drawable)
        binding.ivGraphProfile.visibility = android.view.View.VISIBLE
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
        // const val EXTRA_KEY_TYPE = "ExtraKeyType"
    }
}