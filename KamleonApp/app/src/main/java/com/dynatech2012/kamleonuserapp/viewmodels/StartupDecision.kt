package com.dynatech2012.kamleonuserapp.viewmodels

import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.network.FailureKind
import com.dynatech2012.kamleonuserapp.network.failureKindOf
import com.dynatech2012.kamleonuserapp.network.isConnectionProblem
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException

/**
 * The server answered, and refused the signed-in user: the account was disabled or deleted, or the
 * session revoked. Retrying cannot fix that, only logging in again can, so it must not be shown as
 * a connection problem. Network and server trouble (unavailable, timeout, ...) stays retryable.
 */
internal fun isSessionRejected(e: Exception): Boolean = failureKindOf(e) == FailureKind.SessionRejected

/**
 * The profile could not be read because of the network or a busy server, which a new attempt can
 * fix. Anything else (an internal error, a profile that cannot be parsed) may never go away by
 * retrying, so it also offers a logout.
 */
internal fun isTransientFailure(e: Exception): Boolean = e.isConnectionProblem()

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
 * NOTE: nothing checks the account again when the connection returns; the first call the server
 * refuses with a session error reaches MainActivity through SessionWatcher.
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

/**
 * Whether the email counts as verified. The signed-in user is cached, and the email is verified
 * outside the app (a link), so a cached "not verified" may be stale: ask the server with [reload],
 * which answers null when it has no answer (the network layer applies the time limit and catches
 * errors). No answer counts as not verified, so the user is asked instead of the splash waiting.
 */
internal suspend fun verifiedAfterReload(
    cachedVerified: Boolean,
    reload: suspend () -> Boolean?
): Boolean = cachedVerified || (reload() ?: false)


/**
 * Runs the startup check and turns an unexpected exception into a failure the caller can show. Without it
 * an exception in the coroutine would leave the screen on the splash for ever. Cancellation is not a
 * failure: it means a newer check took over, so it propagates.
 */
internal suspend fun <T> guardedStartup(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
