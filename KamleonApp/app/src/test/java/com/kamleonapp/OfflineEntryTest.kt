package com.kamleonapp

import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.KamleonLegal
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel.StartupState
import com.dynatech2012.kamleonuserapp.viewmodels.canContinueOffline
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Opening the app without a connection, with the profile the phone already has. The server could not
 * be asked, so this is only for a network failure and only when the cached account was complete.
 */
class OfflineEntryTest {
    private val complete = Response.Success(CustomUser(legal = KamleonLegal(privacyPolicyApp = true)))

    @Test fun aNetworkFailureWithACompleteCachedAccountCanContinueOffline() {
        assertTrue(canContinueOffline(StartupState.RetryableError, complete, verified = true))
    }

    @Test fun anIncompleteCachedAccountCannot() {
        val policyPending = Response.Success(CustomUser(legal = KamleonLegal(privacyPolicyApp = false)))
        assertFalse(canContinueOffline(StartupState.RetryableError, policyPending, verified = true))
        assertFalse(canContinueOffline(StartupState.RetryableError, complete, verified = false))
    }

    @Test fun noCachedProfileMeansNoOfflineEntry() {
        assertFalse(canContinueOffline(StartupState.RetryableError, null, verified = true))
        assertFalse(canContinueOffline(StartupState.RetryableError, Response.Failure(Exception("not cached")), verified = true))
    }

    @Test fun onlyANetworkFailureQualifies() {
        // The server answered in these states: skipping its check would hide a refused or broken account.
        StartupState.entries.filter { it != StartupState.RetryableError }.forEach {
            assertFalse(it.name, canContinueOffline(it, complete, verified = true))
        }
    }
}
