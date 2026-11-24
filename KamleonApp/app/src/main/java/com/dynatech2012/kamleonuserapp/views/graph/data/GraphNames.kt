package com.dynatech2012.kamleonuserapp.views.graph.data

/**
 * The words the graph labels its bars with, taken from the app's strings: short day names from Sunday,
 * short month names from January, and the hour format ("%1$d:00 h"). Kept out of the builder so it
 * stays free of Android resources.
 */
class GraphNames(
    val days: List<String>,
    val months: List<String>,
    private val hourFormat: String,
) {
    init {
        require(days.size == 7) { "seven day names, from Sunday" }
        require(months.size == 12) { "twelve month names, from January" }
    }

    fun hour(hour: Int): String = String.format(java.util.Locale.ROOT, hourFormat, hour)
}
