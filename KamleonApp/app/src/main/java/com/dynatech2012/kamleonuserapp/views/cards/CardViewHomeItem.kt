package com.dynatech2012.kamleonuserapp.views.cards

import android.graphics.Insets
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalMinimumTouchTargetEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardViewHomeItem(tip: Tip, modifier: Modifier, showBottomSheet: Boolean, sheetState: SheetState, onClick: () -> Unit, onDismiss: () -> Unit, sheetContent: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val elevation = 10.dp
    val clickable = tip.clickable
    val locked = tip.locked
    val foregroundColor = colorResource(id = R.color.color_fa_80)
    val lockString = stringResource(id = R.string.subscribe_to_unlock)

    //val sheetState = rememberModalBottomSheetState(true)
    val scope = rememberCoroutineScope()
    //var showBottomSheet by remember { mutableStateOf(false) }

    ClippedShadowCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = clickable, onClick = { onClick() }),
        shape = shape,
        containerColor = colorResource(id = R.color.color_fa_40),
        elevation = elevation,
    )
    {
        Box (){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(100.dp)
            ) {
                Column(
                    modifier = Modifier
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.tip1),
                        modifier = Modifier.size(100.dp),
                        /*
                                painter = rememberVectorPainter(

                                    image = ImageVector.vectorResource(
                                        id = /*tip.image ?: */R.drawable.tip1
                                    )
                ),
                */
                        contentDescription = "",
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 16.dp)
                        .fillMaxWidth(),

                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tip.type,
                        fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                        color = Color(0xFF808285)
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 4.dp),
                        text = tip.title,
                        fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                        color = colorResource(id = R.color.kamleon_dark_grey),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        modifier = Modifier
                            .padding(top = 4.dp),
                        text = tip.description,
                        fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                        color = colorResource(id = R.color.kamleon_dark_grey),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (locked) {
                Row (
                    modifier = Modifier
                        .height(124.dp)
                        .background(color = foregroundColor, shape = shape),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .padding(24.dp),
                        text = lockString,
                        fontSize = dimensionResource(R.dimen.ts_16).value.sp,
                        color = colorResource(id = R.color.kamleon_dark_grey),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                }
            }
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier
                    .fillMaxSize(),
                onDismissRequest = {
                    //showBottomSheet = false
                    onDismiss()
                },
                sheetState = sheetState,
                shape = BottomSheetDefaults.ExpandedShape,
                dragHandle = {},
            ) {
                // Sheet content
                sheetContent()
            }
        }
    }
}

data class Tip(
    val type: String,
    val clickable: Boolean = true,
    val locked: Boolean = false,
    val title: String,
    val description: String,
    val image: Int? = null
)

fun Modifier.coloredShadow(
    color: Color,
    alpha: Float = 0.2f,
    borderRadius: Dp = 0.dp,
    shadowRadius: Dp = 20.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = composed {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparent = color.copy(alpha= 0f).toArgb()
    this.drawBehind {
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