package com.kamleonapp

import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphBarDrawData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataBuilder
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphViewMode
import com.dynatech2012.kamleonuserapp.views.graph.data.StreakCount
import com.dynatech2012.kamleonuserapp.views.graph.data.averageText
import com.dynatech2012.kamleonuserapp.views.graph.data.barLabel
import com.dynatech2012.kamleonuserapp.views.graph.data.barSamples
import com.dynatech2012.kamleonuserapp.views.graph.data.countsForStreak
import com.dynatech2012.kamleonuserapp.views.graph.data.isTappable
import com.dynatech2012.kamleonuserapp.views.graph.data.streakCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KamleonGraphBarsTest {

    private class Item(override var timestamp: Long, val value: Double, override val isPrecise: Boolean = true) : AveragesData {
        override fun scoreValue(type: KamleonGraphDataType): Double = value
    }

    /** Wednesday 18 June 2025, 15:30 local time. */
    private val today: Date = at(2025, Calendar.JUNE, 18, 15, 30)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Date =
        Calendar.getInstance().apply { clear(); set(year, month, day, hour, minute) }.time

    private fun item(date: Date, value: Double, precise: Boolean = true) = Item(date.time, value, precise)

    private fun build(
        mode: KamleonGraphViewMode,
        items: List<AveragesData>,
        type: KamleonGraphDataType = KamleonGraphDataType.hydration,
    ): KamleonGraphBarDrawData = KamleonGraphDataBuilder.build(today, mode, type, items, "%")

    @Test
    fun barsAreLabelledByTheirSpan() {
        assertEquals("9:00 h", build(KamleonGraphViewMode.Daily, emptyList()).barLabel(9))
        // The week ends today, Wednesday: it starts on Thursday.
        assertEquals("Thu", build(KamleonGraphViewMode.Weekly, emptyList()).barLabel(0))
        assertEquals("Wed", build(KamleonGraphViewMode.Weekly, emptyList()).barLabel(6))
        // The year ends in June: it starts in July of the year before.
        assertEquals("Jul", build(KamleonGraphViewMode.Yearly, emptyList()).barLabel(0))
        assertEquals("Jun", build(KamleonGraphViewMode.Yearly, emptyList()).barLabel(11))
    }

    @Test
    fun theAverageOnlyCountsPreciseBars() {
        val data = build(
            KamleonGraphViewMode.Daily,
            listOf(item(at(2025, Calendar.JUNE, 18, 8), 80.0), item(at(2025, Calendar.JUNE, 18, 10), 60.0), item(at(2025, Calendar.JUNE, 18, 12), 10.0, precise = false)),
        )
        assertEquals("70", data.averageText())
    }

    @Test
    fun electrolytesAverageKeepsADecimal() {
        val data = build(
            KamleonGraphViewMode.Daily,
            listOf(item(at(2025, Calendar.JUNE, 18, 8), 12.0), item(at(2025, Calendar.JUNE, 18, 9), 13.0)),
            KamleonGraphDataType.electrolytes,
        )
        assertEquals(String.format(Locale.getDefault(), "%.1f", 12.5), data.averageText())
    }

    @Test
    fun withoutPreciseBarsTheAverageIsDashes() {
        assertEquals("--", build(KamleonGraphViewMode.Daily, emptyList()).averageText())
        assertEquals("--", build(KamleonGraphViewMode.Daily, listOf(item(at(2025, Calendar.JUNE, 18, 8), 80.0, precise = false))).averageText())
    }

    @Test
    fun theStreakCountsOptimumPreciseBars() {
        val data = build(
            KamleonGraphViewMode.Daily,
            listOf(
                item(at(2025, Calendar.JUNE, 18, 8), 80.0),
                item(at(2025, Calendar.JUNE, 18, 9), 50.0),
                item(at(2025, Calendar.JUNE, 18, 10), 90.0, precise = false),
            ),
        )
        assertEquals(StreakCount(optimum = 1, total = 2), data.streakCount())
        assertTrue(data.countsForStreak(8))
        assertFalse(data.countsForStreak(9))
        assertFalse(data.countsForStreak(10))
    }

    @Test
    fun volumeHasLimitsInsteadOfAStreak() {
        val data = build(
            KamleonGraphViewMode.Daily,
            listOf(item(at(2025, Calendar.JUNE, 18, 8), 100.0), item(at(2025, Calendar.JUNE, 18, 9), 200.0), item(at(2025, Calendar.JUNE, 18, 10), 150.0)),
            KamleonGraphDataType.volume,
        )
        assertNull(data.streakCount())
        assertTrue(data.countsForStreak(8))
        assertTrue(data.countsForStreak(9))
        assertFalse(data.countsForStreak(10))
    }

    @Test
    fun onlyBarsWithAHeightCanBeTapped() {
        val data = build(KamleonGraphViewMode.Daily, listOf(item(at(2025, Calendar.JUNE, 18, 8), 80.0)))
        assertTrue(data.isTappable(8))
        assertFalse(data.isTappable(9))
    }

    @Test
    fun aBarsSamplesComeNewestFirst() {
        val early = item(at(2025, Calendar.JUNE, 18, 8, 5), 80.0)
        val late = item(at(2025, Calendar.JUNE, 18, 8, 40), 70.0)
        val data = build(KamleonGraphViewMode.Daily, listOf(early, late))
        assertEquals(listOf(late.timestamp, early.timestamp), data.barSamples(8).map { it.timestamp })
    }
}
