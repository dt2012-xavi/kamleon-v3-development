package com.kamleonapp

import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.KamleonLegal
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel
import com.dynatech2012.kamleonuserapp.viewmodels.startupStateFor
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupDecisionTest {
    @Test fun signedOutUserNeedsLogin() {
        assertEquals(AuthViewModel.StartupState.NeedsLogin, startupStateFor(false, false, null))
    }

    @Test fun missingProfileCanBeRecoveredEvenBeforeVerification() {
        assertEquals(
            AuthViewModel.StartupState.NeedsProfile,
            startupStateFor(true, false, Response.Failure(UserProfileMissingException()))
        )
    }

    @Test fun networkFailureIsRetryable() {
        assertEquals(
            AuthViewModel.StartupState.RetryableError,
            startupStateFor(true, true, Response.Failure(java.io.IOException("offline")))
        )
    }

    @Test fun verifiedUserWithAcceptedPolicyIsReady() {
        val user = CustomUser(legal = KamleonLegal(privacyPolicyApp = true))
        assertEquals(
            AuthViewModel.StartupState.Ready,
            startupStateFor(true, true, Response.Success(user))
        )
    }

    @Test fun policyAndVerificationRemainRequired() {
        val user = CustomUser()
        assertEquals(
            AuthViewModel.StartupState.NeedsPolicy,
            startupStateFor(true, true, Response.Success(user))
        )
        assertEquals(
            AuthViewModel.StartupState.NeedsVerification,
            startupStateFor(true, false, Response.Success(user))
        )
    }
}
