package com.dynatech2012.kamleonuserapp.models

/**
 * Hydration band of a sample, from its score. The bands are Kamleon's published ones (the web's
 * "How the result works"): 1-43 severely dehydrated, 44-64 dehydrated, 65-93 hydrated, 94-99 overhydrated.
 * Hydrated is the optimum the graphs' streak counts.
 */
enum class HydrationLevel {
    SeverelyDehydrated, Dehydrated, Hydrated, Overhydrated;

    companion object {
        const val DEHYDRATED_MIN = 44
        const val HYDRATED_MIN = 65
        const val HYDRATED_MAX = 93

        fun of(score: Int): HydrationLevel = when {
            score < DEHYDRATED_MIN -> SeverelyDehydrated
            score < HYDRATED_MIN -> Dehydrated
            score <= HYDRATED_MAX -> Hydrated
            else -> Overhydrated
        }

        /** Whether a (possibly averaged) score is in the optimum band. */
        fun isOptimum(score: Double): Boolean = score >= HYDRATED_MIN && score <= HYDRATED_MAX
    }
}

/**
 * Electrolyte band of a sample, from its conductivity in mS/cm: under 5 hypotonic, 5-25 normotonic,
 * over 25 hypertonic. Judged on the exact reading, not the whole number shown, so 25.4 is already high.
 */
enum class ElectrolyteLevel {
    Low, Balanced, High;

    companion object {
        const val BALANCED_MIN = 5.0
        const val BALANCED_MAX = 25.0

        /** Null when there is no reliable value. */
        fun of(conductivity: Double?): ElectrolyteLevel? = when {
            conductivity == null -> null
            conductivity < BALANCED_MIN -> Low
            conductivity <= BALANCED_MAX -> Balanced
            else -> High
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
