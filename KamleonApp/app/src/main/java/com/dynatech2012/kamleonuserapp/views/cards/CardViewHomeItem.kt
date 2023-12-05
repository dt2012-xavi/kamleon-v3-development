package com.dynatech2012.kamleonuserapp.views.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.extensions.px
import com.dynatech2012.kamleonuserapp.models.Recommendation
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardViewHomeItem(recommendation: Recommendation, modifier: Modifier, showBottomSheet: Boolean, sheetState: SheetState, onClick: () -> Unit, onDismiss: () -> Unit, sheetContent: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val elevation = 10.dp
    val clickable = recommendation.clickable
    val blocked = recommendation.blocked
    val foregroundColor = colorResource(id = R.color.color_fa_80)
    val lockString = stringResource(id = R.string.premium_card_message)

    //val sheetState = rememberModalBottomSheetState(true)
    //val scope = rememberCoroutineScope()
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
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(100.dp)
            ) {
                Column(
                    modifier = Modifier
                ) {
                    if (recommendation.image != null) {
                        Image(
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                            ,
                            painter = painterResource(id = recommendation.image),

                            contentDescription = "",

                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (recommendation.kind != null) {
                        Text(
                            text = stringResource(id = recommendation.kind),
                            fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                            color = colorResource(id = R.color.kamleon_secondary_grey_60)
                        )
                    }
                    Text(
                        modifier = Modifier.padding(bottom = 4.dp),
                        text = stringResource(id = recommendation.titleShort),
                        fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                        color = colorResource(id = R.color.kamleon_dark_grey),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        modifier = Modifier
                            .padding(top = 4.dp),
                        text = if (recommendation.text != null ) stringResource(id = recommendation.text!!) else "",
                        fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                        color = colorResource(id = R.color.kamleon_dark_grey),
                        maxLines = if (recommendation.kind != null) 2 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (blocked) {
                Box (
                    modifier = Modifier
                        .height(124.dp)
                        .background(color = foregroundColor, shape = shape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        text = lockString,
                        fontSize = dimensionResource(R.dimen.ts_16).value.sp,
                        color = colorResource(id = R.color.kamleon_dark_grey),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                    Row()
                    {
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            Image(
                                painterResource(id = R.drawable.padlock),
                                contentDescription = "",
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                                    //.background(color = Color.Red, shape = TriangleShape(12f)),
                                    .drawBehind {
                                        drawCornerTriangle() }
                                    ,
                                alignment = Alignment.TopEnd
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

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


private fun DrawScope.drawCornerTriangle(
) {
    rotate(degrees = 45F) {

        drawRect(
            color = Color.White,
            topLeft = Offset(-100f, -194f),
            size = Size(96.px.toFloat(), 96.px.toFloat()),
            style = androidx.compose.ui.graphics.drawscope.Fill,
            alpha = 1f,
            colorFilter = null
        )
    }



}

class TriangleShape(private val roundRadius: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ) = Outline.Generic(
        Path().apply {
            val CR = size.width
            val AR = size.height / 2

            val DAE = atan(CR / AR)
            val DAQ = PI.toFloat() / 2 - DAE
            val DAM = DAE / 2
            val AD = roundRadius / tan(DAM)
            val DQ = AD * sin(DAQ)
            val AQ = AD * cos(DAQ)

            //move to point D
            moveTo(AQ, DQ)

            val GCN = (PI.toFloat() - 2 * DAE) / 2
            val CG = roundRadius / tan(GCN)
            val GT = CG * sin(GCN)
            val CT = CG * cos(GCN)

            // line to point G
            lineTo(CR - CT, AR - GT)

            val CN = roundRadius / sin(GCN)
            val CNG = PI.toFloat() / 2 - GCN
            // right arc
            arcToRad(Rect(Offset(CR - CN, AR), roundRadius), -CNG, 2 * CNG, false)

            val AB = size.height

            // line to point K
            lineTo(AQ, AB - DQ)

            val OBV = DAM + DAQ
            val BO = roundRadius / sin(DAM)
            val BV = BO * cos(OBV)
            val OV = BO * sin(OBV)

            val BOV = PI.toFloat() - OBV
            val BOK = PI.toFloat() - DAM
            val KOV = BOK - BOV
            val KOU = PI.toFloat() / 2 - KOV
            val JOK = PI.toFloat() - DAE

            // bottom left arc
            arcToRad(Rect(Offset(BV, AB - OV), roundRadius), KOU, JOK, false)

            // line to point E
            lineTo(0f, AD)

            // top left arc
            arcToRad(Rect(Offset(BV, OV), roundRadius), PI.toFloat(), JOK, false)
        }
    )
}



fun Modifier.coloredShadow(
    color: Color,
    alpha: Float = 0.2f,
    borderRadius: Dp = 0.dp,
    shadowRadius: Dp = 20.dp,
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