package com.dynatech2012.kamleonuserapp.security

import com.nulabinc.zxcvbn.Zxcvbn

enum class PasswordStrength { WEAK, GOOD, STRONG }

fun interface PasswordStrengthEstimator {
    /** Scores [password] locally (no network). Blocking and CPU-bound: call off the main thread. */
    fun estimate(password: String): PasswordStrength
}

/**
 * zxcvbn estimator. Same mapping as the web app: score <= 1 is weak, 2 is good, >= 3 is strong.
 */
class ZxcvbnPasswordStrengthEstimator : PasswordStrengthEstimator {
    // Building the matchers loads the dictionaries; do it once and only when first needed.
    private val zxcvbn by lazy(LazyThreadSafetyMode.PUBLICATION) { Zxcvbn() }

    override fun estimate(password: String): PasswordStrength = when (zxcvbn.measure(password).score) {
        0, 1 -> PasswordStrength.WEAK
        2 -> PasswordStrength.GOOD
        else -> PasswordStrength.STRONG
    }
}
