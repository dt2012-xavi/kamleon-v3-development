package com.dynatech2012.kamleonuserapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dynatech2012.kamleonuserapp.viewmodels.AuthErrorKind
import com.dynatech2012.kamleonuserapp.viewmodels.authErrorKindOf
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The real classification of sign-in errors. Firebase's exceptions need real Android classes. */
@RunWith(AndroidJUnit4::class)
class AuthErrorKindTest {
    @Test fun wrongCredentials() {
        assertEquals(
            AuthErrorKind.InvalidCredentials,
            authErrorKindOf(FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "test"))
        )
    }

    @Test fun unknownAndDisabledAccountsAreToldApart() {
        assertEquals(
            AuthErrorKind.UserNotFound,
            authErrorKindOf(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "test"))
        )
        assertEquals(
            AuthErrorKind.UserDisabled,
            authErrorKindOf(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "test"))
        )
    }

    @Test fun tooManyAttempts() {
        assertEquals(AuthErrorKind.TooManyRequests, authErrorKindOf(FirebaseTooManyRequestsException("test")))
    }

    @Test fun noNetwork() {
        assertEquals(AuthErrorKind.Network, authErrorKindOf(FirebaseNetworkException("test")))
    }

    @Test fun anythingElseIsOther() {
        assertEquals(AuthErrorKind.Other, authErrorKindOf(Exception("unknown")))
        assertEquals(AuthErrorKind.Other, authErrorKindOf(null))
    }
}
