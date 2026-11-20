package com.kamleonapp

import com.dynatech2012.kamleonuserapp.views.profileImageUrlOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** When the account has a profile photo to show, and when only the initials should be on screen. */
class ProfileImageTest {
    @Test fun noProfileMeansNoPhoto() {
        assertNull(profileImageUrlOf(null))
    }

    @Test fun anEmptyOrBlankUrlMeansNoPhoto() {
        // Removing the photo stores an empty string, not a missing field.
        assertNull(profileImageUrlOf(""))
        assertNull(profileImageUrlOf("   "))
    }

    @Test fun aUrlIsKeptAsIs() {
        assertEquals("https://host/p.jpeg?alt=media&token=t", profileImageUrlOf("https://host/p.jpeg?alt=media&token=t"))
    }

    @Test fun surroundingSpacesAreIgnored() {
        assertEquals("https://host/p.jpeg", profileImageUrlOf("  https://host/p.jpeg\n"))
    }
}
