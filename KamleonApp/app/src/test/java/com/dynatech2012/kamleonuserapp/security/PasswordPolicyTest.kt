package com.dynatech2012.kamleonuserapp.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class PasswordPolicyTest {
    private var strength = PasswordStrength.STRONG
    private var breachCalls = 0
    private var breachResult: () -> Boolean = { false }

    private val policy = PasswordPolicy(
        strengthEstimator = { strength },
        breachChecker = { breachCalls++; breachResult() },
        computeDispatcher = Dispatchers.Unconfined
    )

    private fun validate(password: String) = runBlocking { policy.validate(password) }
    private fun rejected(reason: PasswordPolicyReason) = PasswordPolicyResult.Rejected(reason)

    @Test fun sevenCharactersAreTooShortAndNothingElseRuns() {
        assertEquals(rejected(PasswordPolicyReason.TOO_SHORT), validate("a".repeat(7)))
        assertEquals(0, breachCalls)
    }

    @Test fun moreThan128CharactersAreTooLong() {
        assertEquals(rejected(PasswordPolicyReason.TOO_LONG), validate("a".repeat(129)))
        assertEquals(PasswordPolicyResult.Valid, validate("a".repeat(128)))
    }

    @Test fun weakPasswordIsRejectedBeforeTheNetworkCheck() {
        strength = PasswordStrength.WEAK
        assertEquals(rejected(PasswordPolicyReason.WEAK), validate("password123"))
        assertEquals(0, breachCalls)
    }

    @Test fun breachedPasswordIsRejected() {
        breachResult = { true }
        assertEquals(rejected(PasswordPolicyReason.BREACHED), validate("correct horse battery"))
    }

    @Test fun breachCheckFailureFailsOpen() {
        breachResult = { throw IOException("offline") }
        assertEquals(PasswordPolicyResult.Valid, validate("correct horse battery"))
        assertEquals(1, breachCalls)
    }

    @Test fun goodAndStrongPasswordsPass() {
        strength = PasswordStrength.GOOD
        assertEquals(PasswordPolicyResult.Valid, validate("correct horse battery"))
    }

    @Test fun noCompositionRulesApply() {
        // Lowercase only, no digits or symbols: fine if long and strong enough.
        assertEquals(PasswordPolicyResult.Valid, validate("lowercaseonlypassphrase"))
    }
}
