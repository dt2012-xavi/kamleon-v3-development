package com.dynatech2012.kamleonuserapp.constants

/**
 *
 * <font color="teal">
 * Edu:<br></br>
 * Constant class. It is final to prevent be extended <br></br>
 * It has a private constructor to prevent being instantiated
</font> */
object UrlConstants {
    private const val BASE_URL = "https://kamleon-v3-pre.vercel.app/"

    const val URL_CONSENT = "${BASE_URL}legal/consent"
    const val URL_TERMS = "${BASE_URL}legal/terms"
    const val URL_POLICY = "${BASE_URL}legal/user/policy"
    const val URL_POLICY_APP = "${BASE_URL}legal/app/policy"
    const val URL_POLICY_ADMIN = "${BASE_URL}legal/admin/policy"
}