package com.dynatech2012.kamleonuserapp.viewmodels

import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * Values of [AuthViewModel.uiState] that the login screen reacts to. The state is shared with the
 * registration and onboarding flows, which use 0..8 and -1..-7: the codes added later start at -8.
 * 6 (policy sheet), 7 (signed in) and 8 (profile missing) are positive and listed here for reference.
 */
internal object LoginState {
    const val UNKNOWN = -1
    const val WRONG_PASSWORD = -2
    const val USER_NOT_FOUND = -3
    const val INVALID_EMAIL = -4
    const val EMAIL_NOT_VERIFIED = -5
    const val NETWORK = -6
    const val ACCOUNT_EXISTS = -7

    /** The account exists but was disabled: signing in cannot work until someone enables it. */
    const val ACCOUNT_DISABLED = -8
    const val TOO_MANY_ATTEMPTS = -9

    /** Signed in, but the server refuses to give this user their profile. */
    const val ACCOUNT_REJECTED = -10

    /** Signed in, but the profile could not be read right now: worth another try. */
    const val PROFILE_RETRY = -11

    /** The answer to the privacy policy could not be saved right now: worth another try. */
    const val POLICY_RETRY = -12

    const val PROFILE_MISSING = 8
}

/** Why a sign-in failed, in the terms the user needs: what to do differs for each. */
internal enum class AuthErrorKind { InvalidCredentials, UserNotFound, UserDisabled, TooManyRequests, Network, Other }

internal fun authErrorKindOf(e: Exception?): AuthErrorKind = when (e) {
    is FirebaseNetworkException -> AuthErrorKind.Network
    is FirebaseTooManyRequestsException -> AuthErrorKind.TooManyRequests
    is FirebaseAuthInvalidUserException ->
        if (e.errorCode == "ERROR_USER_DISABLED") AuthErrorKind.UserDisabled else AuthErrorKind.UserNotFound
    is FirebaseAuthInvalidCredentialsException -> AuthErrorKind.InvalidCredentials
    else -> AuthErrorKind.Other
}

internal fun loginStateFor(kind: AuthErrorKind): Int = when (kind) {
    AuthErrorKind.InvalidCredentials -> LoginState.WRONG_PASSWORD
    AuthErrorKind.UserNotFound -> LoginState.USER_NOT_FOUND
    AuthErrorKind.UserDisabled -> LoginState.ACCOUNT_DISABLED
    AuthErrorKind.TooManyRequests -> LoginState.TOO_MANY_ATTEMPTS
    AuthErrorKind.Network -> LoginState.NETWORK
    AuthErrorKind.Other -> LoginState.UNKNOWN
}

/**
 * The state after a successful sign-in whose profile could not be read. The user just signed in, so
 * there is network: a refusal will not go away by retrying, and anything else may.
 */
internal fun loginStateForProfileFailure(
    e: Exception,
    sessionRejected: (Exception) -> Boolean = ::isSessionRejected
): Int = when {
    e is UserProfileMissingException -> LoginState.PROFILE_MISSING
    sessionRejected(e) -> LoginState.ACCOUNT_REJECTED
    else -> LoginState.PROFILE_RETRY
}
