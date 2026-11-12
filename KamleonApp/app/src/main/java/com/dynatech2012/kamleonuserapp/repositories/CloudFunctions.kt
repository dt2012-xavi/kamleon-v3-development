package com.dynatech2012.kamleonuserapp.repositories

import timber.log.Timber
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.InvitationStatus
import com.dynatech2012.kamleonuserapp.models.Organization
import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 *
 * <font color="teal">
 * Edu: <br></br>
 * Http request to notify firebase that an invitation has been accepted or denied.
 * This will fire a cloud function in firebase
</font> */
class CloudFunctions(private val userRepository: UserRepository) {
    private val functions = Firebase.functions
    private val uuid: String?
        get() = if (Constants.DEBUG_MODE) FirebaseConstants.USER_UID_DEBUG else userRepository.uuid
    private val email: String?
        get() = userRepository.email

    // Invitations
    suspend fun getInvitations(): Response<ArrayList<Invitation>> =
        suspendCoroutine { continuation ->
            Timber.d("Loading invitations")

            if (email == null) {
                continuation.resume(Response.Failure(Exception("User not logged in")))
                return@suspendCoroutine
            }
            val body = hashMapOf(
                "email" to email
            )
            functions.getHttpsCallable("getInvitationsForUser").call(body)
                .addOnSuccessListener { result ->
                    val data = result?.data as? ArrayList<HashMap<String, Any>> ?: ArrayList()
                    val invitations = ArrayList<Invitation>()
                    data.forEach {

                        val inv = Invitation(it)
                        invitations.add(inv)
                    }
                    Timber.d("Invitations loaded: count=%s", invitations.size)
                    invitations.sortByDescending { it.dateSent }
                    continuation.resume(Response.Success(invitations))
                }
                .addOnFailureListener { exception ->
                    Timber.w("Loading invitations failed: %s", exception.javaClass.simpleName)
                    continuation.resume(Response.Failure(exception))
                }
        }

    suspend fun acceptInvitation(
        invitationId: String,
        optional: Boolean
    ): ResponseNullable<Nothing> {
        return changeInvitationStatus(invitationId, InvitationStatus.ACCEPTED, optional)
    }

    suspend fun rejectInvitation(invitationId: String): ResponseNullable<Nothing> {
        return changeInvitationStatus(invitationId, InvitationStatus.REJECTED, false)
    }

    private suspend fun changeInvitationStatus(
        invitationId: String,
        status: InvitationStatus,
        optional: Boolean?
    ): ResponseNullable<Nothing> {// = suspendCoroutine { continuation ->
        if (uuid == null) {
            /*continuation.resume(ResponseNullable.Failure(Exception("User not logged in")))
            return@suspendCoroutine
            */
            return ResponseNullable.Failure(Exception("User not logged in"))
        }

        val body = hashMapOf(
            "invitationID" to invitationId,
            "userID" to uuid,
            "status" to status.rawValue,
            "roster" to (optional ?: false)
        )
        Timber.d("Changing invitation status")
        try {
            val result = functions.getHttpsCallable("changeInvitationStatus").call(body).await()
            val data = result.data as? ArrayList<HashMap<String, Any>>
            val invitations = ArrayList<Invitation>()
            data?.forEach {
                val inv = Invitation(it)
                invitations.add(inv)
            }
            Timber.d("Invitation status changed")
            //continuation.resume(ResponseNullable.Success())
            return ResponseNullable.Success()
        } catch (e: Exception) {
            Timber.w("Changing invitation status failed: %s", e.javaClass.simpleName)
            //continuation.resume(ResponseNullable.Failure(e))
            return ResponseNullable.Failure(e)
        }
    }

    // User profiles
    suspend fun getUserProfiles(): Response<ArrayList<Organization>> =
        suspendCoroutine { continuation ->
            Timber.d("Loading user profiles")
            if (uuid == null) {
                continuation.resume(Response.Failure(Exception("User not logged in")))
                return@suspendCoroutine
            }
            val body = hashMapOf(
                "id" to uuid
            )
            functions.getHttpsCallable("getUserProfiles").call(body)
                .addOnSuccessListener { result ->
                    val data = result?.data as? ArrayList<HashMap<String, Any>> ?: ArrayList()

                    val organizations = ArrayList<Organization>()
                    data.forEach {
                        val organization = Organization(it)
                        organizations.add(organization)
                    }
                    Timber.d("User profiles loaded: count=%s", organizations.size)
                    continuation.resume(Response.Success(organizations))
                }
                .addOnFailureListener { exception ->
                    Timber.w("Loading user profiles failed: %s", exception.javaClass.simpleName)
                    continuation.resume(Response.Failure(exception))
                }
        }

    suspend fun resetPwd(email: String): ResponseNullable<Nothing> {
        val body = hashMapOf("email" to email)
        return try {
            val result = functions.getHttpsCallable("sendPasswordEmail").call(body).await()
            val data = result.data as? HashMap<String, String>
            val message = data?.get("message")
            if (message != "Reset Password email sent successfully") {
                throw Exception("$message")
            }
            Timber.d("Password reset email requested successfully")
            ResponseNullable.Success()
        } catch (e: Exception) {
            Timber.w("Password reset email request failed: %s", e.javaClass.simpleName)
            ResponseNullable.Failure(e)
        }
    }

    suspend fun resetPin(email: String, userId: String): ResponseNullable<Nothing> {
        val body = hashMapOf(
            "email" to email,
            "id" to userId
        )
        return try {
            val result = functions.getHttpsCallable("sendPINEmail").call(body).await()
            val data = result.data as? HashMap<String, String>
            val message = data?.get("message")
            if (message != "Reset PIN email sent successfully") {
                throw Exception("$message")
            }
            Timber.d("PIN reset email requested successfully")
            ResponseNullable.Success()
        } catch (e: Exception) {
            Timber.w("PIN reset email request failed: %s", e.javaClass.simpleName)
            ResponseNullable.Failure(e)
        }
    }

    suspend fun sendVerificationEmail(
        userId: String,
        email: String,
        username: String
    ): ResponseNullable<Nothing> {
        val body = hashMapOf(
            "id" to userId,
            "email" to email,
            "username" to username
        )
        return try {
            val result = functions.getHttpsCallable("sendEmailVerifyEmail").call(body).await()
            val data = result.data as? HashMap<String, String>
            val message = data?.get("message")
            if (message != "Verify email sent successfully") {
                //throw Exception("$message")
            }
            Timber.d("Verification email requested successfully")
            ResponseNullable.Success()
        } catch (e: Exception) {
            Timber.w("Verification email request failed: %s", e.javaClass.simpleName)
            ResponseNullable.Failure(e)
        }
    }

    suspend fun sendTrialEmail(email: String): ResponseNullable<Nothing> {
        val body = hashMapOf("email" to email)
        return try {
            val result = functions.getHttpsCallable("sendPremiumInterestEmail").call(body).await()
            val data = result.data as? HashMap<String, String>
            val message = data?.get("message")
            if (message != "Verify email sent successfully") {
                //throw Exception("$message")
            }
            Timber.d("Trial email requested successfully")
            ResponseNullable.Success()
        } catch (e: Exception) {
            Timber.w("Trial email request failed: %s", e.javaClass.simpleName)
            ResponseNullable.Failure(e)
        }
    }

    companion object {
        val TAG: String = CloudFunctions::class.java.simpleName
    }
}