package com.dynatech2012.kamleonuserapp

import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import org.junit.Assert.assertEquals
import org.junit.Test

class EmailLocaleTest {
    @Test
    fun `spanish and catalan phones get their language, others english`() {
        assertEquals("es", CloudFunctions.emailLocaleFor("es"))
        assertEquals("cat", CloudFunctions.emailLocaleFor("ca"))
        assertEquals("en", CloudFunctions.emailLocaleFor("en"))
        assertEquals("en", CloudFunctions.emailLocaleFor("fr"))
        assertEquals("en", CloudFunctions.emailLocaleFor(""))
    }
}
