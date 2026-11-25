package com.dynatech2012.kamleonuserapp.views.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.utils.DebugLog

@Composable
fun AnimatedCircularProgressIndicator(
    currentValue: Int?,
    maxValue: Int,
    progressBackgroundColor: Color,
    progressIndicatorColor: Color,
            //completedColor: Color,
    modifier: Modifier = Modifier
) {
    val stroke = with(LocalDensity.current) {
        Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    }

    val colorList: List<Color> = listOf(
        colorResource(id = R.color.gradient_1),
        colorResource(id = R.color.gradient_2),
        colorResource(id = R.color.gradient_3),
        colorResource(id = R.color.gradient_4),
        colorResource(id = R.color.gradient_5)
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val animateFloat = remember { (currentValue ?: 0) / maxValue.toFloat() }

        if (currentValue != null) {
            Canvas(
                Modifier
                    .progressSemantics((currentValue ?: 0) / maxValue.toFloat())
                    .size(CircularIndicatorDiameter)
            ) {
                // Start at 12 O'clock
                val startAngle = 270f
                val sweep: Float = animateFloat * 360f
                val diameterOffset = stroke.width / 2

                drawCircle(
                    color = progressBackgroundColor,
                    style = stroke,
                    radius = size.minDimension / 2.0f - diameterOffset
                )
                rotate(270f, Offset(size.width / 2, size.height / 2)) {
                    drawCircularProgressIndicator(
                        startAngle + 90,
                        sweep,
                        progressIndicatorColor,
                        colorList,
                        stroke
                    )
                    drawBallProgressIndicator(
                        startAngle + 90,
                        sweep,
                        progressIndicatorColor,
                        colorList,
                        stroke
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCircularProgressIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    colorList: List<Color>,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset
    drawArc(
        brush = Brush.sweepGradient(
            0f to colorList[0],
            0.25f to colorList[1],
            0.5f to colorList[2],
            0.75f to colorList[3],
            0.98f to colorList[4],
            1f to colorList[0],
        ),
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(diameterOffset, diameterOffset),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
}

private fun DrawScope.drawBallProgressIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    colorList: List<Color>,
    stroke: Stroke,
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset
    // ranges not included
    DebugLog.d("PB") { "Progress Bar color sweep: $sweep _ lerp: ${sweep / 360f}" }
    val finalColor = when (val percentage = (sweep) / 360f)
    {
        in 0f..0.25f ->  {
            val lerp = interpolate(0.0f, 0.25f, percentage)
            val fc = lerp(colorList[0], colorList[1], lerp)
            DebugLog.d("PB") { "Progress Bar color 0,0 a 0,25 _ sweep: $sweep _ lerp: $lerp _ color: $fc" }
            fc
        }
        in 0.25f..0.5f -> {
            val lerp = interpolate(0.25f, 0.5f, percentage)
            val fc = lerp(colorList[1], colorList[2], lerp)
            DebugLog.d("PB") { "Progress Bar color 0,25 a 0,5 _ sweep: $sweep _ lerp: $lerp _ color: $fc" }
            fc
        }
        in 0.5f..0.75f -> {
            val lerp = interpolate(0.5f, 0.75f, percentage)
            val fc = lerp(colorList[2], colorList[3], lerp)
            DebugLog.d("PB") { "Progress Bar color 0,5 a 0,75 _ sweep: $sweep _ lerp: $lerp _ color: $fc" }
            fc
        }
        in 0.75f..1f -> {
            val lerp = interpolate(0.75f, 1f, percentage)
            val fc = lerp(colorList[3], colorList[4], lerp)
            DebugLog.d("PB") { "Progress Bar color 0,75 a 1 _ sweep: $sweep _ lerp: $lerp _ color: $fc" }
            fc
        }
        else -> colorList[4]
    }
    drawCircle(
        color = finalColor,
        /*
        brush = Brush.sweepGradient(
            0.01f to colorList[0],
            0.2f to colorList[0],
            0.4f to colorList[1],
            0.6f to colorList[2],
            0.8f to colorList[3],
            0.96f to colorList[4],
            0.99f to colorList[4],
            1f to colorList[0],
        ),
         */
        radius = stroke.width,
        center = Offset(
            diameterOffset + arcDimen / 2 + (arcDimen / 2) * Math.cos(Math.toRadians((startAngle + sweep).toDouble())).toFloat(),
            diameterOffset + arcDimen / 2 + (arcDimen / 2) * Math.sin(Math.toRadians((startAngle + sweep).toDouble())).toFloat()
        )
    )
}

fun interpolate(floatA: Float, floatB: Float, floatC: Float): Float {
    return (floatC - floatA) / (floatB - floatA)
}

// Diameter of the indicator circle
private val CircularIndicatorDiameter = 100.dp