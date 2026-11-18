package com.kamleonapp

import com.dynatech2012.kamleonuserapp.viewmodels.AuthViewModel.StartupState
import com.dynatech2012.kamleonuserapp.viewmodels.RegistrationFlags
import com.dynatech2012.kamleonuserapp.viewmodels.registrationFlagsAfterCheck
import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrationFlagsTest {
    private val midRegistration = RegistrationFlags(recoveringProfile = true, profileCreated = true)
    private val nothing = RegistrationFlags(recoveringProfile = false, profileCreated = false)

    @Test fun accountWaitingForVerificationKeepsTheRegistrationState() {
        // A scan link can arrive after the account was created and before the email is verified.
        assertEquals(
            midRegistration,
            registrationFlagsAfterCheck(authenticated = true, StartupState.NeedsLogin, midRegistration)
        )
    }

    @Test fun inconclusiveCheckKeepsTheRegistrationState() {
        assertEquals(
            midRegistration,
            registrationFlagsAfterCheck(authenticated = true, StartupState.RetryableError, midRegistration)
        )
    }

    @Test fun aServerErrorKeepsTheRegistrationStateToo() {
        assertEquals(
            midRegistration,
            registrationFlagsAfterCheck(authenticated = true, StartupState.ServerError, midRegistration)
        )
    }

    @Test fun missingProfileStartsRecovery() {
        assertEquals(
            RegistrationFlags(recoveringProfile = true, profileCreated = false),
            registrationFlagsAfterCheck(authenticated = true, StartupState.NeedsProfile, midRegistration)
        )
    }

    @Test fun signedOutUserLosesTheRegistrationState() {
        assertEquals(
            nothing,
            registrationFlagsAfterCheck(authenticated = false, StartupState.NeedsLogin, midRegistration)
        )
    }

    @Test fun completedAccountLosesTheRegistrationState() {
        assertEquals(nothing, registrationFlagsAfterCheck(true, StartupState.Ready, midRegistration))
        assertEquals(nothing, registrationFlagsAfterCheck(true, StartupState.NeedsPolicy, midRegistration))
    }

    @Test fun freshStartupStaysClean() {
        StartupState.entries.forEach { state ->
            val expected = if (state == StartupState.NeedsProfile) {
                RegistrationFlags(recoveringProfile = true, profileCreated = false)
            } else {
                nothing
            }
            assertEquals(expected, registrationFlagsAfterCheck(true, state, nothing))
        }
    }
}
