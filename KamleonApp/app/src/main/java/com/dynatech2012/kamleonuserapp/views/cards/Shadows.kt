package com.dynatech2012.kamleonuserapp.views.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Draws a soft colored shadow behind a rounded rectangle the size of the element. */
fun Modifier.coloredShadow(
    color: Color,
    alpha: Float = 0.2f,
    borderRadius: Dp = 0.dp,
    shadowRadius: Dp = 20.dp,
    blurRadius: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = this.drawBehind {
        val shadowColor = color.copy(alpha = alpha).toArgb()
        val transparent = color.copy(alpha= 0f).toArgb()
        this.drawIntoCanvas {
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.color = transparent
            frameworkPaint.setShadowLayer(
                shadowRadius.toPx(),
                offsetX.toPx(),
                offsetY.toPx(),
                shadowColor
            )

            /*
            if (blurRadius != 0.dp) {
                frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
                    blurRadius.toPx(),
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }

             */
            it.drawRoundRect(
                0f,
                0f,
                this.size.width,
                this.size.height,
                borderRadius.toPx(),
                borderRadius.toPx(),
                paint
            )
        }
}


@Composable
fun ClippedShadowCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = Color.White,
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ShadowBox(
        elevation = elevation,
        shape = shape,
        modifier = modifier
    ) {
            Card(
                modifier = modifier,
                shape = shape,
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    ),
                border = border,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                ),
                content = content
            )
        }
}

@Composable
fun ClippedShadow(elevation: Dp, shape: Shape, modifier: Modifier = Modifier) {
    Layout(
        modifier
            .drawWithCache {
                // Naive cache setup similar to foundation's Background.
                val path = Path()
                var lastSize: Size? = null

                fun updatePathIfNeeded() {
                    if (size != lastSize) {
                        path.reset()
                        path.addOutline(
                            shape.createOutline(size, layoutDirection, this)
                        )
                        lastSize = size
                    }
                }

                onDrawWithContent {
                    updatePathIfNeeded()
                    clipPath(path, ClipOp.Difference) {
                        this@onDrawWithContent.drawContent()
                    }
                }
            }
            .shadow(elevation, shape)
    ) { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowBox(
    elevation: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        {
            ClippedShadow(elevation, shape, modifier)
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                content()
            }
        },
        modifier
    ) { measurables, constraints ->
        require(measurables.size == 2)

        val shadow = measurables[0]
        val target = measurables[1]

        val targetPlaceable = target.measure(constraints)
        val width = targetPlaceable.width
        val height = targetPlaceable.height

        val shadowPlaceable = shadow.measure(Constraints.fixed(width, height))

        layout(width, height) {
            shadowPlaceable.place(0, 0)
            targetPlaceable.place(0, 0)
        }
    }
}
