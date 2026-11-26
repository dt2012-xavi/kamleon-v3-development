package com.dynatech2012.kamleonuserapp.repositories

import timber.log.Timber
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.network.CallKind
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 *
 * <font color="teal">
 * Edu: <br></br>
 * Http request to notify firebase that an invitation has been accepted or denied.
 * This will fire a cloud function in firebase
</font> */
class CloudFunctions(
    private val userRepository: UserRepository,
    private val gate: NetworkGate
) {
    private val functions = Firebase.functions
    private val uuid: String?
        get() = if (Constants.DEBUG_MODE) FirebaseConstants.USER_UID_DEBUG else userRepository.uuid
    private val email: String?
        get() = userRepository.email

    /**
     * Emails a link to set a new password. The V2 function answers the same whether the account
     * exists or not (so it does not reveal who has one) and sends at most one email per minute and
     * user: success only means the call went through.
     */
    suspend fun resetPwd(email: String): ResponseNullable<Nothing> =
        callEmailFunction("sendPasswordEmailV2", hashMapOf("email" to email, "locale" to emailLocale()))

    suspend fun sendVerificationEmail(
        userId: String,
        email: String,
        username: String
    ): ResponseNullable<Nothing> = callEmailFunction(
        "sendEmailVerifyEmailV2",
        hashMapOf("id" to userId, "email" to email, "userName" to username, "locale" to emailLocale())
    )

    /** A function that sends an email: an action, done when the call does not fail. */
    private suspend fun callEmailFunction(
        name: String,
        body: HashMap<String, String>
    ): ResponseNullable<Nothing> = gate.safeCallNullable(CallKind.Action) {
        functions.getHttpsCallable(name).call(body).await()
        Timber.d("Email function %s answered", name)
    }

    /** The language of the emails: the phone's, when the functions have it. */
    private fun emailLocale(): String = emailLocaleFor(Locale.getDefault().language)

    companion object {
        val TAG: String = CloudFunctions::class.java.simpleName

        /**
         * The functions' locale for a phone [language] (ISO 639): Spanish and Catalan (`cat`, as the
         * web sends it), English for anything else, which is also the functions' own fallback.
         */
        fun emailLocaleFor(language: String): String = when (language.lowercase(Locale.ROOT)) {
            "es" -> "es"
            "ca" -> "cat"
            else -> "en"
        }
    }
}