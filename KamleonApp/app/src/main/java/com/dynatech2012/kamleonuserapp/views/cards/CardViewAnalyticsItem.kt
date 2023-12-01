package com.dynatech2012.kamleonuserapp.views.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.progress.AnimatedCircularProgressIndicator
import com.dynatech2012.kamleonuserapp.views.progress.VolumeProgressIndicator

@Composable
fun CardViewAnalyticsItem(analyticType: AnalyticType, subtitleStart: String, descriptionStart: String, startValue: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val elevation = 10.dp
    val value by rememberSaveable { mutableIntStateOf(startValue) }
    val subtitle by rememberSaveable { mutableStateOf(subtitleStart) }
    val description by rememberSaveable { mutableStateOf(descriptionStart) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.color_fa),
        ),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(if (analyticType == AnalyticType.EMPTY) 0.5f else 1f)
            .clickable { onClick() }
            .coloredShadow(
                color = Color.Black,
                alpha = 0.15f,
                offsetX = 2.dp,
                offsetY = 2.dp,
                shadowRadius = 8.dp,
                //blurRadius = 12.dp,
                //spread = 0.dp,
                borderRadius = 12.dp,
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
    )

    {
        if (analyticType == AnalyticType.EMPTY) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .height(130.dp)
                    .padding(horizontal = 20.dp)
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.baseline_add_24),
                    contentDescription = "",
                    modifier = Modifier
                        .size(36.dp)
                        //.clip(CircleShape)
                        .coloredShadow(
                            color = Color.Black,
                            alpha = 0.15f,
                            offsetX = 2.dp,
                            offsetY = 2.dp,
                            borderRadius = 18.dp,
                            shadowRadius = 4.dp,
                        )
                        .background(colorResource(id = R.color.color_fa), CircleShape),
                    colorFilter = tint(colorResource(id = R.color.kamleon_dark_grey))
                )
                Spacer(modifier = Modifier
                    .weight(1f))
                Text(
                    text = stringResource(id = R.string.analytic_add_new),
                    fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey))
            }
        } else {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 10.dp)
                        .weight(1f),

                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 2.dp),
                        text = analyticType.title,
                        fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                        color = colorResource(id = R.color.kamleon_secondary_grey_50),
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 6.dp),
                        text = subtitle,
                        fontSize = dimensionResource(R.dimen.ts_20).value.sp,
                        color = colorResource(id = R.color.kamleon_dark_grey),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        modifier = Modifier
                            .padding(top = 4.dp),
                        text = description,
                        fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                        color = colorResource(id = R.color.kamleon_secondary_grey_40),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp),
                    //verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    when (analyticType) {
                        AnalyticType.HYDRATION -> HydrationView(value)
                        AnalyticType.ELECTROLYTE -> ElecView(value)
                        AnalyticType.VOLUME -> VolumeView(value)
                        //AnalyticType.EMPTY -> AnalyticsEmptyView()
                        else -> {}
                    }
                }
            }
        }
    }
}

enum class AnalyticType(val title: String) {
    HYDRATION("Hydration"),
    ELECTROLYTE("Electrolytes"),
    VOLUME("Volume"),
    EMPTY("Empty");
    companion object {
        fun from(id: Int): AnalyticType {
            return when (id) {
                0 -> HYDRATION
                1 -> ELECTROLYTE
                2 -> VOLUME
                3 -> EMPTY
                else -> HYDRATION
            }
        }
    }
}

@Composable
fun HydrationView(hydration: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        /*
        Image(
            painter = painterResource(id =
            when (hydration) {
                in 0..30 -> R.drawable.electrolytes_blue
                in 31..64 -> R.drawable.electrolytes_orange
                in 64..90 -> R.drawable.electrolytes_orange
                else -> R.drawable.electrolytes_red
            }
            ),
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            contentDescription = "",
        )
        */
        AnimatedCircularProgressIndicator(
            currentValue = hydration,
            maxValue = 100,
            progressBackgroundColor = colorResource(id = R.color.transparent),
            progressIndicatorColor = colorResource(id = R.color.color_red),
            //completedColor = colorResource(id = R.color.kamleon_blue),
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .alignByBaseline()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = hydration.toString(),
                    fontSize = dimensionResource(R.dimen.ts_36).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
            }
            Column(
                modifier = Modifier
                    .alignByBaseline(),
                verticalArrangement = Arrangement.aligned(Alignment.Bottom)
            ) {
                Text(
                    text = stringResource(id = R.string.unit_percentage),
                    fontSize = dimensionResource(R.dimen.ts_20).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
            }
        }
    }
}

@Composable
fun ElecView(electrolytes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id =
                when (electrolytes) {
                    in 0..4 -> R.drawable.electrolytes_blue
                    in 5..19 -> R.drawable.electrolytes_orange
                    else -> R.drawable.electrolytes_red
                }
            ),
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            contentDescription = "",
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = electrolytes.toString(),
                    fontSize = dimensionResource(R.dimen.ts_36).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "mS/cm",
                    fontSize = dimensionResource(R.dimen.ts_16).value.sp,
                    color = colorResource(id = R.color.kamleon_secondary_grey_60)
                )
            }
        }
    }
}

@Composable
fun VolumeView(volume: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VolumeProgressIndicator(
            currentValue = volume,
            maxValue = 250,
            color = colorResource(id = R.color.kamleon_blue),
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = volume.toString(),
                    fontSize = dimensionResource(R.dimen.ts_36).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ml",
                    fontSize = dimensionResource(R.dimen.ts_16).value.sp,
                    color = colorResource(id = R.color.kamleon_secondary_grey_60)
                )
            }
        }
    }
}


@Composable
fun AnalyticsEmptyView() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f),
    ) {

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                    text = "+",
                    fontSize = dimensionResource(R.dimen.ts_36).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
            Text(
                    text = stringResource(id = R.string.unit_percentage),
                    fontSize = dimensionResource(R.dimen.ts_20).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
        }
    }
}
