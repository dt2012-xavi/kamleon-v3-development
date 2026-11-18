package com.dynatech2012.kamleonuserapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel.StartupState
import com.dynatech2012.kamleonuserapp.viewmodels.ErrorCode
import com.dynatech2012.kamleonuserapp.viewmodels.errorCodeOf
import com.dynatech2012.kamleonuserapp.viewmodels.startupStateFor
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real classification of Firebase errors at startup. Firebase's exceptions need real Android
 * classes, so this cannot be a unit test.
 */
@RunWith(AndroidJUnit4::class)
class SessionRejectedTest {
    private fun stateAfter(failure: Exception) = startupStateFor(
        authenticated = true,
        verified = true,
        profile = Response.Failure(failure)
    )

    @Test fun serverRefusingTheUserEndsTheSession() {
        assertEquals(StartupState.SessionRejected, stateAfter(FirebaseFirestoreException("test", Code.PERMISSION_DENIED)))
        assertEquals(StartupState.SessionRejected, stateAfter(FirebaseFirestoreException("test", Code.UNAUTHENTICATED)))
    }

    @Test fun disabledOrDeletedAccountEndsTheSession() {
        assertEquals(
            StartupState.SessionRejected,
            stateAfter(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "test"))
        )
        assertEquals(
            StartupState.SessionRejected,
            stateAfter(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "test"))
        )
    }

    @Test fun networkAndBusyServerCodesAreRetryable() {
        listOf(Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED, Code.ABORTED, Code.RESOURCE_EXHAUSTED, Code.CANCELLED)
            .forEach {
                assertEquals(it.name, StartupState.RetryableError, stateAfter(FirebaseFirestoreException("test", it)))
            }
    }

    @Test fun everyOtherCodeOffersALogoutToo() {
        val handled = setOf(
            Code.OK, Code.PERMISSION_DENIED, Code.UNAUTHENTICATED,
            Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED, Code.ABORTED, Code.RESOURCE_EXHAUSTED, Code.CANCELLED
        )
        Code.values().filter { it !in handled }.forEach {
            assertEquals(it.name, StartupState.ServerError, stateAfter(FirebaseFirestoreException("test", it)))
        }
    }

    @Test fun eachCauseHasItsOwnCode() {
        assertEquals(ErrorCode.SessionUnauthenticated, errorCodeOf(FirebaseFirestoreException("t", Code.UNAUTHENTICATED)))
        assertEquals(ErrorCode.SessionPermissionDenied, errorCodeOf(FirebaseFirestoreException("t", Code.PERMISSION_DENIED)))
        assertEquals(ErrorCode.AccountDisabled, errorCodeOf(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "t")))
        assertEquals(ErrorCode.AccountNotFound, errorCodeOf(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "t")))
        assertEquals(ErrorCode.ServerError, errorCodeOf(FirebaseFirestoreException("t", Code.INTERNAL)))
        assertEquals(ErrorCode.ServerError, errorCodeOf(IllegalStateException("unparseable profile")))
    }
}
