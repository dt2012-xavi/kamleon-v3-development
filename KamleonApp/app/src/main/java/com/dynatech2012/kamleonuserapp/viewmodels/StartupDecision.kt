package com.dynatech2012.kamleonuserapp.viewmodels

import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException

internal fun startupStateFor(
    authenticated: Boolean,
    verified: Boolean,
    profile: Response<CustomUser>?
): AuthViewModel.StartupState {
    if (!authenticated) return AuthViewModel.StartupState.NeedsLogin
    return when (profile) {
        is Response.Success -> when {
            !verified -> AuthViewModel.StartupState.NeedsLogin
            profile.data.legal.privacyPolicyApp -> AuthViewModel.StartupState.Ready
            else -> AuthViewModel.StartupState.NeedsPolicy
        }
        is Response.Failure ->
            if (profile.exception is UserProfileMissingException) AuthViewModel.StartupState.NeedsProfile
            else AuthViewModel.StartupState.RetryableError
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
