package com.example.graphviewdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
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
import com.example.graphviewdemo.customview.views.KmlnOptionView
import java.util.Date
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    var graphView: KamleonGraphView? = null

    var optionView1: KmlnOptionView? = null
    var optionView2: KmlnOptionView? = null
    var optionView3: KmlnOptionView? = null

    private val graphTypes = arrayOf(KamleonGraphDataType.hydration, KamleonGraphDataType.electrolytes, KamleonGraphDataType.volume)

    private val optListener = object : KmlnOptionView.KmlnOptionViewListener {
        override fun onOptionSelected(optionView: KmlnOptionView) {
            val arrOptionViews = arrayOf(optionView1, optionView2, optionView3)
            arrOptionViews.indexOf(optionView)?.let {
                if (it >= 0 && it < graphTypes.size) {
                    updateGraphType(graphTypes[it])
                }
            }
        }
    }

    var activeType: KamleonGraphDataType = KamleonGraphDataType.hydration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initGraphTypeViews()

        graphView = findViewById(R.id.graphView)

//        val date = KamleonUtils.dateFrom(2023, 10, 8)
        val yearData = yearDataSourceOf(Date())
        graphView?.setData(yearData)
    }

    private fun initGraphTypeViews() {
        optionView1 = findViewById(R.id.optionType1)
        optionView2 = findViewById(R.id.optionType2)
        optionView3 = findViewById(R.id.optionType3)

        optionView1?.setOptionText(KamleonGraphDataType.hydration.identifier)
        optionView2?.setOptionText(KamleonGraphDataType.electrolytes.identifier)
        optionView3?.setOptionText(KamleonGraphDataType.volume.identifier)

        optionView1?.listener = optListener
        optionView2?.listener = optListener
        optionView3?.listener = optListener

        updateGraphType(activeType)
    }

    private fun updateGraphType(type: KamleonGraphDataType) {
        val arrOptionViews = arrayOf(optionView1, optionView2, optionView3)
        for ((index, element) in arrOptionViews.withIndex()) {
            element?.setSelection(type == graphTypes[index])
        }

        graphView?.setGraphDataType(type)
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