package com.dynatech2012.kamleonuserapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel.StartupState
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

    @Test fun serverRefusingTheUserSendsThemToLogin() {
        assertEquals(StartupState.NeedsLogin, stateAfter(FirebaseFirestoreException("test", Code.PERMISSION_DENIED)))
        assertEquals(StartupState.NeedsLogin, stateAfter(FirebaseFirestoreException("test", Code.UNAUTHENTICATED)))
    }

    @Test fun disabledOrDeletedAccountSendsTheUserToLogin() {
        assertEquals(
            StartupState.NeedsLogin,
            stateAfter(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "test"))
        )
        assertEquals(
            StartupState.NeedsLogin,
            stateAfter(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "test"))
        )
    }

    @Test fun everyOtherFirestoreCodeIsRetryable() {
        Code.values()
            .filter { it != Code.OK && it != Code.PERMISSION_DENIED && it != Code.UNAUTHENTICATED }
            .forEach {
                assertEquals(it.name, StartupState.RetryableError, stateAfter(FirebaseFirestoreException("test", it)))
            }
    }
}
