package com.kamleonapp

import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.ElectrolyteLevel
import com.dynatech2012.kamleonuserapp.models.HydrationLevel
import com.dynatech2012.kamleonuserapp.models.VolumeLevel
import com.dynatech2012.kamleonuserapp.models.HomeStatus
import com.dynatech2012.kamleonuserapp.models.homeStatusAfterLoad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class HomeMetricsTest {
    @Test fun electrolytesWithoutAValueHaveNoLevel() = assertNull(ElectrolyteLevel.of(null as Float?))

    @Test fun electrolyteBandsMatchThePublishedRanges() {
        // Under 5 mS/cm hypotonic, 5-25 normotonic, over 25 hypertonic.
        assertEquals(ElectrolyteLevel.Low, ElectrolyteLevel.of(0f))
        assertEquals(ElectrolyteLevel.Low, ElectrolyteLevel.of(4.99f))
        assertEquals(ElectrolyteLevel.Balanced, ElectrolyteLevel.of(5f))
        assertEquals(ElectrolyteLevel.Balanced, ElectrolyteLevel.of(25f))
        assertEquals(ElectrolyteLevel.High, ElectrolyteLevel.of(25.01f))
        assertTrue(ElectrolyteLevel.isOptimum(5.0))
        assertFalse(ElectrolyteLevel.isOptimum(25.5))
    }

    @Test fun hydrationBandsMatchThePublishedRanges() {
        assertEquals(HydrationLevel.SeverelyDehydrated, HydrationLevel.of(0))
        assertEquals(HydrationLevel.SeverelyDehydrated, HydrationLevel.of(43))
        assertEquals(HydrationLevel.Dehydrated, HydrationLevel.of(44))
        assertEquals(HydrationLevel.Dehydrated, HydrationLevel.of(64))
        assertEquals(HydrationLevel.Hydrated, HydrationLevel.of(65))
        assertEquals(HydrationLevel.Hydrated, HydrationLevel.of(93))
        assertEquals(HydrationLevel.Overhydrated, HydrationLevel.of(94))
        assertEquals(HydrationLevel.Overhydrated, HydrationLevel.of(100))
    }

    @Test fun theOptimumHydrationForTheGraphsIsTheHydratedBand() {
        assertFalse(HydrationLevel.isOptimum(64.9))
        assertTrue(HydrationLevel.isOptimum(65.0))
        assertTrue(HydrationLevel.isOptimum(93.0))
        assertFalse(HydrationLevel.isOptimum(93.5))
    }

    @Test fun homeAndAnalyticsReadTheSameHydrationLevel() {
        val measure = MeasureData().apply { score = 65 }
        assertEquals(HydrationLevel.Hydrated, measure.hydrationLevel)
    }

    @Test fun volumeBandsHaveTheirOptimumBetween150And250() {
        assertEquals(VolumeLevel.NoData, VolumeLevel.of(null))
        assertEquals(VolumeLevel.Insufficient, VolumeLevel.of(0))
        assertEquals(VolumeLevel.Insufficient, VolumeLevel.of(149))
        assertEquals(VolumeLevel.Average, VolumeLevel.of(150))
        assertEquals(VolumeLevel.Average, VolumeLevel.of(250))
        assertEquals(VolumeLevel.High, VolumeLevel.of(251))
    }

    @Test fun anySampleMeansThereIsSomethingToShow() {
        assertNull(homeStatusAfterLoad(sampleCount = 3, remoteSucceeded = true))
        assertNull(homeStatusAfterLoad(sampleCount = 1, remoteSucceeded = false))
    }

    @Test fun noSamplesAfterASuccessfulLoadIsAnEmptyAccount() =
        assertEquals(HomeStatus.Empty, homeStatusAfterLoad(sampleCount = 0, remoteSucceeded = true))

    @Test fun noSamplesAfterAFailedLoadIsUnavailable() =
        assertEquals(HomeStatus.Unavailable, homeStatusAfterLoad(sampleCount = 0, remoteSucceeded = false))
}
