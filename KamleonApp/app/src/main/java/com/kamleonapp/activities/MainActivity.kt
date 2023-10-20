package com.kamleonapp.activities

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kamleonapp.adapters.TipListAdapter
import com.kamleonapp.base.BaseActivity
import com.kamleonapp.databinding.ActivityMainBinding
import com.kamleonapp.views.CirclePagerIndicatorDecoration
import com.kamleonapp.views.chart.data.KamleonGraphBarItemData
import com.kamleonapp.views.chart.data.KamleonGraphDataType
import com.kamleonapp.views.chart.exts.addDays
import com.kamleonapp.views.chart.exts.addHours
import com.kamleonapp.views.chart.exts.addMonths
import com.kamleonapp.views.chart.exts.beginningOfDay
import com.kamleonapp.views.chart.exts.beginningOfYear
import com.kamleonapp.views.chart.exts.endOfMonth
import com.kamleonapp.views.chart.exts.formatDate
import java.util.Date
import kotlin.random.Random

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun setBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    companion object {
        const val EXTRA_KEY_TYPE = "ExtraKeyType"
    }

    lateinit var adapter: TipListAdapter
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
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        adapter = TipListAdapter(tipsAry)
        binding.rcvList.adapter = adapter
//        binding.rcvList.orientation = ViewPager2.ORIENTATION_HORIZONTAL
//        binding.rcvList.registerOnPageChangeCallback(onPageChangeCallback)
        binding.rcvList.layoutManager = layoutManager
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rcvList)

        binding.rcvList.addItemDecoration(CirclePagerIndicatorDecoration())

        val yearData = yearDataSourceOf(Date())
        binding.graphView.setData(yearData)

        val typeFromIntent = intent.getIntExtra(EXTRA_KEY_TYPE, 0)
        binding.graphView.setGraphDataType(graphTypes[typeFromIntent])

        binding.tvNavTitle.text = graphTypes[typeFromIntent].identifier
    }

    override fun initEvent() {
        binding.navBack.setOnClickListener {
            finish()
        }
    }

    private fun dailyDataSourceOf(date: Date) : ArrayList<KamleonGraphBarItemData> {
        var dataSource = ArrayList<KamleonGraphBarItemData>()
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
        var dataSource = ArrayList<KamleonGraphBarItemData>()
        val startDate = date.beginningOfDay

        for (i in 0..date.endOfMonth.date) {
            val dayDataSource = dailyDataSourceOf(startDate.addDays(i))
            dataSource.addAll(dayDataSource)
        }
        return dataSource
    }

    private fun yearDataSourceOf(date: Date) : ArrayList<KamleonGraphBarItemData> {
        var dataSource = ArrayList<KamleonGraphBarItemData>()
        val startDate = date.beginningOfYear

        for (i in 0..11) {
            val monthDataSource = monthDataSourceOf(startDate.addMonths(i))
            dataSource.addAll(monthDataSource)
        }
        return dataSource
    }
}