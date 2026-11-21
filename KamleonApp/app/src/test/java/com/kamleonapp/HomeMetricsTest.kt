package com.kamleonapp

import com.dynatech2012.kamleonuserapp.models.ElectrolyteLevel
import com.dynatech2012.kamleonuserapp.models.HomeStatus
import com.dynatech2012.kamleonuserapp.models.VolumeLevel
import com.dynatech2012.kamleonuserapp.models.homeStatusAfterLoad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeMetricsTest {
    @Test fun electrolytesWithoutAValueHaveNoLevel() = assertNull(ElectrolyteLevel.of(null))

    @Test fun electrolyteBandsKeepTheirLimits() {
        assertEquals(ElectrolyteLevel.Low, ElectrolyteLevel.of(0))
        assertEquals(ElectrolyteLevel.Low, ElectrolyteLevel.of(4))
        assertEquals(ElectrolyteLevel.Balanced, ElectrolyteLevel.of(5))
        assertEquals(ElectrolyteLevel.Balanced, ElectrolyteLevel.of(25))
        assertEquals(ElectrolyteLevel.High, ElectrolyteLevel.of(26))
    }

    @Test fun volumeBandsKeepTheirLimits() {
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
