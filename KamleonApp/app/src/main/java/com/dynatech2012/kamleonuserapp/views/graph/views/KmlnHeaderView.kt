package com.dynatech2012.kamleonuserapp.views.graph.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphBarDrawData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphViewMode
import com.dynatech2012.kamleonuserapp.extensions.formatDate
import kotlinx.coroutines.DEBUG_PROPERTY_NAME

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

    private var optionViewDaily = view.findViewById(R.id.kmlnHeaderItemDaily) as KmlnOptionView
    private var optionViewWeekly = view.findViewById(R.id.kmlnHeaderItemWeekly) as KmlnOptionView
    private var optionViewMonthly = view.findViewById(R.id.kmlnHeaderItemMonthly) as KmlnOptionView
    private var optionViewYearly = view.findViewById(R.id.kmlnHeaderItemYearly) as KmlnOptionView

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
            val header = context.getString(KamleonGraphViewMode.viewMode(index).displayName())
            element.setOptionText(header)
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
        Log.d("KmlnHeaderView", "setData size: ${data.values.size}")
        if (data.values.isEmpty()) {
            return
        }

        val nonZeroVals = data.values.filter { it.y > 0 }
        val avgValue = nonZeroVals.sumOf { it.y } / nonZeroVals.size
        val strAvgUnit = context.getString(data.type.getUnit())
        if (nonZeroVals.isEmpty()) {
            Log.d("KmlnHeaderView", "setData: nonZeroVals.isEmpty()")
            val strDateRange = when(data.mode) {
                KamleonGraphViewMode.Daily -> data.date.formatDate("dd MMM, yyyy")
                KamleonGraphViewMode.Weekly -> data.startDate().formatDate("dd MMM, yyyy") + " - " + data.endDate().formatDate("dd MMM, yyyy")
                KamleonGraphViewMode.Monthly -> data.startDate().formatDate("MMM") + " - " + data.endDate().formatDate("MMM")
                KamleonGraphViewMode.Yearly -> data.startDate().formatDate("yyyy") + " - " + data.endDate().formatDate("yyyy")
            }
            setVisualData("--", strAvgUnit, strDateRange)
            return
        }
        val strAvg: String = when (data.type) {
            KamleonGraphDataType.hydration -> {
                String.format("%d", avgValue.toInt())
            }

            KamleonGraphDataType.electrolytes -> {
                String.format("%.1f", avgValue)
            }

            KamleonGraphDataType.volume -> {
                String.format("%d", avgValue.toInt())
            }
        }
        val strDateRange = when(data.mode) {
            KamleonGraphViewMode.Daily -> data.date.formatDate("dd MMM, yyyy")
            KamleonGraphViewMode.Weekly -> data.startDate().formatDate("dd MMM, yyyy") + " - " + data.endDate().formatDate("dd MMM, yyyy")
            KamleonGraphViewMode.Monthly -> data.startDate().formatDate("MMM") + " - " + data.endDate().formatDate("MMM")
            KamleonGraphViewMode.Yearly -> data.startDate().formatDate("yyyy") + " - " + data.endDate().formatDate("yyyy")
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