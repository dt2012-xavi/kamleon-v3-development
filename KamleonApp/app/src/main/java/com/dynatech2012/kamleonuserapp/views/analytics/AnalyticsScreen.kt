package com.dynatech2012.kamleonuserapp.views.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.extensions.sampleTimeText
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.models.analyticsCardsOf
import com.dynatech2012.kamleonuserapp.models.analyticsNoteOf
import com.dynatech2012.kamleonuserapp.views.cards.AnalyticType
import com.dynatech2012.kamleonuserapp.views.cards.CardViewAnalyticsItem
import com.dynatech2012.kamleonuserapp.views.home.HomeProfile
import com.dynatech2012.kamleonuserapp.views.home.ProfileButton
import java.util.Date

/** How far the list stays above the bottom edge, so it clears the tab bar (and the navigation bar). */
private val TabBarClearance = 104.dp

/**
 * The Analytics tab: the last sample's hydration, electrolytes and volume as cards, each of which opens
 * its graph. Draws edge to edge, like Home.
 */
@Composable
fun AnalyticsScreen(
    measure: MeasureData?,
    profile: HomeProfile,
    onProfileClick: () -> Unit,
    onOpenGraph: (AnalyticType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fa = colorResource(R.color.color_fa)
    val gradient = Brush.linearGradient(
        colors = listOf(fa, fa, colorResource(R.color.bg_analytics_gradient_2)),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
    val cards = remember(measure) { analyticsCardsOf(measure) }
    val note = analyticsNoteOf(measure)
    val resources = LocalResources.current
    val lastSample = measure?.let { sample ->
        val date = remember(sample.analysisDate, resources) { Date(sample.analysisDate).sampleTimeText(resources) }
        stringResource(R.string.home_last_sample_message, date)
    } ?: stringResource(R.string.home_last_sample_message_default)
    // Applied at layout, not read during composition: a screen's first frame is composed before the
    // window insets reach it, and a value read then would be 0 and jump a frame later.
    val bottomClearance = Modifier
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        .padding(bottom = TabBarClearance)

    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
        ) {
            // The list starts right under the profile button, as in the old layout.
            Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                ProfileButton(
                    profile = profile,
                    onClick = onProfileClick,
                    modifier = Modifier.padding(end = 20.dp).align(Alignment.CenterEnd),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(bottomClearance)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    text = lastSample.uppercase(),
                    fontSize = 14.sp,
                    fontFamily = MatTheme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.kamleon_secondary_grey_50),
                    modifier = Modifier.padding(horizontal = 6.dp).padding(top = 4.dp, bottom = 8.dp),
                )
                cards.forEach { card ->
                    CardViewAnalyticsItem(
                        analyticType = card.type,
                        subtitle = stringResource(card.subtitle),
                        description = card.description?.let { stringResource(it) } ?: "",
                        value = card.value,
                        isPrecise = card.isPrecise,
                        electrolyteLevel = card.electrolyteLevel,
                        onClick = { onOpenGraph(card.type) },
                    )
                }
                // Below the cards, so the cards keep their place whatever the sample.
                if (note != null) {
                    Text(
                        text = stringResource(note),
                        fontSize = 13.sp,
                        fontFamily = MatTheme.fontFamily,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.kamleon_secondary_grey_50),
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp).testTag("analytics_note"),
                    )
                }
            }
        }
    }
}

// Previews: a sample, an imprecise sample, and no sample yet.

private fun previewSample(precise: Boolean = true) = MeasureData().apply {
    analysisDate = 1738281600000L
    score = 100
    vol = 85f
    msCond = 2f
    precision = if (precise) MeasurePrecision.Good else MeasurePrecision.Bad
}

@Composable
private fun PreviewAnalytics(measure: MeasureData?) =
    AnalyticsScreen(measure, HomeProfile(initials = "5U"), onProfileClick = {}, onOpenGraph = {})

@Preview(name = "Sample", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AnalyticsSamplePreview() = PreviewAnalytics(previewSample())

@Preview(name = "Imprecise sample", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AnalyticsImprecisePreview() = PreviewAnalytics(previewSample(precise = false))

@Preview(name = "No sample", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AnalyticsNoSamplePreview() = PreviewAnalytics(null)
