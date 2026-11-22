package com.kamleonapp

import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphBarDrawData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataBuilder
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class KamleonGraphDataBuilderTest {

    /** A sample or an average: the same value for every metric, so tests can pick any type. */
    private class Item(override var timestamp: Long, val value: Double, override val isPrecise: Boolean = true) : AveragesData {
        override fun scoreValue(type: KamleonGraphDataType): Double = value
    }

    /** Wednesday 18 June 2025, 15:30 local time: the day the graphs are drawn for. */
    private val today: Date = at(2025, Calendar.JUNE, 18, 15, 30)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Date =
        Calendar.getInstance().apply { clear(); set(year, month, day, hour, minute) }.time

    private fun item(date: Date, value: Double, precise: Boolean = true) = Item(date.time, value, precise)

    private fun build(
        mode: KamleonGraphViewMode,
        items: List<AveragesData>,
        type: KamleonGraphDataType = KamleonGraphDataType.hydration,
        on: Date = today,
    ): KamleonGraphBarDrawData = KamleonGraphDataBuilder.build(on, mode, type, items, "%")

    private fun KamleonGraphBarDrawData.bar(index: Int) = values[index].y

    // Bars per mode

    @Test
    fun eachModeHasItsNumberOfBars() {
        assertEquals(24, build(KamleonGraphViewMode.Daily, emptyList()).values.size)
        assertEquals(7, build(KamleonGraphViewMode.Weekly, emptyList()).values.size)
        assertEquals(30, build(KamleonGraphViewMode.Monthly, emptyList()).values.size)
        assertEquals(12, build(KamleonGraphViewMode.Yearly, emptyList()).values.size)
    }

    @Test
    fun withoutDataEveryBarIsEmptyAndNotPrecise() {
        val graph = build(KamleonGraphViewMode.Weekly, emptyList())
        assertTrue(graph.values.all { it.y == 0.0 })
        assertTrue(graph.arePrecise.none { it })
    }

    // Which bar an item falls in

    @Test
    fun dailyBarsAreHours() {
        val graph = build(KamleonGraphViewMode.Daily, listOf(
            item(at(2025, Calendar.JUNE, 18, 0, 0), 10.0),
            item(at(2025, Calendar.JUNE, 18, 9, 59), 90.0),
            item(at(2025, Calendar.JUNE, 18, 23, 59), 40.0),
        ))
        assertEquals(10.0, graph.bar(0), 0.0)
        assertEquals(90.0, graph.bar(9), 0.0)
        assertEquals(0.0, graph.bar(10), 0.0)
        assertEquals(40.0, graph.bar(23), 0.0)
    }

    @Test
    fun weeklyBarsAreTheLastSevenDaysEndingToday() {
        val graph = build(KamleonGraphViewMode.Weekly, listOf(
            item(at(2025, Calendar.JUNE, 12), 60.0),  // 6 days ago: first bar
            item(at(2025, Calendar.JUNE, 18), 80.0),  // today: last bar
        ))
        assertEquals(60.0, graph.bar(0), 0.0)
        assertEquals(80.0, graph.bar(6), 0.0)
    }

    @Test
    fun monthlyBarsAreTheLastThirtyDaysEndingToday() {
        val graph = build(KamleonGraphViewMode.Monthly, listOf(
            item(at(2025, Calendar.MAY, 20), 55.0),   // 29 days ago: first bar
            item(at(2025, Calendar.JUNE, 18), 75.0),  // today: last bar
        ))
        assertEquals(55.0, graph.bar(0), 0.0)
        assertEquals(75.0, graph.bar(29), 0.0)
    }

    @Test
    fun yearlyBarsAreTheLastTwelveMonthsEndingThisMonth() {
        val graph = build(KamleonGraphViewMode.Yearly, listOf(
            item(at(2025, Calendar.JANUARY, 1), 50.0),  // a monthly average, dated the 1st
            item(at(2025, Calendar.JUNE, 1), 70.0),     // this month
        ))
        assertEquals(50.0, graph.bar(6), 0.0)   // July 2024 is bar 0, so January 2025 is bar 6
        assertEquals(70.0, graph.bar(11), 0.0)
    }

    @Test
    fun theFirstMonthOfTheYearViewShowsItsAverage() {
        // Monthly averages are dated the 1st at 00:00, so the first month must start on the 1st too.
        val graph = build(KamleonGraphViewMode.Yearly, listOf(item(at(2024, Calendar.JULY, 1), 65.0)))
        assertEquals(65.0, graph.bar(0), 0.0)
    }

    @Test
    fun yearlyBarsAreWholeCalendarMonths() {
        val graph = build(KamleonGraphViewMode.Yearly, listOf(
            item(at(2024, Calendar.JUNE, 30, 23, 59), 99.0),  // the month before the graph
            item(at(2024, Calendar.JULY, 31, 23, 59), 40.0),  // last minute of the first month
            item(at(2024, Calendar.AUGUST, 1), 60.0),         // first minute of the second
        ))
        assertEquals(40.0, graph.bar(0), 0.0)
        assertEquals(60.0, graph.bar(1), 0.0)
        assertTrue((2..11).all { graph.bar(it) == 0.0 })
    }

    @Test
    fun yearlyMonthsStayAlignedOnTheLastDayOfALongMonth() {
        // 31 March: eleven months back there is no 31 April, so the graph must not drift into May.
        val graph = build(
            KamleonGraphViewMode.Yearly,
            listOf(item(at(2024, Calendar.APRIL, 1), 10.0), item(at(2025, Calendar.FEBRUARY, 1), 20.0)),
            on = at(2025, Calendar.MARCH, 31, 12, 0),
        )
        assertEquals("Apr", graph.xLabels.first().label)
        assertEquals("Mar", graph.xLabels.last().label)
        assertEquals(10.0, graph.bar(0), 0.0)
        assertEquals(20.0, graph.bar(10), 0.0)
    }

    @Test
    fun yearlyStartsOnTheFirstOfTheMonthElevenMonthsBack() {
        listOf(
            at(2025, Calendar.JANUARY, 1) to at(2024, Calendar.FEBRUARY, 1),
            at(2025, Calendar.JUNE, 18, 15, 30) to at(2024, Calendar.JULY, 1),
            at(2024, Calendar.FEBRUARY, 29, 23, 0) to at(2023, Calendar.MARCH, 1),
            at(2025, Calendar.DECEMBER, 31, 23, 59) to at(2025, Calendar.JANUARY, 1),
        ).forEach { (day, start) ->
            assertEquals(start, KamleonGraphBarDrawData.calcStartDateFrom(KamleonGraphViewMode.Yearly, day))
        }
    }

    @Test
    fun itemsOutsideTheGraphAreIgnored() {
        val graph = build(KamleonGraphViewMode.Weekly, listOf(
            item(at(2025, Calendar.JUNE, 11, 23, 59), 99.0),  // 7 days ago
            item(at(2025, Calendar.JUNE, 19), 99.0),          // tomorrow
        ))
        assertTrue(graph.values.all { it.y == 0.0 })
    }

    // How a bar is computed

    @Test
    fun aBarIsTheAverageOfItsItems() {
        val graph = build(KamleonGraphViewMode.Daily, listOf(
            item(at(2025, Calendar.JUNE, 18, 8, 10), 60.0),
            item(at(2025, Calendar.JUNE, 18, 8, 50), 80.0),
        ))
        assertEquals(70.0, graph.bar(8), 0.0)
        assertTrue(graph.arePrecise[8])
        assertEquals(2, graph.values[8].data.size)
    }

    @Test
    fun preciseItemsWinOverImpreciseOnes() {
        val graph = build(KamleonGraphViewMode.Daily, listOf(
            item(at(2025, Calendar.JUNE, 18, 8, 10), 80.0),
            item(at(2025, Calendar.JUNE, 18, 8, 20), 20.0, precise = false),
        ))
        assertEquals(80.0, graph.bar(8), 0.0)
        assertTrue(graph.arePrecise[8])
        // The bar's detail still lists both.
        assertEquals(2, graph.values[8].data.size)
    }

    @Test
    fun onlyImpreciseItemsAreAveragedButMarkedNotPrecise() {
        val graph = build(KamleonGraphViewMode.Daily, listOf(
            item(at(2025, Calendar.JUNE, 18, 8, 10), 30.0, precise = false),
            item(at(2025, Calendar.JUNE, 18, 8, 20), 50.0, precise = false),
        ))
        assertEquals(40.0, graph.bar(8), 0.0)
        assertFalse(graph.arePrecise[8])
    }

    // Axes

    @Test
    fun hydrationAxisGoesToOneHundredPercent() {
        val graph = build(KamleonGraphViewMode.Daily, emptyList())
        assertEquals(100.0, graph.xyRange.y, 0.0)
        assertEquals(listOf("0%", "20%", "40%", "60%", "80%", "100%"), graph.yLabels.map { it.label })
    }

    @Test
    fun volumeAxisGoesToFiveHundred() {
        val graph = build(KamleonGraphViewMode.Daily, listOf(item(at(2025, Calendar.JUNE, 18, 9), 900.0)), KamleonGraphDataType.volume)
        assertEquals(500.0, graph.xyRange.y, 0.0)
        assertEquals(listOf("0", "100", "200", "300", "400", "500"), graph.yLabels.map { it.label })
    }

    @Test
    fun electrolytesAxisGrowsWithTheHighestBar() {
        fun axisFor(value: Double) = build(
            KamleonGraphViewMode.Daily, listOf(item(at(2025, Calendar.JUNE, 18, 9), value)), KamleonGraphDataType.electrolytes
        ).xyRange.y
        assertEquals(50.0, axisFor(50.0), 0.0)
        assertEquals(100.0, axisFor(50.1), 0.0)
        assertEquals(200.0, axisFor(150.0), 0.0)
        assertEquals(500.0, axisFor(201.0), 0.0)
    }

    @Test
    fun xLabelsMatchEachMode() {
        val daily = build(KamleonGraphViewMode.Daily, emptyList()).xLabels.map { it.label }
        assertEquals(listOf("0", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20", "22"), daily)
        val weekly = build(KamleonGraphViewMode.Weekly, emptyList()).xLabels.map { it.label }
        assertEquals(listOf("Thu", "Fri", "Sat", "Sun", "Mon", "Tue", "Wed"), weekly)
        val yearly = build(KamleonGraphViewMode.Yearly, emptyList()).xLabels.map { it.label }
        assertEquals(listOf("Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun"), yearly)
    }

    // Streak figures (footer)

    @Test
    fun streakFiguresOnlyCountPreciseBarsWithData() {
        val graph = build(KamleonGraphViewMode.Weekly, listOf(
            item(at(2025, Calendar.JUNE, 16), 60.0),
            item(at(2025, Calendar.JUNE, 17), 90.0),
            item(at(2025, Calendar.JUNE, 18), 10.0, precise = false),
        ))
        assertEquals(75.0, graph.averageForStreak(), 0.0)
        assertEquals(60.0, graph.minForStreak(), 0.0)
        assertEquals(90.0, graph.maxForStreak(), 0.0)
    }

    @Test
    fun streakFiguresAreZeroWithoutPreciseData() {
        val graph = build(KamleonGraphViewMode.Weekly, emptyList())
        assertEquals(0.0, graph.averageForStreak(), 0.0)
        assertEquals(0.0, graph.minForStreak(), 0.0)
        assertEquals(0.0, graph.maxForStreak(), 0.0)
    }

    // Data per mode

    @Test
    fun eachModeReadsItsOwnData() {
        val measures = listOf(item(today, 1.0))
        val daily = listOf(item(today, 2.0))
        val monthly = listOf(item(today, 3.0))
        assertEquals(measures, KamleonGraphDataBuilder.sourceFor(KamleonGraphViewMode.Daily, measures, daily, monthly))
        assertEquals(daily, KamleonGraphDataBuilder.sourceFor(KamleonGraphViewMode.Weekly, measures, daily, monthly))
        assertEquals(daily, KamleonGraphDataBuilder.sourceFor(KamleonGraphViewMode.Monthly, measures, daily, monthly))
        assertEquals(monthly, KamleonGraphDataBuilder.sourceFor(KamleonGraphViewMode.Yearly, measures, daily, monthly))
    }
}
