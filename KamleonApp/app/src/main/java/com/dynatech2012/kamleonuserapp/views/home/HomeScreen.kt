package com.dynatech2012.kamleonuserapp.views.home

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.HomeContent
import com.dynatech2012.kamleonuserapp.models.HydrationLevel
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.views.cards.HomeMetricsCard
import java.util.Date

/** How far the content stays above the bottom edge, so it clears the tab bar (and the navigation bar). */
private val TabBarClearance = 128.dp

/**
 * The ring at its designed size (as the old XML drew it); smaller screens scale it, and the score inside
 * it, down. Its figures use Circular Regular: the XML screens look Regular too, because the theme's
 * AppCompat fontFamily wins over the "Book" styles.
 */
private val RingSize = 232.dp

/**
 * The Home screen: a hydration gradient behind a header, then either the last sample (score ring and
 * metrics card) or, when there is none, an illustration that says why. Draws edge to edge: the gradient
 * fills the screen, while the content stays clear of the system bars.
 */
@Composable
fun HomeScreen(
    content: HomeContent,
    profile: HomeProfile,
    hasNotifications: Boolean,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onScan: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = homeLookOf(content)
    val gradient = Brush.linearGradient(
        colors = listOf(colorResource(look.start), colorResource(look.end)),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
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
            HomeHeader(
                profile = profile,
                hasNotifications = hasNotifications,
                onProfileClick = onProfileClick,
                onNotificationsClick = onNotificationsClick,
            )
            when (content) {
                HomeContent.Empty, HomeContent.Unavailable -> HomeEmptyState(
                    unavailable = content == HomeContent.Unavailable,
                    onScan = onScan,
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f).then(bottomClearance),
                )
                else -> HomeSample(
                    content = content,
                    look = look,
                    modifier = Modifier.weight(1f).then(bottomClearance),
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    profile: HomeProfile,
    hasNotifications: Boolean,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.kamleon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = Modifier.padding(start = 18.dp).height(36.dp).align(Alignment.CenterStart),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 20.dp).align(Alignment.CenterEnd),
        ) {
            NotificationsButton(hasNotifications = hasNotifications, onClick = onNotificationsClick)
            ProfileButton(profile = profile, onClick = onProfileClick)
        }
    }
}

/** The last sample (or the placeholders while it loads): date, verdict, score ring and metrics card. */
@Composable
private fun HomeSample(content: HomeContent, look: HomeLook, modifier: Modifier = Modifier) {
    val measure = (content as? HomeContent.Sample)?.measure
    val precise = measure?.isPrecise == true
    val textColor = colorResource(if (look.lightText) R.color.color_fa else R.color.kamleon_dark_grey)
    val lastSample = measure?.let { sample ->
        val date = remember(sample.analysisDate) { Date(sample.analysisDate).formatTime }
        stringResource(R.string.home_last_sample_message, date)
    }?.uppercase() ?: stringResource(R.string.home_last_sample_message_default).uppercase()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = lastSample,
            fontSize = 14.sp,
            fontFamily = MatTheme.fontFamily,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.kamleon_dark_grey),
            modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 16.dp),
        )
        Text(
            text = stringResource(look.message),
            fontSize = 32.sp,
            fontFamily = MatTheme.fontFamily,
            color = textColor,
            modifier = Modifier.padding(horizontal = 26.dp).padding(top = 2.dp),
        )
        // The ring takes the room left between the message and the card, up to its designed size.
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
        ) {
            ScoreRing(
                score = measure?.takeIf { precise }?.score,
                size = minOf(RingSize, maxWidth, maxHeight),
            )
        }
        HomeMetricsCard(measure)
    }
}

/** The hydration score inside a ring that fills up to it. Without a score it is an empty ring with "--". */
@Composable
private fun ScoreRing(score: Int?, size: Dp) {
    val scale = size / RingSize
    val dark = colorResource(R.color.kamleon_dark_grey)
    val description = score?.let { stringResource(R.string.home_score_description, it) }
        ?: stringResource(R.string.analytic_no_data)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clearAndSetSemantics { contentDescription = description },
    ) {
        CircularProgressIndicator(
            progress = { (score ?: 0) / 100f },
            modifier = Modifier.fillMaxSize(),
            color = colorResource(R.color.white),
            trackColor = colorResource(R.color.white40),
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
        Row {
            Text(
                text = score?.toString() ?: stringResource(R.string.default_value),
                fontSize = (82 * scale).sp,
                fontFamily = MatTheme.fontFamily,
                color = dark,
                modifier = Modifier.alignByBaseline(),
            )
            if (score != null) {
                Text(
                    text = stringResource(R.string.unit_percentage),
                    fontSize = (30 * scale).sp,
                    fontFamily = MatTheme.fontFamily,
                    color = dark,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

/** No sample to show: an illustration, what happened, and the action that fixes it (scan or retry). */
@Composable
private fun HomeEmptyState(
    unavailable: Boolean,
    onScan: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp),
    ) {
        Image(painter = painterResource(R.drawable.img_no_data), contentDescription = null)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(if (unavailable) R.string.home_status_unavailable else R.string.home_status_empty),
            fontSize = 32.sp,
            fontFamily = MatTheme.fontFamily,
            color = colorResource(R.color.kamleon_dark_grey),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(if (unavailable) R.string.home_unavailable_body else R.string.home_empty_body),
            fontSize = 16.sp,
            fontFamily = MatTheme.fontFamily,
            color = colorResource(R.color.kamleon_secondary_grey_70),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = if (unavailable) onRetry else onScan,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.kamleon_blue),
                contentColor = colorResource(R.color.white),
            ),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier.height(36.dp).widthIn(min = 160.dp),
        ) {
            Text(
                text = stringResource(if (unavailable) R.string.retry else R.string.home_empty_scan),
                fontSize = 16.sp,
                fontFamily = MatTheme.fontFamily,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** The colors, and the verdict, a state of Home is shown with. */
private class HomeLook(
    @ColorRes val start: Int,
    @ColorRes val end: Int,
    @StringRes val message: Int,
    /** Light text on a strong gradient; dark text on the grey ones. */
    val lightText: Boolean,
)

private fun homeLookOf(content: HomeContent): HomeLook {
    val measure = (content as? HomeContent.Sample)?.measure
        ?: return HomeLook(R.color.color_fa, R.color.home_bg_no_measure_end, R.string.home_status_loading, lightText = false)
    if (!measure.isPrecise) {
        return HomeLook(R.color.color_fa, R.color.bg_analytics_gradient_2, R.string.analytic_volume_low, lightText = false)
    }
    return when (measure.hydrationLevel) {
        HydrationLevel.VeryHydrated -> HomeLook(R.color.home_bg_very_hydrated_start, R.color.home_bg_very_hydrated_end, R.string.analytic_very_hydrated, lightText = true)
        HydrationLevel.Hydrated -> HomeLook(R.color.home_bg_hydrated_start, R.color.home_bg_hydrated_end, R.string.analytic_hydrated, lightText = true)
        HydrationLevel.Dehydrated -> HomeLook(R.color.home_bg_dehydrated_start, R.color.home_bg_dehydrated_end, R.string.analytic_dehydrated, lightText = true)
        HydrationLevel.SeverelyDehydrated -> HomeLook(R.color.home_bg_severely_dehydrated_start, R.color.home_bg_severely_dehydrated_end, R.string.analytic_severely_dehydrated, lightText = true)
    }
}

// Previews: one per state, so a layout change can be checked without a device.

private val previewProfile = HomeProfile(initials = "5U")

private fun previewSample(score: Int, precise: Boolean = true) = MeasureData().apply {
    analysisDate = 1738281600000L
    this.score = score
    vol = 85f
    msCond = 2f
    precision = if (precise) MeasurePrecision.Good else MeasurePrecision.Bad
}

@Composable
private fun PreviewHome(content: HomeContent) =
    HomeScreen(
        content, previewProfile, hasNotifications = true,
        onProfileClick = {}, onNotificationsClick = {}, onScan = {}, onRetry = {},
    )

@Preview(name = "Very hydrated", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeVeryHydratedPreview() = PreviewHome(HomeContent.Sample(previewSample(100)))

@Preview(name = "Dehydrated", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeDehydratedPreview() = PreviewHome(HomeContent.Sample(previewSample(50)))

@Preview(name = "Low volume", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeLowVolumePreview() = PreviewHome(HomeContent.Sample(previewSample(0, precise = false)))

@Preview(name = "Loading", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeLoadingPreview() = PreviewHome(HomeContent.Loading)

@Preview(name = "No samples yet", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeEmptyPreview() = PreviewHome(HomeContent.Empty)

@Preview(name = "Data unavailable", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeUnavailablePreview() = PreviewHome(HomeContent.Unavailable)

@Preview(name = "Short screen", showBackground = true, widthDp = 393, heightDp = 640)
@Composable
private fun HomeShortScreenPreview() = PreviewHome(HomeContent.Sample(previewSample(100)))
