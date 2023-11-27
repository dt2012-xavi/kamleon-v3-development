package com.dynatech2012.kamleonuserapp.activities

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.adapters.TipListAdapter
import com.dynatech2012.kamleonuserapp.base.BaseActivity
import com.dynatech2012.kamleonuserapp.databinding.ActivityGraphicsBinding
import com.dynatech2012.kamleonuserapp.views.CirclePagerIndicatorDecoration
import com.dynatech2012.kamleonuserapp.views.chart.data.KamleonGraphBarItemData
import com.dynatech2012.kamleonuserapp.views.chart.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.chart.exts.addDays
import com.dynatech2012.kamleonuserapp.views.chart.exts.addHours
import com.dynatech2012.kamleonuserapp.views.chart.exts.addMonths
import com.dynatech2012.kamleonuserapp.views.chart.exts.beginningOfDay
import com.dynatech2012.kamleonuserapp.views.chart.exts.beginningOfYear
import com.dynatech2012.kamleonuserapp.views.chart.exts.endOfMonth
import com.dynatech2012.kamleonuserapp.views.chart.exts.formatDate
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date
import kotlin.random.Random
@AndroidEntryPoint
class GraphicsActivity : BaseActivity<ActivityGraphicsBinding>() {
    override fun setBinding(): ActivityGraphicsBinding = ActivityGraphicsBinding.inflate(layoutInflater)

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
        //binding.graphView.setData(yearData)

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
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
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