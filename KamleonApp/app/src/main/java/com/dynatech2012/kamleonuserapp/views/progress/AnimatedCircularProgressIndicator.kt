package com.dynatech2012.kamleonuserapp.views.progress

import android.util.Log
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

@Composable
fun AnimatedCircularProgressIndicator(
    currentValue: Int,
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
        val animateFloat = remember { currentValue / maxValue.toFloat() }
        /*
        val animateFloat = remember { Animatable(0f) }
        LaunchedEffect(animateFloat) {
            animateFloat.animateTo(
                targetValue = currentValue / maxValue.toFloat(),
                animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
            )
        }
        */

        Canvas(
            Modifier
                .progressSemantics(currentValue / maxValue.toFloat())
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
            //drawCircularProgressIndicator(startAngle, sweep, progressIndicatorColor, stroke)
            rotate(270f, Offset(size.width / 2, size.height / 2)) {
                drawCircularProgressIndicator(startAngle + 90, sweep, progressIndicatorColor, colorList, stroke)
                drawBallProgressIndicator(startAngle + 90, sweep, progressIndicatorColor, colorList, stroke)
            }
            /*
            if (currentValue == maxValue) {
                drawCircle(
                    color = completedColor,
                    style = stroke,
                    radius = size.minDimension / 2.0f - diameterOffset
                )
            }
            */
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
        //color = color,
        /*brush = Brush.sweepGradient( // !!! that what
            0f to Color(0x00EF7B7B),
            0.9f to Color(0xFFEF7B7B),
            0.91f to Color(0x00EF7B7B), // there was a problem with start of the gradient, maybe there way to solve it better
            1f to Color(0x00EF7B7B)
        ),*/
        brush = Brush.sweepGradient(
            0.2f to colorList[0],
            0.4f to colorList[1],
            0.6f to colorList[2],
            0.8f to colorList[3],
            0.98f to colorList[4],
            1f to colorList[0],
            //center = Offset(size.width / 2, size.height / 2),
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
    Log.d("PB", "sweep: $sweep _ lerp: ${sweep / 360f}")
    val finalColor = when (val percentage = sweep / 360f)
    {
        in 0.0f..0.2f -> lerp(colorList[0], colorList[1], percentage)
        in 0.2f..0.4f -> lerp(colorList[1], colorList[2], percentage)
        in 0.4f..0.6f -> lerp(colorList[2], colorList[3], percentage)
        in 0.6f..0.8f -> lerp(colorList[3], colorList[4], percentage)
        in 0.8f..1f -> colorList[4]
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
            //center = Offset(size.width / 2, size.height / 2),
        ),
         */
        radius = stroke.width,
        center = Offset(
            diameterOffset + arcDimen / 2 + (arcDimen / 2) * Math.cos(Math.toRadians((startAngle + sweep).toDouble())).toFloat(),
            diameterOffset + arcDimen / 2 + (arcDimen / 2) * Math.sin(Math.toRadians((startAngle + sweep).toDouble())).toFloat()
        )
        //style = style,
        //radius = size.minDimension / 2.0f - diameterOffset
    )
}

// Diameter of the indicator circle
private val CircularIndicatorDiameter = 100.dp