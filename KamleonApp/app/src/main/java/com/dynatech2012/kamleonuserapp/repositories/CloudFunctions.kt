package com.dynatech2012.kamleonuserapp.repositories

import timber.log.Timber
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.network.CallKind
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import kotlinx.coroutines.tasks.await

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

    suspend fun resetPwd(email: String): ResponseNullable<Nothing> =
        callEmailFunction("sendPasswordEmail", hashMapOf("email" to email), "Reset Password email sent successfully")

    suspend fun sendVerificationEmail(
        userId: String,
        email: String,
        username: String
    ): ResponseNullable<Nothing> =
        callEmailFunction("sendEmailVerifyEmail", hashMapOf("id" to userId, "email" to email, "username" to username), null)

    /** A function that sends an email: an action. [expectedMessage], when given, is what a successful answer says. */
    private suspend fun callEmailFunction(
        name: String,
        body: HashMap<String, String>,
        expectedMessage: String?
    ): ResponseNullable<Nothing> = gate.safeCallNullable(CallKind.Action) {
        val result = functions.getHttpsCallable(name).call(body).await()
        val message = (result.data as? HashMap<String, String>)?.get("message")
        if (expectedMessage != null && message != expectedMessage) throw Exception("$message")
        Timber.d("Email function %s answered", name)
    }

    companion object {
        val TAG: String = CloudFunctions::class.java.simpleName
    }
}