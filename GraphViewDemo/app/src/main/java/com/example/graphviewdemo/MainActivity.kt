package com.example.graphviewdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.graphviewdemo.customview.KamleonGraphView
import com.example.graphviewdemo.customview.KamleonUtils
import com.example.graphviewdemo.customview.data.KamleonGraphBarItemData
import com.example.graphviewdemo.customview.data.KamleonGraphDataType
import com.example.graphviewdemo.customview.data.KamleonGraphViewMode
import com.example.graphviewdemo.customview.exts.addDays
import com.example.graphviewdemo.customview.exts.addHours
import com.example.graphviewdemo.customview.exts.addMonths
import com.example.graphviewdemo.customview.exts.beginningOfDay
import com.example.graphviewdemo.customview.exts.beginningOfWeek
import com.example.graphviewdemo.customview.exts.beginningOfYear
import com.example.graphviewdemo.customview.exts.endOfMonth
import com.example.graphviewdemo.customview.exts.endOfWeek
import com.example.graphviewdemo.customview.exts.formatDate
import com.example.graphviewdemo.customview.exts.getWeekDay
import com.example.graphviewdemo.customview.exts.toDate
import java.util.Date
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    var graphView: KamleonGraphView? = null

    var typeLayout1: RelativeLayout? = null
    var typeText1: TextView? = null
    var typeLayout2: RelativeLayout? = null
    var typeText2: TextView? = null
    var typeLayout3: RelativeLayout? = null
    var typeText3: TextView? = null

    var activeType: KamleonGraphDataType = KamleonGraphDataType.hydration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initGraphTypeViews()

        graphView = findViewById(R.id.graphView)

        val date = KamleonUtils.dateFrom(2023, 10, 8)
        val yearData = yearDataSourceOf(date) // dailyDataSourceOf
        graphView?.setData(yearData)
    }

    private fun initGraphTypeViews() {
        typeLayout1 = findViewById(R.id.idLayoutGraphType1)
        typeText1 = findViewById(R.id.idGraphTypeText1)
        typeLayout2 = findViewById(R.id.idLayoutGraphType2)
        typeText2 = findViewById(R.id.idGraphTypeText2)
        typeLayout3 = findViewById(R.id.idLayoutGraphType3)
        typeText3 = findViewById(R.id.idGraphTypeText3)

        updateGraphType(activeType)
    }

    private fun updateGraphType(type: KamleonGraphDataType) {
        val arrViewModeLayouts = arrayOf(typeLayout1, typeLayout2, typeLayout3)
        val arrViewModeTvs = arrayOf(typeText1, typeText2, typeText3)
        val graphTypes = arrayOf(KamleonGraphDataType.hydration, KamleonGraphDataType.electrolytes, KamleonGraphDataType.volume)

        for ((index, element) in arrViewModeLayouts.withIndex()) {
            if (type == graphTypes[index]) {
                arrViewModeTvs[index]?.setTextColor(getColor(R.color.kmln_graph_header_item_text_active))
                element?.background = getDrawable(R.drawable.kmln_bg_graph_header_item)
                element?.setOnClickListener(null)
            } else {
                arrViewModeTvs[index]?.setTextColor(getColor(R.color.kmln_graph_header_item_text_normal))
                element?.background = null
                element?.setOnClickListener {
                    activeType = graphTypes[index]
                    updateGraphType(graphTypes[index])
                }
            }
        }

        graphView?.setGraphDataType(activeType)
    }

    private fun dailyDataSourceOf(date: Date) : ArrayList<KamleonGraphBarItemData> {
        var dataSource = ArrayList<KamleonGraphBarItemData>()
        val startDate = date.beginningOfDay
        for (i in 0..23 step 1) {
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