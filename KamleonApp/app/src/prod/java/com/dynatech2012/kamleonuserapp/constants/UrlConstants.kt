package com.dynatech2012.kamleonuserapp.constants

/**
 *
 * <font color="teal">
 * Edu:<br></br>
 * Constant class. It is final to prevent be extended <br></br>
 * It has a private constructor to prevent being instantiated
</font> */
object UrlConstants {
    private const val BASE_URL = "https://kamleon.app/"

    const val URL_CONSENT = "${BASE_URL}legal/consent"
    const val URL_TERMS = "${BASE_URL}legal/terms"
    const val URL_POLICY = "${BASE_URL}legal/user/policy"
    const val URL_POLICY_APP = "${BASE_URL}legal/app/policy"
    const val URL_POLICY_ADMIN = "${BASE_URL}legal/admin/policy"
    const val URL_DELETE = "${BASE_URL}request-data-deletion"
    const val URL_DEACTIVATE = "${BASE_URL}request-deactivate-account"
}