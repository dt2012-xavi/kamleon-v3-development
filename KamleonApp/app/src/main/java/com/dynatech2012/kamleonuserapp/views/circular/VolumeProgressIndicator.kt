package com.dynatech2012.kamleonuserapp.views.circular

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun VolumeProgressIndicator(
    currentValue: Int,
    maxValue: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val diff = 10

    val stroke = with(LocalDensity.current) {
        Stroke(width = 6.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Round)
    }
    val alfa = when {
        currentValue < 150 -> 0.3f
        currentValue < 250 -> 0.6f
        else -> 1f
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

/*        val animateFloat = remember { Animatable(0f) }
        LaunchedEffect(animateFloat) {
            animateFloat.animateTo(
                targetValue = currentValue / maxValue.toFloat(),
                animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
            )
        }*/
        val displayValue = remember { currentValue / maxValue.toFloat() }

        Canvas(
            Modifier
                .progressSemantics(currentValue / maxValue.toFloat())
                .size(CircularIndicatorDiameter)
                .alpha(alfa)
        ) {
            // Start at 12 O'clock
            if (currentValue > 0f) {
                val startAngle = 135f
                val sweep: Float = 90f - diff//80f
                drawCircularProgressIndicator(startAngle, sweep, color, stroke)
            }

            if (currentValue >= 150f) {
                val startAngle = 225f + diff/2//200f + diff//230f
                val sweep: Float = 90f - diff//80f
                drawCircularProgressIndicator(startAngle, sweep, color, stroke)
            }

            if (currentValue >= 250f) {
                val startAngle = 315f + diff//325f
                val sweep: Float = 90f - diff//80f
                drawCircularProgressIndicator(startAngle, sweep, color, stroke)
            }
        }
    }
}

private fun DrawScope.drawCircularProgressIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(diameterOffset, diameterOffset),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
}

// Diameter of the indicator circle
private val CircularIndicatorDiameter = 100.dp