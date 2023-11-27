package com.dynatech2012.kamleonuserapp.fragments

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.adapters.TipListAdapter
import com.dynatech2012.kamleonuserapp.base.BaseFragment
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.databinding.ActivityGraphicsBinding
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.CirclePagerIndicatorDecoration
import com.dynatech2012.kamleonuserapp.views.cards.TipType
import com.dynatech2012.kamleonuserapp.views.cards.ViewPager
import com.dynatech2012.kamleonuserapp.views.chart.data.KamleonGraphBarItemData
import com.dynatech2012.kamleonuserapp.views.chart.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.chart.exts.addDays
import com.dynatech2012.kamleonuserapp.views.chart.exts.addHours
import com.dynatech2012.kamleonuserapp.views.chart.exts.addMonths
import com.dynatech2012.kamleonuserapp.views.chart.exts.beginningOfDay
import com.dynatech2012.kamleonuserapp.views.chart.exts.beginningOfYear
import com.dynatech2012.kamleonuserapp.views.chart.exts.day
import com.dynatech2012.kamleonuserapp.views.chart.exts.endOfMonth
import com.dynatech2012.kamleonuserapp.views.chart.exts.formatDate
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date
import kotlin.random.Random
@AndroidEntryPoint
class GraphicsFragment : BaseFragment<ActivityGraphicsBinding>() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun setBinding(): ActivityGraphicsBinding = ActivityGraphicsBinding.inflate(layoutInflater)

    private lateinit var adapter: TipListAdapter
    private var tipsAry: Array<TipListAdapter.TipListItemModel> = arrayOf(
        TipListAdapter.TipListItemModel("Healthy Diet", "The full spectrum of diet healthiness relies significantly on the inclusion of the water we ingest."),
        TipListAdapter.TipListItemModel("Fitness workout", "The full spectrum of diet healthiness relies significantly on the inclusion of the water we ingest.")
    )
//    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
//        override fun onPageSelected(position: Int) {
//            binding.pgTipLayout.setActiveIndex(position)
//        }
//    }

    private val graphTypes = arrayOf(KamleonGraphDataType.hydration, KamleonGraphDataType.electrolytes, KamleonGraphDataType.volume)

    override fun initView() {
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = RecyclerView.HORIZONTAL
        adapter = TipListAdapter(tipsAry)
        binding.rcvListNew.adapter = adapter
//        binding.rcvList.orientation = ViewPager2.ORIENTATION_HORIZONTAL
//        binding.rcvList.registerOnPageChangeCallback(onPageChangeCallback)
        binding.rcvListNew.layoutManager = layoutManager
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rcvListNew)

        binding.rcvListNew.addItemDecoration(CirclePagerIndicatorDecoration())

        val yearData = yearDataSourceOf(Date())
        binding.graphViewNew.setData(yearData)

        val typeFromIntent = viewModel.graphicType
        binding.graphViewNew.setGraphDataType(graphTypes[typeFromIntent])

        binding.tvNavTitle.text = graphTypes[typeFromIntent].identifier

        // TODO: uncomment when cards replaced
        // setupTips()
        initObservers()
    }

    private fun setupTips() {
        val tipType = when (viewModel.graphicType) {
            0 -> TipType.HYDRATION
            1 -> TipType.ELECTROLYTE
            2 -> TipType.VOLUME
            else -> TipType.HOME
        }
        binding.cvGraphTips.setContent {
            ViewPager(tipType = tipType, modifier = Modifier) {
                findNavController().navigate(R.id.action_homeFragment_to_analyticFragment)
            }
        }
    }

    override fun initEvent() {
        binding.navBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }
    private fun initObservers() {
        /*
        viewModel.measures.observe(this, this::onGetMeasures)
        viewModel.averageMonthlyMeasures.observe(this, this::onGetAverageMonthlyMeasures)
        viewModel.averageDailyMeasures.observe(this, this::onGetAverageDailyMeasures)
        */
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
        Log.e("LLL", "addMonthDataSourceOf" + date.formatDate())
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

    companion object {
        val TAG: String = GraphicsFragment::class.java.simpleName
        // const val EXTRA_KEY_TYPE = "ExtraKeyType"
    }
}