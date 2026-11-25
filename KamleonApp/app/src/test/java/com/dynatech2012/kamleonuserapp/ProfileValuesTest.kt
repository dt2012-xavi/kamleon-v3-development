package com.dynatech2012.kamleonuserapp

import com.dynatech2012.kamleonuserapp.views.settings.ProfileValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileValuesTest {

    @Test
    fun `an unset height or weight has no value`() {
        assertNull(ProfileValues.height(null))
        assertNull(ProfileValues.height(0f))
        assertNull(ProfileValues.weightText(null))
        assertNull(ProfileValues.weightText(0f))
    }

    @Test
    fun `heights are rounded to whole centimetres`() {
        assertEquals(176, ProfileValues.height(175.5f))
        assertEquals(175, ProfileValues.height(175.4f))
        assertEquals(180, ProfileValues.height(180f))
    }

    @Test
    fun `weights show one decimal without a trailing zero`() {
        assertEquals("70", ProfileValues.weightText(70f))
        assertEquals("70.5", ProfileValues.weightText(70.5f))
        assertEquals("70.3", ProfileValues.weightText(70.25f + 0.01f))
        assertEquals("71", ProfileValues.weightText(70.96f))
    }

    @Test
    fun `the wheels open on the saved value or the default`() {
        assertEquals(176, ProfileValues.heightStart(175.6f))
        assertEquals(170, ProfileValues.heightStart(null))
        assertEquals("70.5", ProfileValues.weightStart(70.5f))
        assertEquals("80.0", ProfileValues.weightStart(80f))
        assertEquals("70.0", ProfileValues.weightStart(0f))
    }

    @Test
    fun `the wheel stretches to a saved value outside its range`() {
        assertEquals("100", ProfileValues.wheelValues(100..250, 170).first())
        assertEquals("250", ProfileValues.wheelValues(100..250, 170).last())
        assertEquals("95", ProfileValues.wheelValues(100..250, 95).first())
        assertEquals("260", ProfileValues.wheelValues(30..250, 260).last())
    }

    @Test
    fun `a PIN keeps only six digits`() {
        assertEquals("123456", ProfileValues.pinInput("12a34 5678"))
        assertEquals("", ProfileValues.pinInput("abc"))
    }

    @Test
    fun `only six digits make a whole PIN`() {
        assertEquals(true, ProfileValues.isCompletePin("012345"))
        assertEquals(false, ProfileValues.isCompletePin("12345"))
        assertEquals(false, ProfileValues.isCompletePin("12345a"))
    }
}
