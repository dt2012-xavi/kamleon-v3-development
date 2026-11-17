package com.kamleonapp

import com.dynatech2012.kamleonuserapp.fragments.OnboardingSteps
import com.dynatech2012.kamleonuserapp.fragments.finishAfterVerificationEmailSent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRulesTest {
    private val lastStep = OnboardingSteps.COUNT - 1

    @Test fun firstEmailArrivingLateDoesNotFinishTheUserOnTheLastStep() {
        // The email requested when the account is created can take several seconds: the user may
        // already be choosing the gender, which must not be skipped.
        assertFalse(finishAfterVerificationEmailSent(step = lastStep, resendPending = false))
    }

    @Test fun confirmationOfAResendFinishesAgainOnTheLastStep() {
        assertTrue(finishAfterVerificationEmailSent(step = lastStep, resendPending = true))
    }

    @Test fun otherStepsNeverFinish() {
        (OnboardingSteps.BIRTH_DATE until lastStep).forEach { step ->
            assertFalse(finishAfterVerificationEmailSent(step = step, resendPending = false))
            assertFalse(finishAfterVerificationEmailSent(step = step, resendPending = true))
        }
    }
}
