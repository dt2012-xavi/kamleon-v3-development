package com.kamleonapp

import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.HomeContent
import com.dynatech2012.kamleonuserapp.models.HomeStatus
import com.dynatech2012.kamleonuserapp.models.homeContentOf
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeContentTest {
    private val sample = MeasureData().apply { score = 80 }

    @Test
    fun aSampleOnScreenAlwaysWinsOverTheStatus() {
        for (status in HomeStatus.entries) {
            assertEquals(HomeContent.Sample(sample), homeContentOf(sample, status))
        }
    }

    @Test
    fun withoutASampleTheStatusDecides() {
        assertEquals(HomeContent.Loading, homeContentOf(null, HomeStatus.Loading))
        assertEquals(HomeContent.Empty, homeContentOf(null, HomeStatus.Empty))
        assertEquals(HomeContent.Unavailable, homeContentOf(null, HomeStatus.Unavailable))
    }
}
