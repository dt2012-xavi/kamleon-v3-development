package com.kamleonapp

import com.dynatech2012.kamleonuserapp.viewmodels.ErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The codes users read out to support. The numbers are shared with the other clients (iOS, web) and
 * must never change meaning or be reused, so they are pinned here.
 */
class ErrorCodeTest {
    @Test fun labelsCarryTheAndroidPrefixAndAThreeDigitNumber() {
        ErrorCode.entries.forEach {
            assertTrue(it.label, Regex("""KL-AND-\d{3}""").matches(it.label))
        }
    }

    @Test fun numbersAreUnique() {
        val numbers = ErrorCode.entries.map { it.number }
        assertEquals(numbers.distinct(), numbers)
    }

    @Test fun theNumbersOfTheSharedTableDoNotChange() {
        assertEquals("KL-AND-101", ErrorCode.SessionUnauthenticated.label)
        assertEquals("KL-AND-102", ErrorCode.SessionPermissionDenied.label)
        assertEquals("KL-AND-103", ErrorCode.AccountDisabled.label)
        assertEquals("KL-AND-104", ErrorCode.AccountNotFound.label)
        assertEquals("KL-AND-110", ErrorCode.ServerError.label)
    }
}
