package com.kamleonapp.views.chart.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.kamleonapp.R
import com.kamleonapp.views.chart.data.KamleonGraphBarDrawData
import com.kamleonapp.views.chart.data.KamleonGraphDataType
import com.kamleonapp.views.chart.data.KamleonGraphViewMode
import com.kamleonapp.views.chart.exts.formatDate
import java.util.Date

class KmlnHeaderView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    interface KmlnHeaderViewListener {
        fun onHeaderViewModeChanged(mode: KamleonGraphViewMode)
    }
    private var headerListener: KmlnHeaderViewListener? = null

    private var mode: KamleonGraphViewMode = KamleonGraphViewMode.Daily

    private var view : View = LayoutInflater.from(context).inflate(R.layout.layout_kmln_header, this, true)

    private var tvValueAvg: TextView? = null
    private var tvValueUnit: TextView? = null
    private var tvTimstamp: TextView? = null

    var optionViewDaily = view.findViewById(R.id.kmlnHeaderItemDaily) as KmlnOptionView
    var optionViewWeekly = view.findViewById(R.id.kmlnHeaderItemWeekly) as KmlnOptionView
    var optionViewMonthly = view.findViewById(R.id.kmlnHeaderItemMonthly) as KmlnOptionView
    var optionViewYearly = view.findViewById(R.id.kmlnHeaderItemYearly) as KmlnOptionView

//    private var layoutDaily: RelativeLayout =
//        view.findViewById(R.id.kmlnHeaderItemDaily) as RelativeLayout
//    private var tvDaily: TextView =
//        view.findViewById(R.id.kmlnHeaderItemDailyTextView) as TextView
//
//    private var layoutWeekly: RelativeLayout =
//        view.findViewById(R.id.kmlnHeaderItemWeekly) as RelativeLayout
//    private var tvWeekly: TextView =
//        view.findViewById(R.id.kmlnHeaderItemWeeklyTextView) as TextView
//
//    private var layoutMonthly: RelativeLayout =
//        view.findViewById(R.id.kmlnHeaderItemMonthly) as RelativeLayout
//    private var tvMonthly: TextView =
//        view.findViewById(R.id.kmlnHeaderItemMonthlyTextView) as TextView
//
//    private var layoutYearly: RelativeLayout =
//        view.findViewById(R.id.kmlnHeaderItemYearly) as RelativeLayout
//    private var tvYearly: TextView =
//        view.findViewById(R.id.kmlnHeaderItemYearlyTextView) as TextView

    init {
        tvValueAvg =  view.findViewById(R.id.textViewAvgValue) as TextView
        tvValueUnit =  view.findViewById(R.id.textViewAvgUnit) as TextView
        tvTimstamp =  view.findViewById(R.id.textViewAvgTimestamp) as TextView

        initModeViews()
        updateViewModeUI(mode)
    }


    private fun initModeViews() {
        val arrOptionViews = modeOptionViews()
        for ((index, element) in arrOptionViews.withIndex()) {
            element.setOptionText(KamleonGraphViewMode.viewMode(index).displayName())
            element.setSelection(mode == KamleonGraphViewMode.viewMode(index))
            element.listener = object : KmlnOptionView.KmlnOptionViewListener {
                override fun onOptionSelected(optionView: KmlnOptionView) {
                    val arrOptionViews = modeOptionViews()
                    arrOptionViews.indexOf(optionView)?.let {
                        if (it >= 0) {
                            viewModeSelected(KamleonGraphViewMode.viewMode(it))
                        }
                    }
                }
            }
        }
    }

    private fun modeOptionViews() : Array<KmlnOptionView> {
        return arrayOf(optionViewDaily, optionViewWeekly, optionViewMonthly, optionViewYearly)
    }

    fun setListener(listener: KmlnHeaderViewListener) {
        headerListener = listener
    }

    fun setData(data: KamleonGraphBarDrawData) {
        if (data.values.isEmpty()) {
            return
        }

        val nonZeroVals = data.values.filter { it.y > 0 }
        val avgValue = nonZeroVals.sumOf { it.y } / nonZeroVals.size
        var strAvg = ""
        val strAvgUnit = data.type.getUnit()
        when (data.type) {
            KamleonGraphDataType.hydration -> {
                strAvg = String.format("%d", avgValue.toInt())
            }

            KamleonGraphDataType.electrolytes -> {
                strAvg = String.format("%.1f", avgValue)
            }

            KamleonGraphDataType.volume -> {
                strAvg = String.format("%d", avgValue.toInt())
            }
        }
        val strDateRange = when(data.mode) {
            KamleonGraphViewMode.Daily -> data.date.formatDate("MMM, dd, yyyy")
            KamleonGraphViewMode.Weekly -> data.startDate().formatDate("MMM, dd, yyyy") + " - " + data.endDate().formatDate("MMM, dd, yyyy")
            KamleonGraphViewMode.Monthly -> data.date.formatDate("MMM, yyyy")
            KamleonGraphViewMode.Yearly -> data.date.formatDate("yyyy")
        }

        setVisualData(strAvg, strAvgUnit, strDateRange)
    }

    private fun setVisualData(avgValue: String, avgUnit: String, dateRange: String) {
        tvValueAvg?.text = avgValue
        tvValueUnit?.text = avgUnit
        tvTimstamp?.text = dateRange
    }

    private fun viewModeSelected(newMode: KamleonGraphViewMode) {
        mode = newMode
        headerListener?.onHeaderViewModeChanged(newMode)
        updateViewModeUI(newMode)
    }

    private fun updateViewModeUI(activeMode: KamleonGraphViewMode) {
        val arrModeOptions = modeOptionViews()

        for ((index, element) in arrModeOptions.withIndex()) {
            element.setSelection(activeMode == KamleonGraphViewMode.viewMode(index))
        }
    }
}