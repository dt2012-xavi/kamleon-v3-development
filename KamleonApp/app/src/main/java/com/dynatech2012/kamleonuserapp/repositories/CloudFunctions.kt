package com.dynatech2012.kamleonuserapp.repositories

import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.InvitationStatus
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
        if (email == null) {
            continuation.resume(Response.Failure(Exception("User not logged in")))
            return@suspendCoroutine
        }
        val body = hashMapOf(
            "email" to email
        )
        try {
            val result = functions.getHttpsCallable("getInvitationsForUser").call(body).result
            val data = result.data as? ArrayList<HashMap<String, Any>>
            val invitations = ArrayList<Invitation>()
            data?.forEach {
                val inv = Invitation(it)
                invitations.add(inv)
            }
            invitations.sortByDescending { it.dateSent }
            continuation.resume(Response.Success(invitations))
        } catch (e: Exception) {
            continuation.resume(Response.Failure(e))
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

    companion object {
        val TAG: String = CloudFunctions::class.java.simpleName
    }
}