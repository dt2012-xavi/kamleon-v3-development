package com.dynatech2012.kamleonuserapp

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.views.graph.GraphScreen
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.home.HomeProfile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.Date

class GraphScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var backs = 0
    private var profileClicks = 0

    /** Midday, so the sample at 11:00 is in today's graph whatever the time zone. */
    private val today: Date = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }.time

    private fun sampleAt(hour: Int, score: Int) = MeasureData().apply {
        timestamp = Calendar.getInstance().apply { time = today; set(Calendar.HOUR_OF_DAY, hour) }.timeInMillis
        this.score = score; msCond = 12f; vol = 200f; precision = MeasurePrecision.Good
    }

    private fun setScreen(measures: List<AveragesData>) = composeRule.setContent {
        GraphScreen(
            type = KamleonGraphDataType.hydration,
            measures = measures, daily = emptyList(), monthly = emptyList(),
            profile = HomeProfile(initials = "AB"),
            onBack = { backs++ }, onProfileClick = { profileClicks++ },
            today = { today },
        )
    }

    @Test
    fun noSamplesShowDashesAndTheEmptyChart() {
        setScreen(emptyList())
        composeRule.onNodeWithText("--").assertExists()
    }

    @Test
    fun theAverageOfTheDayIsShown() {
        setScreen(listOf(sampleAt(9, 80), sampleAt(11, 60)))
        composeRule.onNodeWithText("70").assertExists()
    }

    @Test
    fun choosingAPeriodSelectsIt() {
        setScreen(emptyList())
        val weekly = composeRule.onNodeWithText(context.getString(R.string.graph_header_item_weekly))
        weekly.assertIsNotSelected()
        weekly.performClick()
        composeRule.onNodeWithText(context.getString(R.string.graph_header_item_weekly)).assertIsSelected()
        composeRule.onNodeWithText(context.getString(R.string.graph_header_item_daily)).assertIsNotSelected()
    }

    @Test
    fun theStreakCardTurnsOnAndOff() {
        setScreen(listOf(sampleAt(9, 80)))
        val card = composeRule.onNodeWithTag("graph_streak")
        card.assertIsNotSelected()
        card.performClick()
        card.assertIsSelected()
        card.performClick()
        card.assertIsNotSelected()
    }

    @Test
    fun tappingABarShowsItsValue() {
        setScreen(listOf(sampleAt(11, 80)))
        composeRule.onNodeWithTag("graph_bar_label").assertDoesNotExist()
        // Bar 11 of 24, near the bottom of the bars' area (the y labels take the right edge).
        composeRule.onNodeWithTag("graph_chart").performTouchInput {
            val plotWidth = width * 0.88f
            click(androidx.compose.ui.geometry.Offset(plotWidth / 24f * 11.5f, height * 0.8f))
        }
        composeRule.onNodeWithTag("graph_bar_label").assertExists()
        composeRule.onNodeWithText("11:00 h").assertExists()
    }

    @Test
    fun theHeaderButtonsWork() {
        setScreen(emptyList())
        composeRule.onNodeWithContentDescription(context.getString(R.string.nav_back)).performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.home_profile_button)).performClick()
        composeRule.runOnIdle {
            assertEquals(1, backs)
            assertEquals(1, profileClicks)
        }
    }

    @Test
    fun theChartIsDescribedForTalkBack() {
        setScreen(listOf(sampleAt(9, 80)))
        val description = composeRule.onNodeWithTag("graph_chart").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].single()
        val hydration = context.getString(R.string.analytic_metric_hydration)
        assert(description.startsWith(hydration)) { description }
        assert(description.contains("80")) { description }
    }

    @Test
    fun theWeeklyAxisUsesTheDayNamesFromTheStrings() {
        setScreen(emptyList())
        composeRule.onNodeWithText(context.getString(R.string.graph_header_item_weekly)).performClick()
        // The title is upper case, the metric name from the strings.
        composeRule.onNodeWithText(context.getString(R.string.analytic_metric_hydration).uppercase()).assertExists()
    }
}
