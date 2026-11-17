package com.dynatech2012.kamleonuserapp.security

import androidx.annotation.StringRes
import com.dynatech2012.kamleonuserapp.R

/** User-facing message for a policy rejection. */
@StringRes
fun PasswordPolicyReason.messageRes(): Int = when (this) {
    PasswordPolicyReason.TOO_SHORT -> R.string.password_error_too_short
    PasswordPolicyReason.TOO_LONG -> R.string.password_error_too_long
    PasswordPolicyReason.WEAK -> R.string.password_error_weak
    PasswordPolicyReason.BREACHED -> R.string.password_error_breached
}
