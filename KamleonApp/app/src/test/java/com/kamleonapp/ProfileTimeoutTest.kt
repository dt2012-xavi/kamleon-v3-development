package com.kamleonapp

import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.profileOrTimeout
import com.dynatech2012.kamleonuserapp.viewmodels.startupStateFor
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTimeoutTest {
    @Test fun answerInTimeIsReturnedAsIs() = runBlocking {
        val answer = Response.Success(CustomUser())
        assertEquals(answer, profileOrTimeout(1_000) { answer })
    }

    @Test fun failureInTimeIsReturnedAsIs() = runBlocking {
        val failure = Response.Failure(Exception("offline"))
        assertEquals(failure, profileOrTimeout(1_000) { failure })
    }

    @Test fun slowAnswerBecomesAFailureTheUserCanRetry() = runBlocking {
        // A slow network that neither answers nor fails must not leave the splash up forever.
        val result = profileOrTimeout(50) {
            delay(5_000)
            Response.Success(CustomUser())
        }
        assertTrue(result is Response.Failure && result.exception is TimeoutException)
        assertEquals(
            AuthViewModel.StartupState.RetryableError,
            startupStateFor(authenticated = true, verified = true, profile = result)
        )
    }
}
