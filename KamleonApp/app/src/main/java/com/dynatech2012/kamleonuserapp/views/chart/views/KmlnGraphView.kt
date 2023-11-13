package com.dynatech2012.kamleonuserapp.views.chart.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.util.SizeF
import android.view.View
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.chart.data.KamleonGraphBarDrawData
import com.dynatech2012.kamleonuserapp.views.chart.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.chart.data.KamleonGraphViewMode
import com.dynatech2012.kamleonuserapp.views.chart.exts.addDays
import com.dynatech2012.kamleonuserapp.views.chart.exts.addMonths
import com.dynatech2012.kamleonuserapp.views.chart.exts.formatDate
import kotlin.math.min

class KmlnGraphView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val xAxisLinePaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_grey)
        strokeWidth = 1f
    }

    private val yAxisLinePaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_grey)
        pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
        strokeWidth = 1f
    }

    private val xAxisLabelPaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_black)
        textSize = 32f
        strokeWidth = 1f
    }

    private val yAxisLabelPaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_black)
        textSize = 40f
        strokeWidth = 1f
    }

    private val graphBarItemPaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_active)
        style = Paint.Style.FILL
    }

    private val streakLinePaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_active)
        strokeWidth = 2f
    }

    private val graphBarItemGreyPaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_disabled)
        style = Paint.Style.FILL
    }
    private val graphBarItemOrangePaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_orange)
        style = Paint.Style.FILL
    }

    private val minMaxLabelPaint = Paint().apply {
        color = context.getColor(R.color.kmln_graph_color_black)
        typeface = Typeface.DEFAULT_BOLD
        textSize = 28f
        strokeWidth = 1f
    }

    private val xAxisLines: Int = 4
    private val yAxisLines: Int = 7
    private val xLabelHeight: Int = 40

    private val xAxisMarginStart: Int = 16

    private var isStreak = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        renderAxisLines(canvas)
        renderAxisLabels(canvas)
        renderGraphData(canvas)

        if (isStreak) {
            renderStreakLines(canvas)
        }
    }

    private fun calcTextWidth(text: String, paint: Paint) : Float {
        return paint.measureText(text)
    }

    private var dataSource = KamleonGraphBarDrawData.empty()

    fun setData(data: KamleonGraphBarDrawData) {
        dataSource = data
        invalidate()
    }

    fun setStreakMode(isOn: Boolean) {
        isStreak = isOn
        invalidate()
    }

    fun graphBarRectIndex(touchPos: PointF): Int? {
        for (vIndex in dataSource.values.indices) {
            val barItemRect = calcBarRect(SizeF(this.width.toFloat(), this.height.toFloat()), vIndex)
            Log.e("LLL", "barRect($vIndex) = $barItemRect")
            if (barItemRect.contains(touchPos.x, touchPos.y)) {
                return vIndex
            }
        }

//        return 1
        return null
    }

    fun calcBarRect(canvasRt: SizeF, vIndex: Int) : RectF {
        val hStep = calcHStep(canvasRt)
        val barWidth = graphBarItemWidth(hStep)

        val barItemData = dataSource.values[vIndex]
        val rtL = (xAxisMarginStart + hStep * vIndex) + (hStep - barWidth) / 2.0
        val rtR = rtL + barWidth
        val rtB = canvasRt.height - xLabelHeight
        val rtT = rtB - calcVStep(canvasRt) * barItemData.y

        return RectF(rtL.toFloat(), rtT.toFloat(), rtR.toFloat(), rtB)
    }

    private fun graphBarItemWidth(hStep: Double) : Double {
        return when (dataSource.mode) {
            KamleonGraphViewMode.Daily -> hStep / 10.0 * 6.0
            KamleonGraphViewMode.Weekly -> hStep / 10.0 * 3.0
            KamleonGraphViewMode.Monthly -> hStep / 10.0 * 4.0
            KamleonGraphViewMode.Yearly -> hStep / 10.0 * 5.0
        }
    }

    private fun calcBarRect(canvasRt: SizeF, vIndex: Int, fromY: Double, toY: Double) : RectF {
        val hStep = calcHStep(canvasRt)
        val barWidth = graphBarItemWidth(hStep)

        val rtL = (xAxisMarginStart + hStep * vIndex) + (hStep - barWidth) / 2.0
        val rtR = rtL + barWidth

        val bottomYPos = canvasRt.height - xLabelHeight

        val fromYPos = bottomYPos - calcVStep(canvasRt) * fromY
        val toYPos = bottomYPos - calcVStep(canvasRt) * toY

        return RectF(rtL.toFloat(), fromYPos.toFloat(), rtR.toFloat(), toYPos.toFloat())
    }

    fun getBarValue(index: Int) : String {
        val barItemVal = dataSource.values[index].y
        return String.format("%.1f", barItemVal)
    }

    fun getBarValueUnit() : String {
        return dataSource.type.getUnit()
    }

    fun getBarLabel(index: Int) : String {
        val startDate = dataSource.startDate()
        when (dataSource.mode) {
            KamleonGraphViewMode.Daily -> {
                return "${index + 1}:00 h"
            }
            KamleonGraphViewMode.Monthly -> {
                return dataSource.startDate().addDays(index).formatDate("MMM dd, yyyy")
            }
            KamleonGraphViewMode.Weekly -> {
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                return dayNames[startDate.addDays(index).day]
            }
            KamleonGraphViewMode.Yearly -> {
                val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                return monthNames[startDate.addMonths(index).month]
            }
        }
    }

    private fun calcHStep(canvasRt: SizeF) : Double {
        return ((canvasRt.width - xAxisMarginStart) / (yAxisLines + 1) * yAxisLines) / (dataSource.xyRange.x + 0.3)
    }

    private fun calcVStep(canvasRt: SizeF) : Double {
        return (canvasRt.height - xLabelHeight) / (dataSource.xyRange.y)
    }

    private fun renderStreakLines(canvas: Canvas) {
        if (dataSource.type == KamleonGraphDataType.hydration) {
            renderStreakLine(canvas, KamleonGraphDataType.HydrationStreakValue)
        } else if (dataSource.type == KamleonGraphDataType.electrolytes) {
            renderStreakLine(canvas, KamleonGraphDataType.ElectrolyteStreakValueLower)
            renderStreakLine(canvas, KamleonGraphDataType.ElectrolyteStreakValueUpper)

            renderStreakLineLabel(context.getString(R.string.kmln_graph_label_optimum), canvas, KamleonGraphDataType.ElectrolyteStreakValueUpper, false)

        } else if (dataSource.type == KamleonGraphDataType.volume) {
            renderStreakLine(canvas, dataSource.minForStreak())
            renderStreakLine(canvas, dataSource.maxForStreak())
            renderStreakLineLabel(context.getString(R.string.kmln_graph_label_minimum), canvas, dataSource.minForStreak(), true)
            renderStreakLineLabel(context.getString(R.string.kmln_graph_label_maximum), canvas, dataSource.maxForStreak(), false)
        }
    }

    private fun renderStreakLineLabel(strLabel: String, canvas: Canvas, value: Double, isMin: Boolean) {
        val canvasSize = SizeF(canvas.width.toFloat(), canvas.height.toFloat())
        val yOffset = if (isMin) -30 else 30

        canvas.drawText(strLabel,
            (canvas.width / 3.0 * 2.0).toFloat(), calcBarRect(canvasSize, 0, 0.0, value).bottom + yOffset, minMaxLabelPaint)
    }

    private fun renderStreakLine(canvas: Canvas, value: Double) {
        val canvasSize = SizeF(canvas.width.toFloat(), canvas.height.toFloat())
        val rtB = canvas.height - xLabelHeight
        val streakLinePosY = (rtB - calcVStep(canvasSize) * value).toFloat()

        canvas.drawLine(0f, streakLinePosY, canvas.width.toFloat(), streakLinePosY, streakLinePaint)
    }

    private fun renderGraphData(canvas: Canvas) {
        val canvasSize = SizeF(canvas.width.toFloat(), canvas.height.toFloat())

        for (vIndex in dataSource.values.indices) {
            val orgBarRect = calcBarRect(canvasSize, vIndex)
            if (isStreak) {
                val yVal = dataSource.values[vIndex].y
                if (dataSource.type == KamleonGraphDataType.hydration) {
                    if (yVal > 0 && yVal < KamleonGraphDataType.HydrationStreakValue) {
                        canvas.drawRect(calcBarRect(canvasSize, vIndex, yVal, KamleonGraphDataType.HydrationStreakValue), graphBarItemOrangePaint)
                    }

                    canvas.drawRect(orgBarRect, graphBarItemGreyPaint)

                } else if (dataSource.type == KamleonGraphDataType.electrolytes) {
                    canvas.drawRect(orgBarRect, graphBarItemGreyPaint)
                    if (yVal > KamleonGraphDataType.ElectrolyteStreakValueLower) {
                        canvas.drawRect(calcBarRect(canvasSize, vIndex, KamleonGraphDataType.ElectrolyteStreakValueLower, min(yVal, KamleonGraphDataType.ElectrolyteStreakValueUpper)), graphBarItemPaint)
                    }
                } else if (dataSource.type == KamleonGraphDataType.volume) {
                    val minValue = dataSource.minForStreak()
                    val maxValue = dataSource.maxForStreak()
                    if (yVal == minValue || yVal == maxValue) {
                        canvas.drawRect(orgBarRect, graphBarItemPaint)
                    } else {
                        canvas.drawRect(orgBarRect, graphBarItemGreyPaint)
                    }
                }
            } else {
                canvas.drawRect(orgBarRect, graphBarItemPaint)
            }
        }
    }

    private fun renderAxisLines(canvas: Canvas) {
        val vStep = (canvas.height - xLabelHeight) / (xAxisLines - 1)
        for (xIndex in 0 until xAxisLines) {
            val yPos: Float = (vStep * xIndex).toFloat()
            canvas.drawLine(0f, yPos, canvas.width.toFloat(), yPos, xAxisLinePaint)
        }

        val hStep = canvas.width / (yAxisLines + 1)
        for (yIndex in 0 until yAxisLines) {
            val xPos: Float = (hStep * (yIndex + 1)).toFloat()
            canvas.drawLine(xPos, 0f, xPos, canvas.height.toFloat(), yAxisLinePaint)
        }
    }

    private fun renderAxisLabels(canvas: Canvas) {
        val canvasSize = SizeF(canvas.width.toFloat(), canvas.height.toFloat())
        val xLabelCount = dataSource.xLabels.size
        if (xLabelCount > 0) {
            val hStep = calcHStep(canvasSize)
            for (xLabelIndex in dataSource.xLabels.indices) {
                val xPos: Float = (xAxisMarginStart + hStep * dataSource.xLabels[xLabelIndex].dataVal).toFloat()
                val xLabelDrawWidth = calcTextWidth(dataSource.xLabels[xLabelIndex].label, xAxisLabelPaint)
                canvas.drawText(dataSource.xLabels[xLabelIndex].label,
                    (xPos + hStep / 2.0f - xLabelDrawWidth / 2.0f).toFloat(), canvas.height.toFloat(), xAxisLabelPaint)
            }
        }

        val yLabelCount = dataSource.yLabels.size
        if (yLabelCount > 0) {
            val vStep = calcVStep(canvasSize)
            for (yLabelIndex in dataSource.yLabels.indices) {
                val yPos: Float = canvas.height - (vStep * dataSource.yLabels[yLabelIndex].dataVal).toFloat()
                val xPos: Float = (canvas.width / (yAxisLines + 1.0 - 0.15) * yAxisLines).toFloat()
                canvas.drawText(dataSource.yLabels[yLabelIndex].label, xPos, yPos, yAxisLabelPaint)
            }
        }

        /*
        val xLabelCounts = 30
        val xLabelStep = 2
        val hStep = (canvas.width / (yAxisLines + 1) * yAxisLines) / (xLabelCounts + 1)
        for (xLabelNum in xLabelStep..xLabelCounts step xLabelStep ) {
            val xPos: Float = (hStep * xLabelNum).toFloat()
            val xLabelText = xLabelNum.toString()
            val xLabelDrawWidth = calcTextWidth(xLabelText, xAxisLabelPaint)
            canvas.drawText(xLabelText, xPos - xLabelDrawWidth / 2.0f, canvas.height.toFloat(), xAxisLabelPaint)
        }

        val yLabelCounts = 50

        val vStep = (canvas.height - xLabelHeight) / (yLabelCounts)
        for (yLabelNum in 0..50 step 10 ) {
            val yPos: Float = canvas.height - (vStep * yLabelNum).toFloat()
            val xPos: Float = (canvas.width / (yAxisLines + 1.0 - 0.15) * yAxisLines).toFloat()
            canvas.drawText(yLabelNum.toString(), xPos, yPos, yAxisLabelPaint)
        }
         */
    }
}