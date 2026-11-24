package com.dynatech2012.kamleonuserapp.views.cards

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme
import com.dynatech2012.kamleonuserapp.models.ElectrolyteLevel
import com.dynatech2012.kamleonuserapp.views.progress.AnimatedCircularProgressIndicator
import com.dynatech2012.kamleonuserapp.views.progress.VolumeProgressIndicator

/** One metric of the last sample on Analytics: title, verdict and explanation, with its gauge on the right. */
@Composable
fun CardViewAnalyticsItem(
    analyticType: AnalyticType,
    subtitle: String,
    description: String,
    value: Int?,
    isPrecise: Boolean,
    electrolyteLevel: ElectrolyteLevel? = null,
    onClick: () -> Unit
) {
    // The card takes the click itself, so the press effect is clipped to its rounded corners.
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.color_fa),
        ),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .coloredShadow(
                color = colorResource(id = R.color.kamleon_dark_grey),
                alpha = 0.5f,
                offsetX = 2.dp,
                offsetY = 2.dp,
                shadowRadius = 6.dp,
                borderRadius = 12.dp,
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 10.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    fontFamily = MatTheme.fontFamily,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(analyticType.title),
                    fontSize = dimensionResource(R.dimen.ts_14).value.sp,
                    color = colorResource(id = R.color.kamleon_secondary_grey_60),
                )
                Text(
                    fontFamily = MatTheme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp),
                    text = subtitle,
                    fontSize = dimensionResource(R.dimen.ts_20).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey),
                )
                Text(
                    fontFamily = MatTheme.fontFamily,
                    fontWeight = FontWeight.Medium,
                    text = description,
                    fontSize = dimensionResource(R.dimen.ts_13).value.sp,
                    color = colorResource(id = R.color.kamleon_secondary_grey_50),
                )
            }
            // TalkBack reads the figure as words ("71 percent") instead of the gauge's separate pieces.
            val valueDescription = value?.let { pluralStringResource(analyticType.valueDescription, it, it) }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .clearAndSetSemantics { valueDescription?.let { contentDescription = it } },
            ) {
                when (analyticType) {
                    AnalyticType.HYDRATION -> HydrationView(value)
                    AnalyticType.ELECTROLYTE -> ElecView(value, electrolyteLevel)
                    AnalyticType.VOLUME -> VolumeView(value, isPrecise)
                }
            }
        }
    }
}

enum class AnalyticType(@StringRes val title: Int, @PluralsRes val valueDescription: Int) {
    HYDRATION(R.string.analytic_metric_hydration, R.plurals.analytic_value_hydration),
    ELECTROLYTE(R.string.analytic_metric_electrolytes, R.plurals.analytic_value_electrolytes),
    VOLUME(R.string.analytic_metric_volume, R.plurals.analytic_value_volume),
}

@Composable
fun HydrationView(hydration: Int?) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedCircularProgressIndicator(
            currentValue = hydration,
            maxValue = 100,
            progressBackgroundColor = colorResource(id = R.color.transparent),
            progressIndicatorColor = Color.Red,
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
                val value = hydration?.toString() ?: stringResource(id = R.string.default_value)
                Text(
                    fontFamily = MatTheme.fontFamilyBook,
                    text = value,
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
                    fontFamily = MatTheme.fontFamilyBook,
                    text = if (hydration != null) stringResource(id = R.string.unit_percentage) else "",
                    fontSize = dimensionResource(R.dimen.ts_20).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey)
                )
            }
        }
    }
}

@Composable
fun ElecView(electrolytes: Int?, level: ElectrolyteLevel?) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id =
                when (level) {
                    null -> R.drawable.pb_electrolytes_grey
                    ElectrolyteLevel.Balanced -> R.drawable.pb_electrolytes_blue
                    ElectrolyteLevel.Low, ElectrolyteLevel.High -> R.drawable.pb_electrolytes_red
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
                val value = electrolytes?.toString() ?: stringResource(id = R.string.default_value)
                Text(
                    fontFamily = MatTheme.fontFamilyBook,
                    text = value,
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
                    modifier = Modifier
                        .offset(y = (-4).dp),
                    fontFamily = MatTheme.fontFamilyBook,
                    text = if (electrolytes != null) stringResource(id = R.string.unit_electrolites) else "",
                    fontSize = dimensionResource(R.dimen.ts_16).value.sp,
                    color = colorResource(id = R.color.kamleon_secondary_grey_60)
                )
            }
        }
    }
}

@Composable
fun VolumeView(volume: Int?, isPrecise: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VolumeProgressIndicator(
            currentValue = volume,
            maxValue = 300,
            isPrecise = isPrecise,
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
                val value = volume?.toString() ?: stringResource(id = R.string.default_value)
                Text(
                    fontFamily = MatTheme.fontFamilyBook,
                    text = value,
                    fontSize = dimensionResource(R.dimen.ts_36).value.sp,
                    color = colorResource(id = R.color.kamleon_dark_grey),

                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier
                        .offset(y = (-4).dp),                        //.background(colorResource(id = R.color.color_red)),
                    fontFamily = MatTheme.fontFamilyBook,

                    text = if (volume != null) stringResource(id = R.string.unit_volume) else "",
                    fontSize = dimensionResource(R.dimen.ts_16).value.sp,
                    color = colorResource(id = R.color.kamleon_secondary_grey_60),
                )
            }
        }
    }
}
