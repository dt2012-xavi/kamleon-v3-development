package com.dynatech2012.kamleonuserapp.models

/**
 * Hydration band of a sample, from its score: 1-32 severely dehydrated, 33-66 dehydrated, 67-90 hydrated,
 * 91+ very well hydrated. The graphs' streak counts any score from 67 up.
 */
enum class HydrationLevel {
    SeverelyDehydrated, Dehydrated, Hydrated, VeryHydrated;

    companion object {
        const val DEHYDRATED_MIN = 33
        const val HYDRATED_MIN = 67
        const val HYDRATED_MAX = 90

        fun of(score: Int): HydrationLevel = when {
            score < DEHYDRATED_MIN -> SeverelyDehydrated
            score < HYDRATED_MIN -> Dehydrated
            score <= HYDRATED_MAX -> Hydrated
            else -> VeryHydrated
        }

        /** Whether a (possibly averaged) score counts for the streak: hydrated or above, with no upper limit. */
        fun isOptimum(score: Double): Boolean = score >= HYDRATED_MIN
    }
}

/**
 * Electrolyte band of a sample, from its conductivity in mS/cm: 0-4 hypotonic, 5-25 normotonic,
 * 26+ hypertonic. Judged on the whole number shown (truncated), so 25.7 is still normotonic.
 */
enum class ElectrolyteLevel {
    Low, Balanced, High;

    companion object {
        const val BALANCED_MIN = 5.0
        const val BALANCED_MAX = 25.0

        /** Null when there is no reliable value. */
        fun of(conductivity: Double?): ElectrolyteLevel? = when {
            conductivity == null -> null
            else -> conductivity.toInt().let { whole ->
                when {
                    whole < BALANCED_MIN -> Low
                    whole <= BALANCED_MAX -> Balanced
                    else -> High
                }
            }
        }

        fun of(conductivity: Float?): ElectrolyteLevel? = of(conductivity?.toDouble())

        /** Whether a (possibly averaged) conductivity is in the optimum band. */
        fun isOptimum(conductivity: Double): Boolean = of(conductivity) == Balanced
    }
}

/**
 * Volume band of a sample, from its volume in ml: 150-250 ml is the optimum, below it the sample is
 * insufficient and above it high.
 */
enum class VolumeLevel {
    NoData, Insufficient, Average, High;

    companion object {
        const val OPTIMUM_MIN = 150
        const val OPTIMUM_MAX = 250

        fun of(volume: Int?): VolumeLevel = when {
            volume == null -> NoData
            volume < OPTIMUM_MIN -> Insufficient
            volume <= OPTIMUM_MAX -> Average
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
