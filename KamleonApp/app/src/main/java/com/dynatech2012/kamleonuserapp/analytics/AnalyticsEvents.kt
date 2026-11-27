package com.dynatech2012.kamleonuserapp.analytics

/**
 * The Mixpanel events the app sends: the web's names where the web has the same thing (see the web's
 * `lib/analytics/events.ts` and its spec `2026-11-26-mixpanel-android-app-events-design.md`).
 */
object AnalyticsEvents {
    // Access
    const val SIGNUP_STARTED = "Signup Started"
    const val SIGNUP_STEP2_VIEWED = "Signup Step2 Viewed"
    const val SIGNUP_COMPLETED = "Signup Completed"
    const val SIGNUP_FAILED = "Signup Failed"
    const val SIGNUP_PROFILE_STEP_COMPLETED = "Signup Profile Step Completed"
    const val VERIFICATION_EMAIL_SENT = "Verification Email Sent"
    const val VERIFICATION_RESEND_CLICKED = "Verification Resend Clicked"
    const val EMAIL_VERIFIED = "Email Verified"
    const val LOGIN_ATTEMPTED = "Login Attempted"
    const val LOGIN_SUCCEEDED = "Login Succeeded"
    const val LOGIN_FAILED = "Login Failed"
    const val LOGIN_BLOCKED_UNVERIFIED_EMAIL = "Login Blocked Unverified Email"
    const val FORGOT_PASSWORD_REQUESTED = "Forgot Password Requested"
    const val FORGOT_PASSWORD_FAILED = "Forgot Password Failed"

    // Access attempts (web: lib/analytics/auth-flow-tracker.ts)
    const val AUTH_FLOW_STARTED = "Auth Flow Started"
    const val AUTH_STEP_COMPLETED = "Auth Step Completed"
    const val AUTH_FLOW_COMPLETED = "Auth Flow Completed"
    const val AUTH_FLOW_ABANDONED = "Auth Flow Abandoned"

    // Use
    const val PAGE_VIEWED = "Page Viewed"
    const val DRAWER_OPENED = "Drawer Opened"
    const val DRAWER_CLOSED = "Drawer Closed"
    const val INVITATION_ACCEPTED = "Invitation Accepted"
    const val INVITATION_DECLINED = "Invitation Declined"

    // QR process
    const val QR_FLOW_STARTED = "QR Flow Started"
    const val QR_FLOW_STEP = "QR Flow Step"
    const val QR_FLOW_FINISHED = "QR Flow Finished"
}
