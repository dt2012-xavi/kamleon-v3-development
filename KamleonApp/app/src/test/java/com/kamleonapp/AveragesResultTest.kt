package com.kamleonapp

import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.averagesResult
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The averages are read one day or month at a time; a network failure must not look like "no data". */
class AveragesResultTest {
    @Test fun someReadsSucceedingGivesTheOnesThatDid() {
        val result = averagesResult(listOf(Result.success(1), Result.failure(IOException("x")), Result.success(3)))
        assertEquals(Response.Success(arrayListOf(1, 3)), result)
    }

    @Test fun everyReadFailingIsAFailureWithTheFirstException() {
        val first = IOException("first")
        val result = averagesResult(listOf(Result.failure<Int>(first), Result.failure<Int>(IOException("second"))))
        assertTrue(result is Response.Failure)
        assertEquals(first, (result as Response.Failure).exception)
    }

    @Test fun nothingToReadIsAnEmptySuccess() {
        assertEquals(Response.Success(arrayListOf<Int>()), averagesResult(emptyList<Result<Int>>()))
    }
}
