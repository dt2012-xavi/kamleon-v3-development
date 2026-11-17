package com.dynatech2012.kamleonuserapp.utils

/** What MainActivity does about the notification permission when it opens. */
internal enum class NotificationAsk {
    /** Nothing: it was asked already, or asking now would get in the way. */
    Skip,

    /** No question to ask (granted already, or Android before 13): just remember it. */
    MarkAsked,

    /** Explain what the notifications are for, then show the system dialog. */
    ExplainFirst
}

/**
 * The onboarding asks new users with an explanation. Someone who arrives with an account that
 * already exists (another phone, a reinstall) has not seen it, so Main explains first instead of
 * showing the bare system dialog. [fromScanLink]: the scan result sheet is about to appear, and a
 * second sheet on top of it would be confusing; the flag stays unset, so it is asked next time.
 */
internal fun notificationAskFor(
    sdkInt: Int,
    alreadyAsked: Boolean,
    granted: Boolean,
    fromScanLink: Boolean
): NotificationAsk = when {
    alreadyAsked -> NotificationAsk.Skip
    sdkInt < 33 || granted -> NotificationAsk.MarkAsked
    fromScanLink -> NotificationAsk.Skip
    else -> NotificationAsk.ExplainFirst
}
