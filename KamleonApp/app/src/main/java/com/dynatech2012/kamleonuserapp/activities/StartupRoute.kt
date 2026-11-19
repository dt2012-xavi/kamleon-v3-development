package com.dynatech2012.kamleonuserapp.activities

import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel.StartupState

/** The screens of the startup flow that the routing decision tells apart. */
internal enum class StartupScreen { Splash, Login, Register, Other }

/** What InitActivity has to do about a startup state. */
internal sealed interface StartupAction {
    data object None : StartupAction
    data object OpenMain : StartupAction
    data object ShowError : StartupAction
    data object ShowServerError : StartupAction
    data object ShowSessionEnded : StartupAction
    data object ShowPolicy : StartupAction
    data object ShowVerification : StartupAction
    data class GoTo(val screen: StartupScreen) : StartupAction
}

/**
 * [action] to take and whether it ends the restored-flow mode ([endsRestoredFlow]): InitActivity
 * clears its flag once a state has had its say about where the user belongs.
 */
internal data class StartupRoute(val action: StartupAction, val endsRestoredFlow: Boolean = false)

/**
 * Where the startup state sends the user, given the screen they are on. A state only moves them
 * from the splash (or a restored flow, which starts from wherever Android put them): a scan link
 * can reach an InitActivity that is already open, and must not pull the user out of the login or
 * out of a registration in progress.
 */
internal fun startupRouteFor(
    state: StartupState?,
    current: StartupScreen,
    restoredFlow: Boolean
): StartupRoute {
    val fromSplash = restoredFlow || current == StartupScreen.Splash
    return when (state) {
        StartupState.Ready -> StartupRoute(StartupAction.OpenMain)

        StartupState.NeedsLogin ->
            if (fromSplash) moveTo(StartupScreen.Login, current) else StartupRoute(StartupAction.None)

        // Already signed in: the policy sheet opens over Login without asking for the password.
        StartupState.NeedsPolicy ->
            if (fromSplash) StartupRoute(StartupAction.ShowPolicy, endsRestoredFlow = true)
            else StartupRoute(StartupAction.None)

        // Signed in but the email is not verified: say so over Login instead of asking for the password.
        StartupState.NeedsVerification ->
            if (fromSplash) StartupRoute(StartupAction.ShowVerification, endsRestoredFlow = true)
            else StartupRoute(StartupAction.None)

        // A missing profile is also recovered from the login, where the user may have signed in.
        StartupState.NeedsProfile ->
            if (fromSplash || current == StartupScreen.Login) moveTo(StartupScreen.Register, current)
            else StartupRoute(StartupAction.None)

        // The restored flow stays on: the user still has to get past the error.
        StartupState.RetryableError ->
            if (fromSplash) StartupRoute(StartupAction.ShowError) else StartupRoute(StartupAction.None)

        // Same places as the network error, with a sheet that also offers to log out.
        StartupState.ServerError ->
            if (fromSplash) StartupRoute(StartupAction.ShowServerError) else StartupRoute(StartupAction.None)

        // The server refused the session: explain it before the data is wiped and Login opens.
        StartupState.SessionRejected ->
            if (fromSplash) StartupRoute(StartupAction.ShowSessionEnded) else StartupRoute(StartupAction.None)

        StartupState.Checking, null -> StartupRoute(StartupAction.None)
    }
}

private fun moveTo(target: StartupScreen, current: StartupScreen) =
    StartupRoute(
        action = if (current == target) StartupAction.None else StartupAction.GoTo(target),
        endsRestoredFlow = true
    )
