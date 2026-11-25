package com.dynatech2012.kamleonuserapp

import com.dynatech2012.kamleonuserapp.utils.ProfilePhoto
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfilePhotoTest {

    @Test
    fun `a large landscape photo is decoded with its shorter side at 568`() {
        assertEquals(757 to 568, ProfilePhoto.decodeSize(4000, 3000))
    }

    @Test
    fun `a large portrait photo is decoded with its shorter side at 568`() {
        assertEquals(568 to 757, ProfilePhoto.decodeSize(3000, 4000))
    }

    @Test
    fun `a small photo is decoded at its own size`() {
        assertEquals(300 to 200, ProfilePhoto.decodeSize(300, 200))
    }

    @Test
    fun `the square is taken from the centre of a landscape image`() {
        assertEquals(Triple(85, 0, 512), ProfilePhoto.centreSquare(682, 512))
    }

    @Test
    fun `the square is taken from the centre of a portrait image`() {
        assertEquals(Triple(0, 85, 512), ProfilePhoto.centreSquare(512, 682))
    }

    @Test
    fun `a square image is kept whole`() {
        assertEquals(Triple(0, 0, 512), ProfilePhoto.centreSquare(512, 512))
    }
}
