package com.dynatech2012.kamleonuserapp.fragments

import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Test

class PwdFieldErrorsTest {
    private fun errors(
        current: String = "old-secret-1",
        new: String = "correct-horse-1",
        confirm: String = "correct-horse-1",
        strength: PasswordStrength? = PasswordStrength.STRONG,
        showBlank: Boolean = false,
        force: Boolean = false,
        currentServerError: Int? = null,
        newServerError: Int? = null
    ) = pwdFieldErrors(current, new, confirm, strength, showBlank, force, currentServerError, newServerError)

    @Test
    fun validFormHasNoErrors() {
        assertEquals(PwdFieldErrors(null, null, null), errors(showBlank = true, force = true))
    }

    @Test
    fun blankFieldsAreReportedOnlyOnceSubmitted() {
        assertEquals(PwdFieldErrors(null, null, null), errors("", "", ""))
        assertEquals(
            PwdFieldErrors(
                R.string.field_error_password_required,
                R.string.pwd_error_new_required,
                R.string.signup_error_repeat_required
            ),
            errors("", "", "", showBlank = true, force = true)
        )
    }

    @Test
    fun sameAsCurrentIsReportedWhileTyping() {
        val e = errors(current = "same-pass-1", new = "same-pass-1", confirm = "same-pass-1")
        assertEquals(R.string.pwd_error_same_as_current, e.new)
    }

    @Test
    fun mismatchIsReportedWhileTyping() {
        assertEquals(R.string.pwd_error_dontmatch, errors(confirm = "correct-horse").confirm)
    }

    @Test
    fun lengthAndWeakStrengthAreJudgedOnSubmit() {
        assertEquals(null, errors(new = "abc", confirm = "abc").new)
        assertEquals(R.string.password_error_too_short, errors(new = "abc", confirm = "abc", force = true).new)
        val long = "a".repeat(129)
        assertEquals(R.string.password_error_too_long, errors(new = long, confirm = long, force = true).new)
        assertEquals(null, errors(strength = PasswordStrength.WEAK).new)
        assertEquals(R.string.password_error_weak, errors(strength = PasswordStrength.WEAK, force = true).new)
    }

    @Test
    fun serverVerdictsAreShownOnTheirField() {
        val e = errors(
            currentServerError = R.string.login_alert_description_wrong_pass,
            newServerError = R.string.password_error_breached
        )
        assertEquals(R.string.login_alert_description_wrong_pass, e.current)
        assertEquals(R.string.password_error_breached, e.new)
    }
}
