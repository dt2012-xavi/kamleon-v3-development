package com.dynatech2012.kamleonuserapp.models

/** Electrolyte band of a sample, from its conductivity in mS/cm. Shared by Home and Analytics. */
enum class ElectrolyteLevel {
    Low, Balanced, High;

    companion object {
        /** Null when there is no reliable value. */
        fun of(conductivity: Int?): ElectrolyteLevel? = when (conductivity) {
            null -> null
            in 0..4 -> Low
            in 5..25 -> Balanced
            else -> High
        }
    }
}

/** Volume band of a sample, from its volume in ml. */
enum class VolumeLevel {
    NoData, Insufficient, Average, High;

    companion object {
        fun of(volume: Int?): VolumeLevel = when (volume) {
            null -> NoData
            in 0..149 -> Insufficient
            in 150..250 -> Average
            else -> High
        }
    }
}

/** What Home shows while there is no sample to show. */
enum class HomeStatus { Loading, Empty, Unavailable }

/**
 * Decides what Home shows once the samples have been requested. Null means there is a sample to show.
 * [remoteSucceeded] tells an account with no samples yet apart from a load that failed.
 */
fun homeStatusAfterLoad(sampleCount: Int, remoteSucceeded: Boolean): HomeStatus? = when {
    sampleCount > 0 -> null
    remoteSucceeded -> HomeStatus.Empty
    else -> HomeStatus.Unavailable
}
