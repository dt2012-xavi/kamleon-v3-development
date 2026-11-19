package com.kamleonapp

import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.network.ActionResult
import com.dynatech2012.kamleonuserapp.network.NetworkTimeoutException
import com.dynatech2012.kamleonuserapp.network.NetworkUnavailableException
import com.dynatech2012.kamleonuserapp.network.actionFailureText
import com.dynatech2012.kamleonuserapp.network.actionResultOf
import com.dynatech2012.kamleonuserapp.network.isConnectionProblem
import com.dynatech2012.kamleonuserapp.network.scanFailureText
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.ResponseNullable
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** One criterion for every screen: a connection problem is told the same way wherever it happens. */
class ActionFailureTest {
    private val networkText = R.string.login_error_title_network to R.string.startup_error_message

    @Test fun connectionProblemsAreNoNetworkTimeoutsAndIoFailures() {
        listOf(NetworkUnavailableException(), NetworkTimeoutException(1), IOException("x")).forEach {
            assertTrue(it.javaClass.simpleName, it.isConnectionProblem())
        }
        assertFalse(Exception("x").isConnectionProblem())
    }

    @Test fun aConnectionProblemIsToldAsNoConnection() {
        assertEquals(networkText, actionFailureText(NetworkUnavailableException()))
        assertEquals(networkText, actionFailureText(IOException("offline")))
    }

    @Test fun anythingElseIsAGenericFailure() {
        val generic = R.string.action_failed_title to R.string.action_failed_message
        assertEquals(generic, actionFailureText(Exception("x")))
        assertEquals(generic, actionFailureText(null))
    }

    @Test fun aScanThatCouldNotBeUploadedKeepsItsOwnTextUnlessTheConnectionIsTheCause() {
        assertEquals(networkText, scanFailureText(NetworkUnavailableException()))
        val scan = R.string.dialog_scan_failed_title to R.string.dialog_scan_failed_message
        assertEquals(scan, scanFailureText(Exception("x")))
        assertEquals(scan, scanFailureText(null))
    }

    @Test fun anActionResultKeepsTheFailure() {
        assertTrue(actionResultOf(Response.Success(true)).success)
        val boom = IOException("x")
        val failed = actionResultOf(Response.Failure(boom))
        assertFalse(failed.success)
        assertEquals(boom, failed.failure)
        assertTrue(actionResultOf(ResponseNullable.Success<Nothing>()).success)
        assertEquals(boom, actionResultOf(ResponseNullable.Failure(boom)).failure)
        assertNull(ActionResult().failure)
    }
}
