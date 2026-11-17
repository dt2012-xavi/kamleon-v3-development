package com.dynatech2012.kamleonuserapp.security

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/** Why a password was rejected. Only [WEAK] and [BREACHED] are actionable beyond the length hint. */
enum class PasswordPolicyReason { TOO_SHORT, TOO_LONG, WEAK, BREACHED }

sealed interface PasswordPolicyResult {
    data object Valid : PasswordPolicyResult
    data class Rejected(val reason: PasswordPolicyReason) : PasswordPolicyResult
}

/** Length limits shared by the policy and the UI feedback. */
object PasswordLength {
    const val MIN = 8
    // Upper bound so enormous inputs are never hashed or scored.
    const val MAX = 128

    fun isLongEnough(password: String) = password.length in MIN..MAX
}

/**
 * Password policy for new passwords (sign up and change password), mirroring the web app's server
 * policy: length 8..128, zxcvbn strength, then a Have I Been Pwned check. No composition rules.
 *
 * Checks run in order of cost: length (no work), strength (local), breach check (network). The
 * breach check fails open: if it cannot complete the user is not blocked and nothing is shown; the
 * strength filter is then the only barrier.
 */
/** Carries a policy rejection through `Response.Failure` so the UI can localise it. */
class PasswordPolicyException(val reason: PasswordPolicyReason) : Exception("Password rejected: $reason")

class PasswordPolicy(
    private val strengthEstimator: PasswordStrengthEstimator,
    private val breachChecker: PasswordBreachChecker,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    /**
     * Live feedback for the strength bar: anything outside the length limits is weak right away
     * (no scoring needed), otherwise the zxcvbn score.
     */
    suspend fun estimateStrength(password: String): PasswordStrength {
        if (!PasswordLength.isLongEnough(password)) return PasswordStrength.WEAK
        return withContext(computeDispatcher) { strengthEstimator.estimate(password) }
    }

    suspend fun validate(password: String): PasswordPolicyResult {
        if (password.length < PasswordLength.MIN) return PasswordPolicyResult.Rejected(PasswordPolicyReason.TOO_SHORT)
        if (password.length > PasswordLength.MAX) return PasswordPolicyResult.Rejected(PasswordPolicyReason.TOO_LONG)

        val strength = withContext(computeDispatcher) { strengthEstimator.estimate(password) }
        if (strength == PasswordStrength.WEAK) return PasswordPolicyResult.Rejected(PasswordPolicyReason.WEAK)

        val breached = try {
            breachChecker.isBreached(password)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Fail open. Never log the password or anything derived from it.
            Timber.w("Password breach check unavailable (%s); skipping it", e.javaClass.simpleName)
            false
        }
        return if (breached) PasswordPolicyResult.Rejected(PasswordPolicyReason.BREACHED) else PasswordPolicyResult.Valid
    }
}
