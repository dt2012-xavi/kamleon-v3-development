package com.dynatech2012.kamleonuserapp.viewmodels

import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The server answered, and refused the signed-in user: the account was disabled or deleted, or the
 * session revoked. Retrying cannot fix that, only logging in again can, so it must not be shown as
 * a connection problem. Network and server trouble (unavailable, timeout, ...) stays retryable.
 */
internal fun isSessionRejected(e: Exception): Boolean = when (e) {
    is FirebaseAuthInvalidUserException -> true
    is FirebaseFirestoreException ->
        e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
    else -> false
}

internal fun startupStateFor(
    authenticated: Boolean,
    verified: Boolean,
    profile: Response<CustomUser>?,
    sessionRejected: (Exception) -> Boolean = ::isSessionRejected
): AuthViewModel.StartupState {
    if (!authenticated) return AuthViewModel.StartupState.NeedsLogin
    return when (profile) {
        is Response.Success -> when {
            !verified -> AuthViewModel.StartupState.NeedsLogin
            profile.data.legal.privacyPolicyApp -> AuthViewModel.StartupState.Ready
            else -> AuthViewModel.StartupState.NeedsPolicy
        }
        is Response.Failure -> when {
            profile.exception is UserProfileMissingException -> AuthViewModel.StartupState.NeedsProfile
            sessionRejected(profile.exception) -> AuthViewModel.StartupState.NeedsLogin
            else -> AuthViewModel.StartupState.RetryableError
        }
        else -> AuthViewModel.StartupState.RetryableError
    }
}

/** What the registration flow remembers: the account exists / the profile document exists. */
internal data class RegistrationFlags(val recoveringProfile: Boolean, val profileCreated: Boolean)

/**
 * The registration flags once a session check has finished. The check also runs when a scan link
 * reaches an InitActivity that is already open, possibly in the middle of the registration: if the
 * account exists but is not verified yet, or the check could not tell, the flags must stay, or
 * onboarding would let the user step back into a registration that is already done.
 */
internal fun registrationFlagsAfterCheck(
    authenticated: Boolean,
    state: AuthViewModel.StartupState,
    current: RegistrationFlags
): RegistrationFlags = when {
    state == AuthViewModel.StartupState.NeedsProfile -> RegistrationFlags(recoveringProfile = true, profileCreated = false)
    state == AuthViewModel.StartupState.RetryableError -> current
    authenticated && state == AuthViewModel.StartupState.NeedsLogin -> current
    else -> RegistrationFlags(recoveringProfile = false, profileCreated = false)
}

/** How long the startup check waits for the profile before offering a retry. */
internal const val PROFILE_CHECK_TIMEOUT_MS = 10_000L

/**
 * Runs [load], or gives up after [timeoutMs] with a failure that is not "profile missing", so
 * [startupStateFor] reports it as retryable. Without it a slow network that neither answers nor
 * fails leaves the splash up for ever, with no message and no way to retry.
 */
internal suspend fun profileOrTimeout(
    timeoutMs: Long,
    load: suspend () -> Response<CustomUser>
): Response<CustomUser> =
    withTimeoutOrNull(timeoutMs) { load() } ?: Response.Failure(TimeoutException("Profile check timed out"))
