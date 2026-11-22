package com.dynatech2012.kamleonuserapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.HomeContent
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.views.home.HomeProfile
import com.dynatech2012.kamleonuserapp.views.home.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var profileClicks = 0
    private var scans = 0
    private var retries = 0

    private fun sample(score: Int, precise: Boolean = true) = MeasureData().apply {
        this.score = score
        vol = 300f
        msCond = 12f
        precision = if (precise) MeasurePrecision.Good else MeasurePrecision.Bad
    }

    private fun setScreen(content: HomeContent) = composeRule.setContent {
        HomeScreen(
            content = content,
            profile = HomeProfile(initials = "AB"),
            onProfileClick = { profileClicks++ },
            onScan = { scans++ },
            onRetry = { retries++ },
        )
    }

    @Test
    fun aSampleShowsItsScoreVerdictAndMetrics() {
        setScreen(HomeContent.Sample(sample(100)))
        composeRule.onNodeWithContentDescription(context.getString(R.string.home_score_description, 100)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.analytic_very_hydrated)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.home_metric_electrolytes)).assertIsDisplayed()
    }

    @Test
    fun aLowVolumeSampleHasNoScore() {
        setScreen(HomeContent.Sample(sample(80, precise = false)))
        composeRule.onNodeWithContentDescription(context.getString(R.string.analytic_no_data)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.analytic_volume_low)).assertIsDisplayed()
    }

    @Test
    fun loadingKeepsTheRingAndCardAsPlaceholders() {
        setScreen(HomeContent.Loading)
        composeRule.onNodeWithText(context.getString(R.string.home_status_loading)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.analytic_no_data)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.home_metric_volume)).assertIsDisplayed()
    }

    @Test
    fun noSamplesYetOffersTheScanner() {
        setScreen(HomeContent.Empty)
        composeRule.onNodeWithText(context.getString(R.string.home_status_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.home_empty_scan)).performClick()
        composeRule.runOnIdle {
            assertEquals(1, scans)
            assertEquals(0, retries)
        }
    }

    @Test
    fun unavailableDataOffersRetry() {
        setScreen(HomeContent.Unavailable)
        composeRule.onNodeWithText(context.getString(R.string.home_status_unavailable)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        composeRule.runOnIdle {
            assertEquals(1, retries)
            assertEquals(0, scans)
        }
    }

    @Test
    fun theProfileButtonOpensSettings() {
        setScreen(HomeContent.Loading)
        composeRule.onNodeWithContentDescription(context.getString(R.string.home_profile_button)).performClick()
        composeRule.runOnIdle { assertEquals(1, profileClicks) }
    }
}
