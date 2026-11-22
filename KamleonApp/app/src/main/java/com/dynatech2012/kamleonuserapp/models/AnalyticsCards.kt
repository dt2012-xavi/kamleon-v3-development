package com.dynatech2012.kamleonuserapp.models

import androidx.annotation.StringRes
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.views.cards.AnalyticType

/** One metric card on Analytics: its figure, verdict and explanation for the last sample. */
data class AnalyticsCard(
    val type: AnalyticType,
    val value: Int?,
    val isPrecise: Boolean,
    @StringRes val subtitle: Int,
    /** Null when there is nothing to explain (no reading). */
    @StringRes val description: Int?,
)

/**
 * The hydration, electrolytes and volume cards for [measure] (null: no sample yet). An imprecise sample
 * shows no hydration or electrolytes and is judged as insufficient volume.
 */
fun analyticsCardsOf(measure: MeasureData?): List<AnalyticsCard> {
    val precise = measure?.isPrecise != false
    val score = measure?.score?.takeIf { precise }
    val electrolytes = measure?.msCond?.toInt()?.takeIf { precise }
    val volume = measure?.vol?.toInt()
    val volumeLevel = VolumeLevel.of(volume?.takeIf { precise })
    val electrolyteLevel = ElectrolyteLevel.of(electrolytes)
    return listOf(
        AnalyticsCard(
            type = AnalyticType.HYDRATION,
            value = score,
            isPrecise = precise,
            subtitle = when (score) {
                null -> R.string.analytic_no_data
                in 0..33 -> R.string.analytic_severely_dehydrated
                in 34..66 -> R.string.analytic_dehydrated
                in 67..90 -> R.string.analytic_hydrated
                else -> R.string.analytic_very_hydrated
            },
            description = when (score) {
                null -> null
                in 0..66 -> R.string.analytic_hydratation_under
                else -> R.string.analytic_hydratation_above
            },
        ),
        AnalyticsCard(
            type = AnalyticType.ELECTROLYTE,
            value = electrolytes,
            isPrecise = precise,
            subtitle = when (electrolyteLevel) {
                null -> R.string.analytic_no_data
                ElectrolyteLevel.Low -> R.string.analytic_elec_hypo
                ElectrolyteLevel.Balanced -> R.string.analytic_elec_normal
                ElectrolyteLevel.High -> R.string.analytic_elec_hyper
            },
            description = when (electrolyteLevel) {
                null -> null
                ElectrolyteLevel.Low -> R.string.analytic_elec_under
                ElectrolyteLevel.Balanced -> R.string.analytic_elec_optimal
                ElectrolyteLevel.High -> R.string.analytic_elec_above
            },
        ),
        AnalyticsCard(
            type = AnalyticType.VOLUME,
            value = volume,
            isPrecise = precise,
            subtitle = when (volumeLevel) {
                VolumeLevel.NoData -> R.string.analytic_volume_insuff_title
                VolumeLevel.Insufficient -> R.string.analytic_volume_low
                VolumeLevel.Average -> R.string.analytic_volume_medium
                VolumeLevel.High -> R.string.analytic_volume_high
            },
            description = when (volumeLevel) {
                VolumeLevel.NoData, VolumeLevel.Insufficient -> R.string.analytic_volume_insuff_desc
                else -> R.string.analytic_volume_good
            },
        ),
    )
}
