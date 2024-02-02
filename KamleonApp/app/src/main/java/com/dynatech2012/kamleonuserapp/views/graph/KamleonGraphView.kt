package com.dynatech2012.kamleonuserapp.views.graph

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.SizeF
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnDragListener
import android.view.View.OnLongClickListener
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphAxisLabelItem
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphBarDrawData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataXY
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphViewMode
import com.dynatech2012.kamleonuserapp.extensions.addDays
import com.dynatech2012.kamleonuserapp.extensions.addHours
import com.dynatech2012.kamleonuserapp.extensions.addMinutes
import com.dynatech2012.kamleonuserapp.extensions.addMonths
import com.dynatech2012.kamleonuserapp.extensions.addSeconds
import com.dynatech2012.kamleonuserapp.extensions.beginningOfDay
import com.dynatech2012.kamleonuserapp.extensions.beginningOfMonth
import com.dynatech2012.kamleonuserapp.extensions.endOfDay
import com.dynatech2012.kamleonuserapp.extensions.endOfMonth
import com.dynatech2012.kamleonuserapp.extensions.formatDate
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.views.graph.views.KmlnFooterView
import com.dynatech2012.kamleonuserapp.views.graph.views.KmlnGraphView
import com.dynatech2012.kamleonuserapp.views.graph.views.KmlnHeaderView
import com.dynatech2012.kamleonuserapp.views.graph.views.KmlnLabelView
import java.util.Date

class KamleonGraphView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs),
    KmlnHeaderView.KmlnHeaderViewListener, KmlnFooterView.KmlnFooterViewListener, OnDragListener, OnLongClickListener {
    private var view : View = LayoutInflater.from(context).inflate(R.layout.layout_kamleon_graph_view, this, true)
    private var headerView = view.findViewById(R.id.graphViewHeader) as KmlnHeaderView
    private var footerView = view.findViewById(R.id.graphViewFooter) as KmlnFooterView

    private var contentLayout = view.findViewById(R.id.contentContainer) as FrameLayout
    private var contentView = view.findViewById(R.id.graphViewContent) as KmlnGraphView
    private var labelView = view.findViewById(R.id.graphLabelView) as KmlnLabelView

    private var listener: KamleonGraphViewListener? = null

    private var viewMode: KamleonGraphViewMode = KamleonGraphViewMode.Daily
    private var dataType: KamleonGraphDataType = KamleonGraphDataType.hydration

    init {
        headerView.setListener(this)
        footerView.setListener(this)

        labelView.visibility = INVISIBLE

        contentLayout.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//                when (event?.action) {
//                    MotionEvent.ACTION_DOWN ->
//                }
                val xPos = (event?.x ?: 0) as Float
                val yPos = (event?.y ?: 0) as Float
                PointF(xPos, yPos)
                Log.d(TAG, "Event:($xPos:$yPos)" + event?.action)
                handleTouchPos(PointF(xPos, yPos))
                return true
            }
        })
    }

    private var dataSourceNew = ArrayList<AveragesData>()
    private var measuresDataSource = ArrayList<MeasureData>()
    private var dailyDataSource = ArrayList<AverageDailyMeasureData>()
    private var monthlyDataSource = ArrayList<AverageMonthlyMeasureData>()


    /*
    private var dataSource = ArrayList<KamleonGraphBarItemData>()
    fun setData(data: ArrayList<KamleonGraphBarItemData>) {
        dataSource = data
        refreshGraphView()
    }
     */


    fun setMeasuresDataSource(data: ArrayList<MeasureData>) {
        measuresDataSource = data

        /*
        val fakeData = MeasureData()
        val date = Date().addDays(-6)
        Log.d(TAG, "Date measure 1: ${date.formatDate()}")
        fakeData.timestamp = Date().time
        fakeData.score = 90
        fakeData.msCond = 12f
        fakeData.vol = 88f
        //fakeData.precision = MeasurePrecision.Bad
        val fakeData2 = MeasureData()
        fakeData2.timestamp = Date().addMinutes(-20).addDays(-1).time
        fakeData2.score = 55
        fakeData2.msCond = 12f
        fakeData2.vol = 68f
        //fakeData2.precision = MeasurePrecision.Bad
        val fakeData3 = MeasureData()
        fakeData3.timestamp = Date().addHours(-1).addDays(-5).time
        fakeData3.score = 76
        fakeData3.msCond = 12f
        fakeData3.vol = 410f
        //fakeData3.precision = MeasurePrecision.Bad
        val fakeData4 = MeasureData()
        fakeData4.timestamp = Date().addHours(-2).addDays(-6).time
        fakeData4.score = 82
        fakeData4.msCond = 12f
        fakeData4.vol = 200f
        val fakeData5 = MeasureData()
        fakeData5.timestamp = Date().addHours(-2).time
        fakeData5.score = 40
        fakeData5.msCond = 2f
        fakeData5.vol = 130f
        measuresDataSource.add(fakeData)
        measuresDataSource.add(fakeData2)
        measuresDataSource.add(fakeData3)
        measuresDataSource.add(fakeData4)
        measuresDataSource.add(fakeData5)
         */

        refreshGraphView()
    }

    fun setDailyDataSource(data: ArrayList<AverageDailyMeasureData>) {
        dailyDataSource = data
        /*
        val fakeData = AverageDailyMeasureData()
        fakeData.hydration = 90.0
        fakeData.timestamp = Date().time
        val fakeData2 = AverageDailyMeasureData()
        fakeData2.hydration = 80.0
        fakeData2.timestamp = Date().addDays(-1).time
        val fakeData3 = AverageDailyMeasureData()
        fakeData3.hydration = 70.0
        fakeData3.timestamp = Date().addDays(-2).time
        val fakeData4 = AverageDailyMeasureData()
        fakeData4.hydration = 60.0
        fakeData4.timestamp = Date().addDays(-3).time
        val fakeData5 = AverageDailyMeasureData()
        fakeData5.hydration = 50.0
        fakeData5.timestamp = Date().addDays(-4).time
        val fakeData6 = AverageDailyMeasureData()
        fakeData6.hydration = 60.0
        fakeData6.timestamp = Date().addDays(-5).time
        val fakeData7 = AverageDailyMeasureData()
        fakeData7.hydration = 70.0
        fakeData7.timestamp = Date().addDays(-6).addMinutes(1).time
        dailyDataSource.add(fakeData)
        dailyDataSource.add(fakeData2)
        dailyDataSource.add(fakeData3)
        dailyDataSource.add(fakeData4)
        dailyDataSource.add(fakeData5)
        dailyDataSource.add(fakeData6)
        dailyDataSource.add(fakeData7)
        */

        refreshGraphView()
    }

    fun setMonthlyDataSource(data: ArrayList<AverageMonthlyMeasureData>) {
        monthlyDataSource = data
        refreshGraphView()
    }

    fun setGraphViewListener(listener: KamleonGraphViewListener) {
        this.listener = listener
    }

    private fun setGraphViewMode(mode: KamleonGraphViewMode) {
        if (viewMode == mode) { return }
        viewMode = mode
        labelView.visibility = INVISIBLE

        refreshGraphView()
    }

    fun setGraphDataType(type: KamleonGraphDataType) {
        if (dataType == type) { return }
        dataType = type
        labelView.visibility = INVISIBLE

        refreshGraphView()
    }

    fun setStreakMode(isOn: Boolean) {
        contentView.setStreakMode(isOn)
    }

    private fun handleTouchPos(touchPos: PointF) {
        val cgPos = PointF(touchPos.x, touchPos.y)
//        val cgPos = PointF(touchPos.x, height - touchPos.y)
        val graphBarIndex = contentView.graphBarRectIndex(cgPos)
        Log.d(TAG, "TOUCH POS: $graphBarIndex")
        if (graphBarIndex == null) {
            labelView.visibility = INVISIBLE
        } else {
            val graphBarRect = contentView.calcBarRect(SizeF(contentView.width.toFloat(), contentView.height.toFloat()), graphBarIndex)
            showLabelView(graphBarIndex, graphBarRect, cgPos)
        }
    }

    private fun showLabelView(graphBarIndex: Int, barRect: RectF, pos: PointF) {
        Log.d(TAG, "showLabelView")
        if (labelView.visibility == INVISIBLE) {
            labelView.visibility = VISIBLE
        }

        val barRtMidX = (barRect.left + barRect.right) / 2.0
        var labelViewX = (barRtMidX - labelView.width / 2.0)
        var labelIndicatorDelta: Float = (labelView.width / 2.0).toFloat()
        if (labelViewX < 0) {
            labelIndicatorDelta = (labelView.width / 2.0 + labelViewX).toFloat()
            labelViewX = -labelView.width / 2.0
        } else if (labelViewX >= (barRect.right - labelView.width)) {
            labelIndicatorDelta = (labelViewX - (barRect.right - labelView.width)).toFloat()
            labelViewX = (barRect.right - labelView.width).toDouble()
        }

        var labelViewY = (barRect.top - labelView.height / 2.0)

        labelView.x = (labelViewX + labelView.width / 2.0).toFloat()
        labelView.y = (contentLayout.y + labelViewY - labelView.height / 2.0).toFloat()

        labelView.setData(contentView.getBarValue(graphBarIndex), contentView.getBarValueUnit(), contentView.getBarLabel(graphBarIndex),
            contentView.getDataInRange(graphBarIndex), dataType, viewMode)
        labelView.setIndicatorPos((labelView.width / 2.0 - labelIndicatorDelta).toFloat())
    }

    private fun refreshGraphView() {
        val dateToDraw = Date()
        val graphDrawData = generateGraphDrawDatasource(dateToDraw)

        headerView.setData(graphDrawData)
        footerView.setData(graphDrawData)
        contentView.setData(graphDrawData)
    }

    private fun generateGraphDrawDatasource(date: Date) : KamleonGraphBarDrawData {
        val startDate = KamleonGraphBarDrawData.calcStartDateFrom(viewMode, date)
        val endDate = KamleonGraphBarDrawData.calcEndDateFrom(viewMode, date)
        dataSourceNew = when (viewMode) {
            KamleonGraphViewMode.Daily -> measuresDataSource as ArrayList<AveragesData>
            KamleonGraphViewMode.Weekly -> dailyDataSource as ArrayList<AveragesData>
            KamleonGraphViewMode.Monthly -> dailyDataSource as ArrayList<AveragesData>
            KamleonGraphViewMode.Yearly -> monthlyDataSource as ArrayList<AveragesData>
        }

        Log.d(TAG, "Start:" + startDate.formatDate() + " -> End:" + endDate.formatDate())
        Log.d("Graph", "dataSource2 size: ${dataSourceNew.size}")
        //val filteredDataSource = dataSource.filter { it.timestamp >= startDate.time && it.timestamp <= endDate.time}
        val filteredDataSource2 = dataSourceNew.filter { it.timestamp >= startDate.time && it.timestamp <= endDate.time }
        Log.d("Graph", "filteredDataSource2 size: ${filteredDataSource2.size}")
        val xStepCount = graphViewXSteps

        val xyValues = ArrayList<KamleonGraphDataXY>()
        val precisionValues = ArrayList<Boolean>()
        var maxYVal = 0.0
        for (i in 0 until xStepCount) {
            val graphBarItemTimeRange = graphViewXRange(startDate, endDate, i)
            //val givenBarItemData = filteredDataSource.filter { it.timestamp >= graphBarItemTimeRange[0] && it.timestamp <= graphBarItemTimeRange[1]}
            val givenBarItemData2 = filteredDataSource2.filter { it.timestamp >= graphBarItemTimeRange[0] && it.timestamp <= graphBarItemTimeRange[1]}
            val givenBarItemDataPrecise = givenBarItemData2.filter { it.isPrecise }
            if (givenBarItemData2.isEmpty()) {
                xyValues.add(KamleonGraphDataXY(i.toDouble(), 0.0))
                precisionValues.add(false)
            }
            // When there is NO precise data, use the average of the non-precise data
            else if (givenBarItemDataPrecise.isEmpty()) {
                Log.d(TAG, "No precise data: ${Date(graphBarItemTimeRange[0]).formatTime}")
                val itemYValueNotPrecise = givenBarItemData2.sumOf { it.scoreValue(dataType) } / givenBarItemData2.size
                if (maxYVal < itemYValueNotPrecise) { maxYVal = itemYValueNotPrecise }
                val kamleonGraphDataXY = KamleonGraphDataXY(i.toDouble(), itemYValueNotPrecise)
                kamleonGraphDataXY.data = givenBarItemData2.map { it }.toCollection(ArrayList())
                xyValues.add(kamleonGraphDataXY)
                precisionValues.add(false)
            // When there is precise data
            } else {
                Log.d(TAG, "No precise data: ${Date(graphBarItemTimeRange[0]).formatTime} _ ${givenBarItemData2.size}")
                Log.d(TAG, "No precise data: ${Date(graphBarItemTimeRange[0]).formatTime} _ ${givenBarItemDataPrecise.size}")
                //val itemYValue = givenBarItemData.sumOf { it.dataVal } / givenBarItemData.size
                val itemYValue2Precise = givenBarItemDataPrecise.sumOf { it.scoreValue(dataType) } / givenBarItemDataPrecise.size
                if (maxYVal < itemYValue2Precise) { maxYVal = itemYValue2Precise }
                val kamleonGraphDataXY = KamleonGraphDataXY(i.toDouble(), itemYValue2Precise)
                kamleonGraphDataXY.data = givenBarItemData2.map { it }.toCollection(ArrayList())
                xyValues.add(kamleonGraphDataXY)
                precisionValues.add(true)
            }
        }

        var xyRange = KamleonGraphDataXY(0.0, 0.0)
        if (dataType == KamleonGraphDataType.hydration) {
            xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 100.0)
        } else if (dataType == KamleonGraphDataType.electrolytes) {
            if (maxYVal <= 50.0) {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 50.0)
            } else if (maxYVal <= 100.0) {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 100.0)
            } else if (maxYVal <= 200.0) {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 200.0)
            } else {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 500.0)
            }
        } else if (dataType == KamleonGraphDataType.volume) {
            if (maxYVal <= 50.0) {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 50.0)
            } else if (maxYVal <= 100.0) {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 100.0)
            } else if (maxYVal <= 200.0) {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 200.0)
            } else if (maxYVal <= 500.0) {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 500.0)
            } else if (maxYVal <= 1000.0) {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 1000.0)
            } else {
                xyRange = KamleonGraphDataXY(xStepCount.toDouble(), 5000.0)
            }
        }

        val maxY = xyRange.y.toInt()
        val yLabelStep = maxY / 5
        val yLabelAry = ArrayList<KamleonGraphAxisLabelItem>()
        for (tempY in 0..maxY step yLabelStep) {
            if (dataType == KamleonGraphDataType.hydration) {
                yLabelAry.add(KamleonGraphAxisLabelItem(tempY.toDouble(), tempY.toString() + context.getString(R.string.unit_percentage)))
            } else {
                yLabelAry.add(KamleonGraphAxisLabelItem(tempY.toDouble(), tempY.toString()))
            }
        }

        return KamleonGraphBarDrawData(
                                        date,
                                        xyValues,
                                        xyRange,
                                        viewMode.xLabelStrings(startDate, xStepCount),
                                        yLabelAry,
                                        dataType,
                                        viewMode,
                                        precisionValues
        )
    }

    private val graphViewXSteps: Int
        get() = when (viewMode) {
            KamleonGraphViewMode.Daily -> 24
            KamleonGraphViewMode.Weekly -> 7
            KamleonGraphViewMode.Monthly -> 30
            KamleonGraphViewMode.Yearly -> 12
        }

    private fun graphViewXRange(startDate: Date, endDate: Date, xStep: Int) : LongArray {
        var rangeLower: Long = startDate.time
        var rangeUpper: Long = startDate.time
        when (viewMode) {
            KamleonGraphViewMode.Daily -> {
                rangeLower = startDate.beginningOfDay.addHours(xStep).time
                rangeUpper = startDate.beginningOfDay.addHours(xStep + 1).time - 1L
            }
            KamleonGraphViewMode.Weekly -> {
                rangeLower = startDate.beginningOfDay.addDays(xStep).time
                rangeUpper = startDate.beginningOfDay.addDays(xStep).endOfDay.time
            }
            KamleonGraphViewMode.Monthly -> {
                rangeLower = startDate.beginningOfDay.addDays(xStep).time
                rangeUpper = startDate.beginningOfDay.addDays(xStep).endOfDay.time
            }
            KamleonGraphViewMode.Yearly -> {
                rangeLower = startDate.beginningOfDay.addMonths(xStep).beginningOfMonth.time
                rangeUpper = startDate.beginningOfDay.addMonths(xStep).endOfMonth().time
            }
        }

        return longArrayOf(rangeLower, rangeUpper)
    }

    /// KmlnHeaderViewListener
    override fun onHeaderViewModeChanged(mode: KamleonGraphViewMode) {
        setGraphViewMode(mode)
    }

    /// KmlnFooterViewListener
    override fun onFooterViewStreakChanged(isStreak: Boolean) {
        setStreakMode(isStreak)
    }

    override fun onDrag(v: View?, event: DragEvent?): Boolean {
        Log.d(TAG, "onDrag")
        return false
    }

    override fun onLongClick(v: View?): Boolean {
        Log.d(TAG, "onLongClick")
        return false
    }

    companion object {
        const val TAG = "KamleonGraphView"
    }
}