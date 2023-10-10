package com.example.graphviewdemo.customview

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Path
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
import com.example.graphviewdemo.R
import com.example.graphviewdemo.customview.data.KamleonGraphAxisLabelItem
import com.example.graphviewdemo.customview.data.KamleonGraphBarDrawData
import com.example.graphviewdemo.customview.data.KamleonGraphBarItemData
import com.example.graphviewdemo.customview.data.KamleonGraphDataType
import com.example.graphviewdemo.customview.data.KamleonGraphDataXY
import com.example.graphviewdemo.customview.data.KamleonGraphViewMode
import com.example.graphviewdemo.customview.exts.addDays
import com.example.graphviewdemo.customview.exts.addHours
import com.example.graphviewdemo.customview.exts.addMonths
import com.example.graphviewdemo.customview.exts.beginningOfDay
import com.example.graphviewdemo.customview.exts.beginningOfMonth
import com.example.graphviewdemo.customview.exts.beginningOfYear
import com.example.graphviewdemo.customview.exts.endOfDay
import com.example.graphviewdemo.customview.exts.endOfMonth
import com.example.graphviewdemo.customview.exts.endOfYear
import com.example.graphviewdemo.customview.exts.formatDate
import com.example.graphviewdemo.customview.exts.with
import com.example.graphviewdemo.customview.views.KmlnFooterView
import com.example.graphviewdemo.customview.views.KmlnGraphView
import com.example.graphviewdemo.customview.views.KmlnHeaderView
import com.example.graphviewdemo.customview.views.KmlnLabelView
import java.time.Month
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
                Log.e("LLL", "Event:($xPos:$yPos)" + event?.action)
                handleTouchPos(PointF(xPos, yPos))
                return true
            }
        })
    }

    private var dataSource = ArrayList<KamleonGraphBarItemData>()

    fun setData(data: ArrayList<KamleonGraphBarItemData>) {
        dataSource = data
        refreshGraphView()
    }

    fun setGraphViewListener(listener: KamleonGraphViewListener) {
        this.listener = listener
    }

    fun setGraphViewMode(mode: KamleonGraphViewMode) {
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
        Log.e("LLL", "TOUCH POS: $graphBarIndex")
        if (graphBarIndex == null) {
            labelView.visibility = INVISIBLE
        } else {
            val graphBarRect = contentView.calcBarRect(SizeF(contentView.width.toFloat(), contentView.height.toFloat()), graphBarIndex)
            showLabelView(graphBarIndex, graphBarRect, cgPos)
        }
    }

    private fun showLabelView(graphBarIndex: Int, barRect: RectF, pos: PointF) {
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

        labelView.setData(contentView.getBarValue(graphBarIndex), contentView.getBarValueUnit(), contentView.getBarLabel(graphBarIndex))
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

        Log.e("LLL", "Start:" + startDate.formatDate() + " -> End:" + endDate.formatDate())
        val filteredDataSource = dataSource.filter { it.timestamp >= startDate.time && it.timestamp <= endDate.time}
        val xStepCount = graphViewXSteps(date)

        var xyValues = ArrayList<KamleonGraphDataXY>()
        var maxYVal = 0.0
        for (i in 0 until xStepCount) {
            val graphBarItemTimeRange = graphViewXRange(startDate, endDate, i)
            val givenBarItemData = filteredDataSource.filter { it.timestamp >= graphBarItemTimeRange[0] && it.timestamp <= graphBarItemTimeRange[1]}
            if (givenBarItemData.isEmpty()) {
                xyValues.add(KamleonGraphDataXY(i.toDouble(), 0.0))
            } else {
                val itemYValue = givenBarItemData.sumOf { it.dataVal } / givenBarItemData.size
                if (maxYVal < itemYValue) { maxYVal = itemYValue }
                xyValues.add(KamleonGraphDataXY(i.toDouble(), itemYValue))
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
                yLabelAry.add(KamleonGraphAxisLabelItem(tempY.toDouble(), tempY.toString() + "%"))
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
                                        viewMode
        )
    }

    private fun graphViewXSteps(date: Date) : Int {
        return when (viewMode) {
            KamleonGraphViewMode.Daily -> 24
            KamleonGraphViewMode.Weekly -> 7
            KamleonGraphViewMode.Monthly -> 31
            KamleonGraphViewMode.Yearly -> 12
        }
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
        Log.e("LLL", "onDrag")
        return false
    }

    override fun onLongClick(v: View?): Boolean {
        Log.e("LLL", "onLongClick")
        return false
    }
}