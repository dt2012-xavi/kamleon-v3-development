package com.dynatech2012.kamleonuserapp.viewmodels

import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
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

/**
 * The profile could not be read because of the network or a busy server, which a new attempt can
 * fix. Anything else (an internal error, a profile that cannot be parsed) may never go away by
 * retrying, so it also offers a logout. Codes are matched inside the function, not in a constant,
 * because Firestore's enum needs real Android classes to load.
 */
internal fun isTransientFailure(e: Exception): Boolean = when (e) {
    is TimeoutException, is IOException, is FirebaseNetworkException -> true
    is FirebaseFirestoreException -> when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
        FirebaseFirestoreException.Code.CANCELLED -> true
        else -> false
    }
    else -> false
}

internal fun startupStateFor(
    authenticated: Boolean,
    verified: Boolean,
    profile: Response<CustomUser>?,
    sessionRejected: (Exception) -> Boolean = ::isSessionRejected,
    transient: (Exception) -> Boolean = ::isTransientFailure
): AuthViewModel.StartupState {
    if (!authenticated) return AuthViewModel.StartupState.NeedsLogin
    return when (profile) {
        is Response.Success -> when {
            !verified -> AuthViewModel.StartupState.NeedsVerification
            profile.data.legal.privacyPolicyApp -> AuthViewModel.StartupState.Ready
            else -> AuthViewModel.StartupState.NeedsPolicy
        }
        is Response.Failure -> when {
            profile.exception is UserProfileMissingException -> AuthViewModel.StartupState.NeedsProfile
            sessionRejected(profile.exception) -> AuthViewModel.StartupState.SessionRejected
            transient(profile.exception) -> AuthViewModel.StartupState.RetryableError
            else -> AuthViewModel.StartupState.ServerError
        }
        else -> AuthViewModel.StartupState.RetryableError
    }
}

/**
 * Whether the user may open the app with the profile cached on the phone because the server could not
 * be reached. Only for a network failure ([StartupState.RetryableError]) and a complete cached account
 * (verified, policy accepted): any state where the server did answer must not skip its check.
 *
 * NOTE: nothing checks the account again when the connection returns. MainActivity has no handling for
 * a refused session (see StartupState.SessionRejected); add it when MainActivity is next touched.
 */
internal fun canContinueOffline(
    state: AuthViewModel.StartupState,
    cached: Response<CustomUser>?,
    verified: Boolean
): Boolean = state == AuthViewModel.StartupState.RetryableError && verified &&
    cached is Response.Success && cached.data.legal.privacyPolicyApp

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
    state == AuthViewModel.StartupState.RetryableError || state == AuthViewModel.StartupState.ServerError -> current
    authenticated && (state == AuthViewModel.StartupState.NeedsLogin ||
        state == AuthViewModel.StartupState.NeedsVerification) -> current
    else -> RegistrationFlags(recoveringProfile = false, profileCreated = false)
}

/** How long the startup check waits for the server to say whether the email is verified now. */
internal const val EMAIL_RELOAD_TIMEOUT_MS = 5_000L

/**
 * Whether the email counts as verified. The signed-in user is cached, and the email is verified
 * outside the app (a link), so a cached "not verified" may be stale: ask the server with [reload],
 * which answers null when it cannot. No answer, an error or [timeoutMs] count as not verified, so the
 * user is asked instead of the splash waiting.
 */
internal suspend fun verifiedAfterReload(
    cachedVerified: Boolean,
    timeoutMs: Long,
    reload: suspend () -> Boolean?
): Boolean {
    if (cachedVerified) return true
    return try {
        withTimeoutOrNull(timeoutMs) { reload() } ?: false
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
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
