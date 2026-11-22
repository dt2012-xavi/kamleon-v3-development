package com.dynatech2012.kamleonuserapp.models

import com.dynatech2012.kamleonuserapp.database.MeasureData

/** Everything Home can be showing: a sample, or the reason there is none. */
sealed interface HomeContent {
    data object Loading : HomeContent
    data object Empty : HomeContent
    data object Unavailable : HomeContent
    data class Sample(val measure: MeasureData) : HomeContent
}

/**
 * Picks what Home shows. A sample already on screen always wins: a failed refresh must not replace it
 * with an error, so [status] only matters while there is no sample.
 */
fun homeContentOf(lastMeasure: MeasureData?, status: HomeStatus): HomeContent = when {
    lastMeasure != null -> HomeContent.Sample(lastMeasure)
    status == HomeStatus.Empty -> HomeContent.Empty
    status == HomeStatus.Unavailable -> HomeContent.Unavailable
    else -> HomeContent.Loading
}
