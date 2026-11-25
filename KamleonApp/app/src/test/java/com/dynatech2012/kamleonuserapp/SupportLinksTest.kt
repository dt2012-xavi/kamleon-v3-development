package com.dynatech2012.kamleonuserapp

import com.dynatech2012.kamleonuserapp.views.settings.SupportLinks
import org.junit.Assert.assertEquals
import org.junit.Test

class SupportLinksTest {

    @Test
    fun `the footer names the app, Android and the phone`() {
        assertEquals(
            "App 1.0.15-dev (2028) · Android 14 · Samsung SM-S911B",
            SupportLinks.emailFooter("1.0.15-dev", 2028, "14", "samsung", "SM-S911B")
        )
    }

    @Test
    fun `a model that already starts with the maker does not repeat it`() {
        assertEquals(
            "App 1.0.15 (2028) · Android 13 · OnePlus 11",
            SupportLinks.emailFooter("1.0.15", 2028, "13", "OnePlus", "OnePlus 11")
        )
        assertEquals(
            "App 1.0.15 (2028) · Android 15 · Google Pixel 8",
            SupportLinks.emailFooter("1.0.15", 2028, "15", "Google", "Pixel 8")
        )
    }
}
