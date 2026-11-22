package com.dynatech2012.kamleonuserapp.views.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.ElectrolyteLevel
import com.dynatech2012.kamleonuserapp.models.VolumeLevel

/**
 * The two other readings of the sample Home is showing: electrolytes and volume.
 * With no [measure] it shows "--" placeholders, so the layout keeps its shape while there is no data.
 */
@Composable
fun HomeMetricsCard(measure: MeasureData?, modifier: Modifier = Modifier) {
    val precise = measure?.isPrecise == true
    val electrolytes = if (precise) measure?.msCond?.toInt() else null
    val volume = if (precise) measure?.vol?.toInt() else null
    val volumeLevel = VolumeLevel.of(volume)
    val noValue = stringResource(R.string.default_value)

    // The card applies its modifier twice (shadow and body), so the margin goes on a wrapper.
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 8.dp)) {
    ClippedShadowCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = colorResource(id = R.color.color_fa_40),
        elevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Metric(
                title = stringResource(R.string.home_metric_electrolytes),
                value = electrolytes?.toString() ?: noValue,
                unit = stringResource(R.string.unit_electrolites),
                label = when (ElectrolyteLevel.of(electrolytes)) {
                    ElectrolyteLevel.Low -> stringResource(R.string.analytic_elec_hypo)
                    ElectrolyteLevel.Balanced -> stringResource(R.string.analytic_elec_normal)
                    ElectrolyteLevel.High -> stringResource(R.string.analytic_elec_hyper)
                    null -> stringResource(R.string.analytic_no_data)
                },
            )
            Metric(
                title = stringResource(R.string.home_metric_volume),
                value = volume?.toString() ?: noValue,
                unit = stringResource(R.string.unit_volume),
                label = when (volumeLevel) {
                    VolumeLevel.NoData -> stringResource(R.string.analytic_no_data)
                    VolumeLevel.Insufficient -> stringResource(R.string.analytic_volume_low)
                    VolumeLevel.Average -> stringResource(R.string.analytic_volume_medium)
                    VolumeLevel.High -> stringResource(R.string.analytic_volume_high)
                },
            )
        }
    }
    }
}

// Same faces as the rest of Home: Regular for figures and text (like the score in the ring), Medium for titles.
private val CircularMedium = FontFamily(Font(R.font.circular_pro_medium))
private val CircularRegular = FontFamily(Font(R.font.circular_pro_regular))

@Composable
private fun RowScope.Metric(title: String, value: String, unit: String, label: String) {
    val dark = colorResource(id = R.color.kamleon_dark_grey)
    Column(modifier = Modifier.weight(1f)) {
        Text(title, fontSize = 14.sp, fontFamily = CircularMedium, color = colorResource(id = R.color.kamleon_secondary_grey_70))
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(value, fontSize = 36.sp, fontFamily = CircularRegular, color = dark, modifier = Modifier.alignByBaseline())
            Text(unit, fontSize = 14.sp, fontFamily = CircularRegular, color = dark, modifier = Modifier.padding(start = 4.dp).alignByBaseline())
        }
        Text(label, fontSize = 14.sp, fontFamily = CircularRegular, color = dark)
    }
}
