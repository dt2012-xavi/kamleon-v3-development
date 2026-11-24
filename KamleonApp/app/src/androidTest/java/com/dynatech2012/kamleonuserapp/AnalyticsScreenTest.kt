package com.dynatech2012.kamleonuserapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.views.analytics.AnalyticsScreen
import com.dynatech2012.kamleonuserapp.views.cards.AnalyticType
import com.dynatech2012.kamleonuserapp.views.home.HomeProfile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnalyticsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val opened = mutableListOf<AnalyticType>()
    private var profileClicks = 0

    private fun setScreen(measure: MeasureData?) = composeRule.setContent {
        AnalyticsScreen(
            measure = measure,
            profile = HomeProfile(initials = "AB"),
            onProfileClick = { profileClicks++ },
            onOpenGraph = { opened += it },
        )
    }

    private val sample = MeasureData().apply {
        score = 95; msCond = 12f; vol = 300f; precision = MeasurePrecision.Good
    }

    @Test
    fun aSampleShowsAVerdictPerMetric() {
        setScreen(sample)
        composeRule.onNodeWithText(context.getString(R.string.analytic_very_hydrated)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.analytic_elec_normal)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.analytic_volume_high)).assertIsDisplayed()
    }

    @Test
    fun eachCardOpensItsGraph() {
        setScreen(sample)
        composeRule.onNodeWithText(context.getString(R.string.analytic_very_hydrated)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.analytic_elec_normal)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.analytic_volume_high)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(AnalyticType.HYDRATION, AnalyticType.ELECTROLYTE, AnalyticType.VOLUME), opened)
        }
    }

    @Test
    fun theProfileButtonOpensSettings() {
        setScreen(null)
        composeRule.onNodeWithContentDescription(context.getString(R.string.home_profile_button)).performClick()
        composeRule.runOnIdle { assertEquals(1, profileClicks) }
    }
}
