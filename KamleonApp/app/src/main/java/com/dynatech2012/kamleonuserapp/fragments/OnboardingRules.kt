package com.dynatech2012.kamleonuserapp.fragments

/**
 * Whether the "verification email sent" state may run the finishing step again.
 *
 * Only the confirmation of a resend asked for from the final dialog may: that dialog closes when
 * "Resend" is pressed and has to come back. The confirmation of the first email, requested when
 * the account is created, can arrive several seconds later while the user is already on the last
 * step choosing a value, and must not finish the onboarding for them.
 */
internal fun finishAfterVerificationEmailSent(step: Int, resendPending: Boolean): Boolean =
    resendPending && step == OnboardingSteps.COUNT - 1
