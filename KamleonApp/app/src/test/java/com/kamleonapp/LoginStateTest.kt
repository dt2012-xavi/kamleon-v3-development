package com.kamleonapp

import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import com.dynatech2012.kamleonuserapp.viewmodels.AuthErrorKind
import com.dynatech2012.kamleonuserapp.viewmodels.LoginState
import com.dynatech2012.kamleonuserapp.viewmodels.loginStateFor
import com.dynatech2012.kamleonuserapp.viewmodels.loginStateForProfileFailure
import java.io.IOException
import java.util.concurrent.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Which message a failed login gets. Firebase's own exceptions are classified in AuthErrorKindTest. */
class LoginStateTest {
    @Test fun everyKindOfFailedSignInHasItsOwnState() {
        assertEquals(LoginState.WRONG_PASSWORD, loginStateFor(AuthErrorKind.InvalidCredentials))
        assertEquals(LoginState.USER_NOT_FOUND, loginStateFor(AuthErrorKind.UserNotFound))
        assertEquals(LoginState.ACCOUNT_DISABLED, loginStateFor(AuthErrorKind.UserDisabled))
        assertEquals(LoginState.TOO_MANY_ATTEMPTS, loginStateFor(AuthErrorKind.TooManyRequests))
        assertEquals(LoginState.NETWORK, loginStateFor(AuthErrorKind.Network))
        assertEquals(LoginState.UNKNOWN, loginStateFor(AuthErrorKind.Other))
    }

    @Test fun noTwoKindsShareAState() {
        val states = AuthErrorKind.entries.map { loginStateFor(it) }
        assertEquals(states.size, states.toSet().size)
    }

    @Test fun newStatesDoNotCollideWithTheOnesAlreadyInUse() {
        // -1..-7 and 0..8 are used by the login, registration and onboarding flows.
        val inUse = (-7..8).toSet()
        listOf(
            LoginState.ACCOUNT_DISABLED,
            LoginState.TOO_MANY_ATTEMPTS,
            LoginState.ACCOUNT_REJECTED,
            LoginState.PROFILE_RETRY
        ).forEach { assertEquals(it.toString(), false, it in inUse) }
    }

    @Test fun aMissingProfileAfterLoginIsRecoveredByRegistering() {
        assertEquals(8, loginStateForProfileFailure(UserProfileMissingException(), sessionRejected = { false }))
    }

    @Test fun aRefusedUserRightAfterSigningInIsNotRetried() {
        // They just signed in, so there is network: retrying cannot help, and staying signed in
        // would only bring them back here on the next start.
        assertEquals(
            LoginState.ACCOUNT_REJECTED,
            loginStateForProfileFailure(Exception("refused"), sessionRejected = { true })
        )
    }

    @Test fun otherProfileFailuresCanBeRetried() {
        listOf(TimeoutException("slow"), IOException("offline"), Exception("unknown")).forEach {
            assertEquals(
                LoginState.PROFILE_RETRY,
                loginStateForProfileFailure(it, sessionRejected = { false })
            )
            assertNotEquals(LoginState.ACCOUNT_REJECTED, loginStateForProfileFailure(it, sessionRejected = { false }))
        }
    }
}
