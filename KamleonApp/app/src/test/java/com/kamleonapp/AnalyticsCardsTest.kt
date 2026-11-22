package com.kamleonapp

import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.models.analyticsCardsOf
import com.dynatech2012.kamleonuserapp.views.cards.AnalyticType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsCardsTest {
    private fun sample(score: Int, cond: Float, vol: Float, precise: Boolean = true) = MeasureData().apply {
        this.score = score
        msCond = cond
        this.vol = vol
        precision = if (precise) MeasurePrecision.Good else MeasurePrecision.Bad
    }

    @Test
    fun cardsComeInOrder() {
        assertEquals(
            listOf(AnalyticType.HYDRATION, AnalyticType.ELECTROLYTE, AnalyticType.VOLUME),
            analyticsCardsOf(null).map { it.type },
        )
    }

    @Test
    fun noSampleShowsNoData() {
        val (hydration, electrolytes, volume) = analyticsCardsOf(null)
        assertNull(hydration.value)
        assertEquals(R.string.analytic_no_data, hydration.subtitle)
        assertNull(hydration.description)
        assertEquals(R.string.analytic_no_data, electrolytes.subtitle)
        assertEquals(R.string.analytic_volume_insuff_title, volume.subtitle)
    }

    @Test
    fun aPreciseSampleIsJudgedOnEachMetric() {
        val (hydration, electrolytes, volume) = analyticsCardsOf(sample(score = 95, cond = 12f, vol = 300f))
        assertEquals(95, hydration.value)
        assertEquals(R.string.analytic_very_hydrated, hydration.subtitle)
        assertEquals(R.string.analytic_hydratation_above, hydration.description)
        assertEquals(12, electrolytes.value)
        assertEquals(R.string.analytic_elec_normal, electrolytes.subtitle)
        assertEquals(300, volume.value)
        assertEquals(R.string.analytic_volume_high, volume.subtitle)
        assertEquals(R.string.analytic_volume_good, volume.description)
    }

    @Test
    fun hydrationBandsKeepTheirLimits() {
        fun subtitleAt(score: Int) = analyticsCardsOf(sample(score, 10f, 200f))[0].subtitle
        assertEquals(R.string.analytic_severely_dehydrated, subtitleAt(33))
        assertEquals(R.string.analytic_dehydrated, subtitleAt(34))
        assertEquals(R.string.analytic_dehydrated, subtitleAt(66))
        assertEquals(R.string.analytic_hydrated, subtitleAt(67))
        assertEquals(R.string.analytic_hydrated, subtitleAt(90))
        assertEquals(R.string.analytic_very_hydrated, subtitleAt(91))
    }

    @Test
    fun anImpreciseSampleKeepsOnlyTheVolumeFigure() {
        val (hydration, electrolytes, volume) = analyticsCardsOf(sample(score = 95, cond = 12f, vol = 85f, precise = false))
        assertNull(hydration.value)
        assertNull(electrolytes.value)
        assertEquals(85, volume.value)
        assertEquals(false, volume.isPrecise)
        assertEquals(R.string.analytic_volume_insuff_title, volume.subtitle)
        assertEquals(R.string.analytic_volume_insuff_desc, volume.description)
    }
}
