package com.dynatech2012.kamleonuserapp.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ZxcvbnPasswordStrengthEstimatorTest {
    private val estimator = ZxcvbnPasswordStrengthEstimator()

    @Test fun commonPasswordsAreWeak() {
        assertEquals(PasswordStrength.WEAK, estimator.estimate("password123"))
        assertEquals(PasswordStrength.WEAK, estimator.estimate("12345678"))
    }

    @Test fun longRandomPassphraseIsNotWeak() {
        assertNotEquals(PasswordStrength.WEAK, estimator.estimate("tractor-violin-nebula-quartz-9"))
    }
}
