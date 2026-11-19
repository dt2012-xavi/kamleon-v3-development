package com.kamleonapp

import com.dynatech2012.kamleonuserapp.activities.StartupAction
import com.dynatech2012.kamleonuserapp.activities.StartupRoute
import com.dynatech2012.kamleonuserapp.activities.StartupScreen
import com.dynatech2012.kamleonuserapp.activities.startupRouteFor
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel.StartupState
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupRouteTest {
    private val none = StartupRoute(StartupAction.None)

    private fun route(state: StartupState?, current: StartupScreen, restored: Boolean = false) =
        startupRouteFor(state, current, restored)

    @Test fun readyAlwaysOpensMain() {
        StartupScreen.entries.forEach { screen ->
            assertEquals(StartupRoute(StartupAction.OpenMain), route(StartupState.Ready, screen))
            assertEquals(StartupRoute(StartupAction.OpenMain), route(StartupState.Ready, screen, restored = true))
        }
    }

    @Test fun checkingAndNothingDoNothing() {
        StartupScreen.entries.forEach { screen ->
            assertEquals(none, route(StartupState.Checking, screen, restored = true))
            assertEquals(none, route(null, screen, restored = true))
        }
    }

    @Test fun signedOutLeavesTheSplashForLogin() {
        assertEquals(
            StartupRoute(StartupAction.GoTo(StartupScreen.Login), endsRestoredFlow = true),
            route(StartupState.NeedsLogin, StartupScreen.Splash)
        )
    }

    @Test fun signedOutDoesNotPullTheUserOutOfAScreenTheyAreUsing() {
        // A scan link can reach an InitActivity that is already open on any of these screens.
        listOf(StartupScreen.Login, StartupScreen.Register, StartupScreen.Other).forEach { screen ->
            assertEquals(none, route(StartupState.NeedsLogin, screen))
        }
    }

    @Test fun signedOutRestoredFlowEndsEvenWhenAlreadyOnLogin() {
        assertEquals(
            StartupRoute(StartupAction.None, endsRestoredFlow = true),
            route(StartupState.NeedsLogin, StartupScreen.Login, restored = true)
        )
        assertEquals(
            StartupRoute(StartupAction.GoTo(StartupScreen.Login), endsRestoredFlow = true),
            route(StartupState.NeedsLogin, StartupScreen.Other, restored = true)
        )
    }

    @Test fun pendingPolicyIsShownRightAwayFromTheSplashOrARestoredFlow() {
        // The user is already signed in: they must not type the password just to see the sheet.
        val showPolicy = StartupRoute(StartupAction.ShowPolicy, endsRestoredFlow = true)
        assertEquals(showPolicy, route(StartupState.NeedsPolicy, StartupScreen.Splash))
        StartupScreen.entries.forEach { screen ->
            assertEquals(showPolicy, route(StartupState.NeedsPolicy, screen, restored = true))
        }
    }

    @Test fun pendingPolicyDoesNotInterruptAScreenTheUserIsUsing() {
        listOf(StartupScreen.Login, StartupScreen.Register, StartupScreen.Other).forEach { screen ->
            assertEquals(none, route(StartupState.NeedsPolicy, screen))
        }
    }

    @Test fun missingProfileGoesToRegisterFromSplashOrLogin() {
        val toRegister = StartupRoute(StartupAction.GoTo(StartupScreen.Register), endsRestoredFlow = true)
        assertEquals(toRegister, route(StartupState.NeedsProfile, StartupScreen.Splash))
        assertEquals(toRegister, route(StartupState.NeedsProfile, StartupScreen.Login))
    }

    @Test fun missingProfileLeavesARegistrationInProgressAlone() {
        assertEquals(none, route(StartupState.NeedsProfile, StartupScreen.Register))
        assertEquals(none, route(StartupState.NeedsProfile, StartupScreen.Other))
        assertEquals(
            StartupRoute(StartupAction.None, endsRestoredFlow = true),
            route(StartupState.NeedsProfile, StartupScreen.Register, restored = true)
        )
        assertEquals(
            StartupRoute(StartupAction.GoTo(StartupScreen.Register), endsRestoredFlow = true),
            route(StartupState.NeedsProfile, StartupScreen.Other, restored = true)
        )
    }

    @Test fun aServerErrorIsShownWhereANetworkErrorIs_withItsOwnSheet() {
        val showServerError = StartupRoute(StartupAction.ShowServerError)
        assertEquals(showServerError, route(StartupState.ServerError, StartupScreen.Splash))
        assertEquals(showServerError, route(StartupState.ServerError, StartupScreen.Login, restored = true))
        listOf(StartupScreen.Login, StartupScreen.Register, StartupScreen.Other).forEach { screen ->
            assertEquals(none, route(StartupState.ServerError, screen))
        }
    }

    @Test fun anUnverifiedEmailIsExplainedFromTheSplashOrARestoredFlow() {
        val showVerification = StartupRoute(StartupAction.ShowVerification, endsRestoredFlow = true)
        assertEquals(showVerification, route(StartupState.NeedsVerification, StartupScreen.Splash))
        StartupScreen.entries.forEach { screen ->
            assertEquals(showVerification, route(StartupState.NeedsVerification, screen, restored = true))
        }
        // A registration in progress (or a login being typed) is not interrupted by a scan link.
        listOf(StartupScreen.Login, StartupScreen.Register, StartupScreen.Other).forEach { screen ->
            assertEquals(none, route(StartupState.NeedsVerification, screen))
        }
    }

    @Test fun anEndedSessionIsExplainedWhereTheOtherStartupErrorsAre() {
        val showEnded = StartupRoute(StartupAction.ShowSessionEnded)
        assertEquals(showEnded, route(StartupState.SessionRejected, StartupScreen.Splash))
        assertEquals(showEnded, route(StartupState.SessionRejected, StartupScreen.Login, restored = true))
        listOf(StartupScreen.Login, StartupScreen.Register, StartupScreen.Other).forEach { screen ->
            assertEquals(none, route(StartupState.SessionRejected, screen))
        }
    }

    @Test fun errorIsShownOnTheSplashOrInARestoredFlowOnly() {
        val showError = StartupRoute(StartupAction.ShowError)
        assertEquals(showError, route(StartupState.RetryableError, StartupScreen.Splash))
        assertEquals(showError, route(StartupState.RetryableError, StartupScreen.Login, restored = true))
        listOf(StartupScreen.Login, StartupScreen.Register, StartupScreen.Other).forEach { screen ->
            assertEquals(none, route(StartupState.RetryableError, screen))
        }
    }
}
