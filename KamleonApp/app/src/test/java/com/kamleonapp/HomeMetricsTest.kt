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

    @Test fun electrolyteBandsGoByTheWholeNumber() {
        // 0-4 mS/cm hypotonic, 5-25 normotonic, 26+ hypertonic, on the truncated reading.
        assertEquals(ElectrolyteLevel.Low, ElectrolyteLevel.of(0f))
        assertEquals(ElectrolyteLevel.Low, ElectrolyteLevel.of(4.99f))
        assertEquals(ElectrolyteLevel.Balanced, ElectrolyteLevel.of(5f))
        assertEquals(ElectrolyteLevel.Balanced, ElectrolyteLevel.of(25.99f))
        assertEquals(ElectrolyteLevel.High, ElectrolyteLevel.of(26f))
        assertTrue(ElectrolyteLevel.isOptimum(5.0))
        assertTrue(ElectrolyteLevel.isOptimum(25.5))
        assertFalse(ElectrolyteLevel.isOptimum(4.9))
        assertFalse(ElectrolyteLevel.isOptimum(26.0))
    }

    @Test fun hydrationBands() {
        // 1-32 severely dehydrated, 33-66 dehydrated, 67-90 hydrated, 91+ very well hydrated.
        assertEquals(HydrationLevel.SeverelyDehydrated, HydrationLevel.of(1))
        assertEquals(HydrationLevel.SeverelyDehydrated, HydrationLevel.of(32))
        assertEquals(HydrationLevel.Dehydrated, HydrationLevel.of(33))
        assertEquals(HydrationLevel.Dehydrated, HydrationLevel.of(66))
        assertEquals(HydrationLevel.Hydrated, HydrationLevel.of(67))
        assertEquals(HydrationLevel.Hydrated, HydrationLevel.of(90))
        assertEquals(HydrationLevel.VeryHydrated, HydrationLevel.of(91))
        assertEquals(HydrationLevel.VeryHydrated, HydrationLevel.of(100))
    }

    @Test fun theHydrationStreakCountsFrom67WithNoUpperLimit() {
        assertFalse(HydrationLevel.isOptimum(66.9))
        assertTrue(HydrationLevel.isOptimum(67.0))
        assertTrue(HydrationLevel.isOptimum(90.5))
        assertTrue(HydrationLevel.isOptimum(100.0))
    }

    @Test fun homeAndAnalyticsReadTheSameHydrationLevel() {
        val measure = MeasureData().apply { score = 67 }
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
