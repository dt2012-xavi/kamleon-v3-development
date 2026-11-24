package com.dynatech2012.kamleonuserapp.views.graph

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme
import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.views.HeaderBackButton
import com.dynatech2012.kamleonuserapp.views.graph.data.GraphNames
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphBarDrawData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataBuilder
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphViewMode
import com.dynatech2012.kamleonuserapp.views.graph.data.averageText
import com.dynatech2012.kamleonuserapp.views.graph.data.periodText
import com.dynatech2012.kamleonuserapp.views.graph.data.streakCount
import com.dynatech2012.kamleonuserapp.views.home.HomeProfile
import com.dynatech2012.kamleonuserapp.views.home.ProfileButton
import java.util.Date

/** Left and right margin of everything on the screen, as in the headers of the other screens. */
private val ScreenMargin = 20.dp

/**
 * The graph of one metric ([type]) over a day, a week, a month or a year, with its average and the
 * streak. [measures] feed the daily graph, [daily] the weekly and monthly ones, [monthly] the yearly one.
 */
@Composable
fun GraphScreen(
    type: KamleonGraphDataType,
    measures: List<AveragesData>,
    daily: List<AveragesData>,
    monthly: List<AveragesData>,
    profile: HomeProfile,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    today: () -> Date = { Date() },
) {
    var mode by rememberSaveable { mutableStateOf(KamleonGraphViewMode.Daily) }
    var streak by rememberSaveable { mutableStateOf(false) }
    val percentUnit = stringResource(R.string.unit_percentage)
    val names = graphNames()
    val data = remember(mode, type, measures, daily, monthly, names) {
        val source = KamleonGraphDataBuilder.sourceFor(mode, measures, daily, monthly)
        KamleonGraphDataBuilder.build(today(), mode, type, source, percentUnit, names)
    }
    val fa = colorResource(R.color.color_fa)
    val gradient = Brush.linearGradient(
        colors = listOf(fa, fa, colorResource(R.color.bg_analytics_gradient_2)),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
    ) {
        GraphHeader(title = stringResource(type.title()).uppercase(), profile = profile, onBack = onBack, onProfileClick = onProfileClick)
        PeriodSelector(mode = mode, onSelect = { mode = it }, modifier = Modifier.padding(top = 8.dp))
        AverageSummary(data = data, modifier = Modifier.padding(start = ScreenMargin, top = 12.dp, bottom = 10.dp))
        GraphChart(
            data = data,
            streak = streak,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = ScreenMargin),
        )
        StreakToggle(
            data = data,
            on = streak,
            onToggle = { streak = !streak },
            modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp).padding(bottom = 8.dp),
        )
    }
}

/** The day and month names and the hour format from the app's strings. */
@Composable
private fun graphNames(): GraphNames {
    val days = stringArrayResource(R.array.graph_day_names)
    val months = stringArrayResource(R.array.graph_month_names)
    val hour = stringResource(R.string.graph_bar_hour)
    return remember(days, months, hour) { GraphNames(days.toList(), months.toList(), hour) }
}

/** Back on the left, the metric in the middle, the profile on the right. */
@Composable
private fun GraphHeader(title: String, profile: HomeProfile, onBack: () -> Unit, onProfileClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(top = 24.dp, bottom = 8.dp)
    ) {
        HeaderBackButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
        Text(
            text = title,
            fontSize = 16.sp,
            fontFamily = MatTheme.fontFamily,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.kamleon_secondary_grey_50),
            modifier = Modifier.align(Alignment.Center),
        )
        ProfileButton(profile = profile, onClick = onProfileClick, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

/** Daily, weekly, monthly and yearly; the chosen one sits on a teal pill. */
@Composable
private fun PeriodSelector(mode: KamleonGraphViewMode, onSelect: (KamleonGraphViewMode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        KamleonGraphViewMode.entries.forEach { option ->
            val selected = option == mode
            val pill = RoundedCornerShape(17.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .height(34.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(30.dp)
                        .then(
                            if (selected) Modifier.shadow(2.dp, pill).background(colorResource(R.color.kmln_graph_color_active), pill)
                            else Modifier
                        )
                        .clip(pill)
                        .clickable(role = Role.Tab) { if (!selected) onSelect(option) }
                        .semantics { this.selected = selected }
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = stringResource(option.displayName()),
                        fontSize = 12.sp,
                        fontFamily = MatTheme.fontFamily,
                        color = colorResource(
                            if (selected) R.color.kmln_graph_header_item_text_active else R.color.kmln_graph_header_item_text_normal
                        ),
                    )
                }
            }
        }
    }
}

/** The period's average with its unit, and the dates it covers. */
@Composable
private fun AverageSummary(data: KamleonGraphBarDrawData, modifier: Modifier = Modifier) {
    val grey = colorResource(R.color.kmln_graph_color_grey)
    val black = colorResource(R.color.kmln_graph_color_black)
    Column(modifier = modifier.testTag("graph_average")) {
        Text(stringResource(R.string.graph_header_average), color = grey, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = MatTheme.fontFamily)
        Row {
            Text(data.averageText(), color = black, fontSize = 32.sp, fontFamily = MatTheme.fontFamily, modifier = Modifier.alignByBaseline())
            Text(stringResource(data.type.getUnit()), color = black, fontSize = 20.sp, fontFamily = MatTheme.fontFamily, modifier = Modifier.alignByBaseline())
        }
        Text(data.periodText(), color = grey, fontSize = 14.sp, fontFamily = MatTheme.fontFamily)
    }
}

/**
 * The streak (hydration, electrolytes) or limits (volume) card: tapping it highlights on the chart the
 * bars that count, and turns the card teal.
 */
@Composable
private fun StreakToggle(data: KamleonGraphBarDrawData, on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val active = colorResource(R.color.kmln_graph_color_active)
    val white = colorResource(R.color.kmln_graph_color_white)
    val accent = if (on) white else active
    val count = data.streakCount()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(3.dp, shape)
            .background(if (on) active else white, shape)
            .clip(shape)
            .clickable(role = Role.Switch, onClick = onToggle)
            .semantics { selected = on }
            .testTag("graph_streak")
            .padding(horizontal = 18.dp),
    ) {
        Image(
            painter = painterResource(if (on) R.drawable.ic_eye_open else R.drawable.ic_eye_crossed),
            contentDescription = null,
            colorFilter = ColorFilter.tint(accent),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(if (data.type == KamleonGraphDataType.volume) R.string.graph_footer_limit else R.string.graph_footer_streak),
            fontSize = 16.sp,
            fontFamily = MatTheme.fontFamily,
            color = if (on) white else colorResource(R.color.black),
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        if (count != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(count.optimum.toString(), color = accent, fontSize = 33.sp, fontFamily = MatTheme.fontFamily, modifier = Modifier.alignByBaseline())
                Text(
                    stringResource(R.string.graph_footer_number_all, count.total.toString()),
                    color = accent, fontSize = 17.sp, fontFamily = MatTheme.fontFamily, modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

// Previews: a week of hydration, and electrolytes with no sample.

private fun previewDays(): List<AveragesData> = (0 until 7).map { day ->
    MeasureData().apply {
        timestamp = System.currentTimeMillis() - day * 86_400_000L
        score = 40 + day * 9
        msCond = 10f + day
        vol = 120f + day * 20
        precision = if (day == 3) MeasurePrecision.Bad else MeasurePrecision.Good
    }
}

@Preview(name = "Hydration", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GraphHydrationPreview() = GraphScreen(
    KamleonGraphDataType.hydration, previewDays(), previewDays(), emptyList(),
    HomeProfile(initials = "5U"), onBack = {}, onProfileClick = {},
)

@Preview(name = "No samples", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GraphEmptyPreview() = GraphScreen(
    KamleonGraphDataType.electrolytes, emptyList(), emptyList(), emptyList(),
    HomeProfile(initials = "5U"), onBack = {}, onProfileClick = {},
)
