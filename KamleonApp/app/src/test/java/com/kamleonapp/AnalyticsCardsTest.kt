package com.kamleonapp

import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.ElectrolyteLevel
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.models.analyticsCardsOf
import com.dynatech2012.kamleonuserapp.models.analyticsNoteOf
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
        assertEquals(R.string.analytic_hydratation_optimal, hydration.description)
        assertEquals(12, electrolytes.value)
        assertEquals(R.string.analytic_elec_normal, electrolytes.subtitle)
        assertEquals(300, volume.value)
        assertEquals(R.string.analytic_volume_high, volume.subtitle)
        assertEquals(R.string.analytic_volume_good, volume.description)
    }

    @Test
    fun hydrationBands() {
        fun card(score: Int) = analyticsCardsOf(sample(score, 10f, 200f))[0]
        // 1-32 severely dehydrated, 33-66 dehydrated, 67-90 hydrated, 91+ very well hydrated.
        listOf(
            1 to R.string.analytic_severely_dehydrated, 32 to R.string.analytic_severely_dehydrated,
            33 to R.string.analytic_dehydrated, 66 to R.string.analytic_dehydrated,
            67 to R.string.analytic_hydrated, 90 to R.string.analytic_hydrated,
            91 to R.string.analytic_very_hydrated, 99 to R.string.analytic_very_hydrated,
        ).forEach { (score, subtitle) -> assertEquals("score $score", subtitle, card(score).subtitle) }
        assertEquals(R.string.analytic_hydratation_under, card(66).description)
        assertEquals(R.string.analytic_hydratation_optimal, card(67).description)
        assertEquals(R.string.analytic_hydratation_optimal, card(90).description)
        assertEquals(R.string.analytic_hydratation_optimal, card(91).description)
    }

    @Test
    fun electrolytesAreJudgedOnTheWholeNumberShown() {
        fun card(cond: Float) = analyticsCardsOf(sample(80, cond, 200f))[1]
        assertEquals(25, card(25.7f).value)
        assertEquals(R.string.analytic_elec_normal, card(25.7f).subtitle)
        assertEquals(ElectrolyteLevel.Balanced, card(25.7f).electrolyteLevel)
        assertEquals(R.string.analytic_elec_hyper, card(26f).subtitle)
        assertEquals(4, card(4.9f).value)
        assertEquals(R.string.analytic_elec_hypo, card(4.9f).subtitle)
        assertEquals(R.string.analytic_elec_normal, card(5f).subtitle)
        assertEquals(R.string.analytic_elec_normal, card(25f).subtitle)
    }

    @Test
    fun volumeBandsHaveTheirOptimumBetween150And250() {
        fun card(vol: Float) = analyticsCardsOf(sample(80, 10f, vol))[2]
        assertEquals(R.string.analytic_volume_low, card(149f).subtitle)
        assertEquals(R.string.analytic_volume_insuff_desc, card(149f).description)
        assertEquals(R.string.analytic_volume_medium, card(150f).subtitle)
        assertEquals(R.string.analytic_volume_medium, card(250f).subtitle)
        assertEquals(R.string.analytic_volume_good, card(250f).description)
        assertEquals(R.string.analytic_volume_high, card(251f).subtitle)
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

    @Test
    fun noSampleAsksForAScan() {
        assertEquals(R.string.analytic_note_no_sample, analyticsNoteOf(null))
    }

    @Test
    fun anImpreciseSampleExplainsTheMissingMetrics() {
        assertEquals(R.string.analytic_note_imprecise, analyticsNoteOf(sample(80, 12f, 40f, precise = false)))
    }

    @Test
    fun aPreciseSampleHasNoNote() {
        assertNull(analyticsNoteOf(sample(80, 12f, 200f)))
    }
}
