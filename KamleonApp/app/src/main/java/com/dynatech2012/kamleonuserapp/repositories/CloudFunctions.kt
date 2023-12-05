package com.dynatech2012.kamleonuserapp.repositories

import android.util.Log
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.InvitationStatus
import com.dynatech2012.kamleonuserapp.models.Organization
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
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
    suspend fun getInvitations(): Response<ArrayList<Invitation>> = suspendCoroutine { continuation ->
        Log.d(TAG, "will try to get invitations")
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
                Log.d(TAG, "get invitations complete: $data")
                val invitations = ArrayList<Invitation>()
                data.forEach {
                    val inv = Invitation(it)
                    invitations.add(inv)
                }
                Log.d(TAG, "get invitations: $invitations")
                invitations.sortByDescending { it.dateSent }
                continuation.resume(Response.Success(invitations))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "get invitations error: ", exception)
                continuation.resume(Response.Failure(exception))
            }
    }

    suspend fun acceptInvitation(invitationId: String): ResponseNullable<Nothing> {
        return changeInvitationStatus(invitationId, InvitationStatus.ACCEPTED)
    }

    suspend fun rejectInvitation(invitationId: String): ResponseNullable<Nothing> {
        return changeInvitationStatus(invitationId, InvitationStatus.REJECTED)
    }

    private suspend fun changeInvitationStatus(invitationId: String, status: InvitationStatus): ResponseNullable<Nothing> = suspendCoroutine { continuation ->
        if (uuid == null) {
            continuation.resume(ResponseNullable.Failure(Exception("User not logged in")))
            return@suspendCoroutine
        }
        val body = hashMapOf(
            "invitationID" to invitationId,
            "userID" to uuid,
            "status" to status.rawValue
        )
        try {
            val result = functions.getHttpsCallable("changeInvitationStatus").call(body).result
            val data = result.data as? ArrayList<HashMap<String, Any>>
            val invitations = ArrayList<Invitation>()
            data?.forEach {
                val inv = Invitation(it)
                invitations.add(inv)
            }
            continuation.resume(ResponseNullable.Success())
        } catch (e: Exception) {
            continuation.resume(ResponseNullable.Failure(e))
        }
    }

    // User profiles
    suspend fun getUserProfiles(): Response<ArrayList<Organization>> = suspendCoroutine { continuation ->
        Log.d(TAG, "will try to get user profiles")
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
                Log.d(TAG, "get invitations complete: $data")
                val organizations = ArrayList<Organization>()
                data.forEach {
                    val organization = Organization(it)
                    organizations.add(organization)
                }
                Log.d(TAG, "get user profiles: $organizations")
                continuation.resume(Response.Success(organizations))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "get user profiles error: ", exception)
                continuation.resume(Response.Failure(exception))
            }
    }

    companion object {
        val TAG: String = CloudFunctions::class.java.simpleName
    }
}